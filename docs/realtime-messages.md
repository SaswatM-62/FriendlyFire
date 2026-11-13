# Realtime messages (STOMP)

What the browser and the Gateway send each other over the WebSocket at `/ws`. The same
shapes are typed for the frontend in
[`frontend/src/lib/api-types.ts`](../frontend/src/lib/api-types.ts); keep the two in step
(FF-005). The REST endpoints are described by the Gateway's OpenAPI docs at `/api/docs`.

## Conventions

- JSON, camelCase field names matching the `.proto` fields (`room_code` → `roomCode`).
- Enum values drop their proto prefix: `ROOM_PHASE_LOBBY` → `"LOBBY"`,
  `CONNECTION_QUALITY_GOOD` → `"GOOD"`.
- Timestamps are ISO-8601 strings in UTC, e.g. `"2026-11-09T18:20:05.120Z"`.
- Fields with their proto default (empty string, 0, false, unset message) may be left out.
  The frontend treats a missing field as its default.
- The `userId` always comes from the checked token on the Gateway, never from a message body.
- **Planned** marks a field from the Architecture page's target design that isn't in the
  `.proto` contracts yet. The story in brackets adds it. Until then the Gateway leaves it out
  and the frontend copes.

## Destinations

| Destination | Direction | Body |
|---|---|---|
| `/topic/room/{code}` | server → room members | [`RoomEvent`](#roomevent) |
| `/user/queue/errors` | server → one player | [`RealtimeError`](#errors) |
| `/app/ping` → `/user/queue/pong` | both | `{"t": 1731170405120}`; the pong echoes `t` and adds `serverTime` |
| `/app/room/{code}/leave` | browser → server | `{}` |
| `/app/room/{code}/kick` | browser → server | `{"targetUserId": "…"}` |
| `/app/room/{code}/settings` | browser → server | `{"questionCount": 10, "secondsPerQuestion": 20}` |
| `/app/room/{code}/start` | browser → server | `{}` |
| `/app/room/{code}/answer` | browser → server | `{"questionId": "…", "optionId": "…"}` |
| `/app/room/{code}/vote` | browser → server | `{"option": "PAUSE" \| "CONTINUE_WITHOUT_PLAYER" \| "END_QUIZ"}` |
| `/app/room/{code}/connection-quality` | browser → server | `{"quality": "GOOD" \| "FAIR" \| "POOR"}` |
| `/app/room/{code}/chat` | browser → server | Proposed, see [Room chat](#room-chat-proposed): `{"text": "gg"}` |

`/app/ping` is answered with the browser's own `t`, so the browser can time the round trip.
With `serverTime` (the Gateway's `System.currentTimeMillis()` when it answered) the
browser also works out its clock offset, so question timers match `phaseDeadline`.

**Who may do what** (the Gateway's `ConnectInterceptor`):

- `CONNECT` sends the Auth0 access token as `Authorization: Bearer …`. Outside production a
  connection without one is allowed, but it may only ping (the status page). In production
  every connection needs one.
- One live connection per player, and the newest wins (ADR-017): a new connection replaces
  the old one, which gets an `ERROR` frame with `ALREADY_CONNECTED` and closes. Replacing a
  connection isn't reported to the room as a drop.
- The token is checked on `CONNECT` only: a live connection stays open after the token
  expires (ADR-022). Deleting the account closes it (`ACCOUNT_DELETED`).
- A session may subscribe to `/user/queue/pong`, `/user/queue/errors`, and
  `/topic/room/{code}` for a room its player is in (the Gateway asks room-service).
  Subscribing starts the room's events flowing to that topic.
- `/app/room/{code}/…` is accepted only from a session subscribed to that room's topic, so
  subscribe first. A refused subscription or action is dropped and answered on
  `/user/queue/errors` (codes `UNAUTHENTICATED`, `NOT_IN_ROOM`, `ROOM_NOT_FOUND`,
  `FORBIDDEN`). Subscribe to `/user/queue/errors` before the room topic to see why a
  room subscription was refused.
- `vote` also accepts the contract's `"WAIT"`; the Gateway sends `"PAUSE"` as
  `VOTE_OPTION_WAIT` until ADR-026 changes the contract.

## RoomEvent

Every message on `/topic/room/{code}`. **Each one carries the whole room**, so the browser
replaces its state with `snapshot` and uses `type` only for effects (a sound, a toast).

```json
{
  "type": "PLAYER_JOINED",
  "subjectUserId": "8f0c…",
  "occurredAt": "2026-11-09T18:20:05.120Z",
  "snapshot": { "...": "a RoomSnapshot" }
}
```

`type` is one of `SNAPSHOT` (always the first message after subscribing), `PLAYER_JOINED`,
`PLAYER_LEFT`, `PLAYER_KICKED`, `PLAYER_DISCONNECTED`, `PLAYER_RECONNECTED`, `HOST_CHANGED`,
`SETTINGS_CHANGED`, `PLAYER_DATA_READY`, `CONNECTION_QUALITY_CHANGED`, `QUIZ_STARTING`,
`QUESTION_STARTED`, `PLAYER_ANSWERED`, `QUESTION_REVEALED`, `VOTE_STARTED`, `VOTE_CAST`
(planned: the counts changed), `VOTE_RESOLVED`, `QUIZ_PAUSED` (planned, ADR-026),
`QUIZ_FINISHED`, `ROOM_CLOSED` (the last message), and `CHAT_MESSAGE` (proposed, see
[Room chat](#room-chat-proposed)).

**Settings and kicks after a quiz (proposed).** The design's final results screen has
"Back to lobby": everyone sees the lobby again while the room stays `FINISHED`, and the
host presses Start (already allowed from `FINISHED`). For the host to change the settings
or remove someone there, `settings` and `kick` must be accepted in `FINISHED` as well as
`LOBBY`. The fake backend does this; the room service refuses them today.

### Room chat (proposed)

The design keeps a room chat open on every room screen. Nothing in the contracts carries it
yet; this is what the frontend and its fake backend do, for the stories that add it.

- The browser sends `/app/room/{code}/chat` with `{"text": "…"}`. The server trims it and
  refuses an empty one or one over 200 characters with `INVALID_ARGUMENT` on
  `/user/queue/errors`.
- The server sends everyone in the room a `RoomEvent` with `type` `CHAT_MESSAGE`,
  `subjectUserId` the sender, the usual `snapshot`, and
  `"chat": {"messageId": "…", "userId": "…", "text": "gg", "sentAt": "…"}`.
- Messages aren't in the snapshot and aren't stored: a browser shows the ones it received
  since it opened the room.
- The grey lines in the chat ("Sam joined the room", "Question 3 started", "Time's up",
  "Maya won the quiz") come from the events the room already sends, so they need nothing
  from the server.

## RoomSnapshot

```json
{
  "roomCode": "K7QM2X",
  "hostUserId": "8f0c…",
  "phase": "QUESTION",
  "settings": { "questionCount": 10, "secondsPerQuestion": 20 },
  "players": [
    {
      "userId": "8f0c…",
      "displayName": "Maya",
      "riotTagLine": "NA1",
      "status": "CONNECTED",
      "dataStatus": "SYNCED",
      "dataEstimatedSeconds": 0,
      "noRecentMatches": false,
      "connectionQuality": "GOOD",
      "answeredCurrentQuestion": true
    }
  ],
  "quizId": "c1d2…",
  "roundNumber": 3,
  "currentQuestion": {
    "questionId": "q-3",
    "text": "Who is most likely to hit the leg of an opponent in the game?",
    "options": [{ "optionId": "8f0c…", "label": "Maya" }]
  },
  "phaseDeadline": "2026-11-09T18:21:00.000Z",
  "scoreboard": [
    { "userId": "8f0c…", "displayName": "Maya", "totalPoints": 3180, "rank": 1, "pointsLastRound": 740, "correctAnswers": 4 }
  ]
}
```

| Field | Notes |
|---|---|
| `phase` | `LOBBY`, `STARTING`, `QUESTION`, `REVEAL`, `VOTE`, `PAUSED` (planned, ADR-026), `FINISHED` |
| `phase` = `STARTING` | The 3-second countdown before the first question (`roundNumber` 0). Proposed, needs FF-417 updated: the same 3 seconds before every later question and after a pause, with `roundNumber` still the question just played, so the browser shows "Get ready, question N+1". The design gives the reveal 10 seconds (FF-417 says 5) |
| `players` | In the order they joined; the frontend gives player N the Nth character and colour |
| `players[].status` | `CONNECTED`, `DISCONNECTED`, `SITTING_OUT` |
| `players[].dataStatus` | Planned (FF-202, replaces `matchDataReady`): `SYNCED`, `SYNCING`, `NOT_SYNCED`, `NOT_LINKED`, `FAILED` |
| `players[].dataEstimatedSeconds` | Planned (FF-202): while `SYNCING` |
| `players[].riotTagLine` | Planned (FF-202): shown as "Sam (NA1)" when two players share a name |
| `players[].noRecentMatches` | Planned (FF-307): synced, but no Competitive or Unrated match in 30 days |
| `players[].matchDataReady` | Today's contract; the frontend reads it when `dataStatus` is missing |
| `players[].look` | Proposed, needs a story: `{icon, colour, expression, random}`. Room hands out favourite icons and colours first come, first served (`random` when someone got a free one instead); `expression` is one of the 8 pickable faces and never changes. Without it the frontend uses the join order |
| `currentQuestion.options[].optionId` | For "pick the player" questions, the player's `userId` |
| `currentQuestion.reveal` | **Only during `REVEAL`**: `{correctOptionId, explanation, results: [{userId, chosenOptionId, correct, points}]}` |
| `phaseDeadline` | When the countdown, question, reveal, vote or pause ends; not set in `LOBBY` and `FINISHED` |
| `scoreboard` | Empty in the lobby; `rank` 1 is first, equal points share a rank; `correctAnswers` from Scoring |
| `activeVote` | During `VOTE`: `{aboutUserId, deadline, pauseVotes, continueVotes, endVotes, votedUserIds, pauseAllowed}`. `votedUserIds` says who voted, never what. `pauseVotes` carries the contract's `wait_votes` until ADR-026 renames it; `pauseAllowed` is planned (ADR-026) |
| `waitingForUserId` | Planned (ADR-026): during `PAUSED` |
| `pauseSecondsLeft` | Planned (ADR-026): pause time left in this quiz, out of 180 |
| `endReason` | Planned: when `FINISHED`, `COMPLETED`, `VOTED_TO_END` or `NOT_ENOUGH_PLAYERS` |
| `fastestAnswer` | Planned: `{userId, seconds}`, the quickest correct answer, for the podium |

## Errors

An action that fails is answered on `/user/queue/errors` for that player only, in the same
format as REST errors (FF-006), plus the destination that failed:

```json
{
  "error": { "code": "FAILED_PRECONDITION", "message": "Maya is still syncing." },
  "destination": "/app/room/K7QM2X/start"
}
```

When the Gateway ends a connection it sends a STOMP `ERROR` frame whose `message` header
starts with the code, then closes it:

| Code | When | The browser |
|---|---|---|
| `UNAUTHENTICATED` | `CONNECT` without a token in production, or with one Auth0 wouldn't accept (e.g. expired) | Reconnects with a fresh token |
| `EMAIL_NOT_VERIFIED`, `INVITE_ONLY` | `CONNECT` from a player who may not use the app yet | Reconnects (the REST calls send them to the right page) |
| `ALREADY_CONNECTED` | The player connected again in another tab or device, which took over (ADR-017) | Stops retrying; offers "Play here" |
| `ACCOUNT_DELETED` | The player deleted their account (ADR-022) | Stops retrying; offers to sign out |
