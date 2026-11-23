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
| `/app/room/{code}/chat` | browser → server | `{"text": "gg"}`, see [Room chat](#room-chat) |

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
- `vote` takes `"PAUSE"`, `"CONTINUE_WITHOUT_PLAYER"` or `"END_QUIZ"` (the contract's
  `VoteOption` without its prefix, ADR-026).

## RoomEvent

Every message on `/topic/room/{code}`. **Each one carries the whole room**, so the browser
replaces its state with `snapshot` and uses `type` only for effects (a sound, a toast).

```json
{
  "type": "PLAYER_JOINED",
  "subjectUserId": "8f0c…",
  "occurredAt": "2026-11-09T18:20:05.120Z",
  "snapshot": { "...": "a RoomSnapshot" },
  "message": "Not enough matches for 15 questions. Try 10."
}
```

`message` is a sentence safe to show every player, left out unless the event has one. Today
only `QUIZ_START_FAILED` sets it (the reason the room went back to the lobby).

`type` is one of `SNAPSHOT` (always the first message after subscribing), `PLAYER_JOINED`,
`PLAYER_LEFT`, `PLAYER_KICKED`, `PLAYER_DISCONNECTED`, `PLAYER_RECONNECTED`, `HOST_CHANGED`,
`SETTINGS_CHANGED`, `PLAYER_DATA_READY`, `CONNECTION_QUALITY_CHANGED`, `QUIZ_PREPARING` (the
host pressed Start; the questions are being built), `QUIZ_START_FAILED` (back to the lobby,
see `message`), `QUIZ_STARTING`,
`QUESTION_STARTED`, `PLAYER_ANSWERED`, `QUESTION_REVEALED`, `VOTE_STARTED`, `VOTE_CAST`
(the counts changed), `VOTE_RESOLVED`, `QUIZ_PAUSED` (ADR-026),
`QUIZ_FINISHED`, `ROOM_CLOSED` (the last message), and `CHAT_MESSAGE` (see
[Room chat](#room-chat)).

**Settings and kicks after a quiz.** The final results screen has "Back to lobby":
everyone sees the lobby again while the room stays `FINISHED`, and the host presses Start
(allowed from `FINISHED`). `settings` and `kick` are accepted in `FINISHED` as well as
`LOBBY`, so the host can change the settings or remove someone there.

**How a quiz moves along** (Room's timings, all from its clock): `PREPARING` at least
8 seconds (at most 60), then a 3-second `STARTING` countdown before every question, the
question for the room's seconds per question (or until every connected player has
answered), then a 10-second `REVEAL`. After a reveal (or after preparing), if a player has
dropped, the others get a 15-second `VOTE`; a pause lasts up to 60 seconds, 180 per quiz,
and unused pause time comes back if they return early. A quiz with fewer than 2 players left
ends with `NOT_ENOUGH_PLAYERS`. A host who stays disconnected for 30 seconds in the lobby (or
after a quiz) hands the host role to the next connected player.

### Room chat

Chat is open in every phase, spectators included (FF-315).

- The browser sends `/app/room/{code}/chat` with `{"text": "…"}`, at most 5 messages per
  5 seconds (`RATE_LIMITED` after that). Room trims it and refuses an empty one or one over
  200 characters with `INVALID_ARGUMENT` on `/user/queue/errors`.
- Room sends everyone in the room a `RoomEvent` with `type` `CHAT_MESSAGE`,
  `subjectUserId` the sender, the usual `snapshot`, and
  `"chat": {"messageId": "…", "userId": "…", "displayName": "Sam", "text": "gg", "sentAt": "…"}`.
  `displayName` is the name when it was sent, so a message still has a name after its
  sender leaves; it becomes "Deleted player" when they delete their account.
- Messages aren't in the snapshot. Room keeps each room's last 200 until the room closes:
  `GET /api/rooms/{code}/chat` returns `{"messages": [...]}`, oldest first, for a page load
  or a rejoin (players in the room only: `403 NOT_IN_ROOM` otherwise). The browser skips
  messages it already shows, by `messageId`.
- The grey lines in the chat ("Sam joined the room", "Question 3 started", "Time's up",
  "Maya won the quiz") come from the events the room already sends.

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
      "notEnoughMatches": false,
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
| `phase` | `LOBBY`, `PREPARING`, `STARTING`, `QUESTION`, `REVEAL`, `VOTE`, `PAUSED` (ADR-026), `FINISHED` |
| `phase` = `PREPARING` | After Start, while the quiz engine builds the questions. Every browser shows the loading screen (how a quiz works). It lasts at least 8 seconds (`phaseDeadline`) and at most 60; then `STARTING`, or `VOTE` first if someone dropped meanwhile, or back to `LOBBY` with `QUIZ_START_FAILED`. Settings, kicks and a second Start are refused; the host can't cancel |
| `phase` = `STARTING` | The 3-second countdown before every question, and after a vote or a pause. `roundNumber` is still the question just played (0 before the first), so the browser shows "Get ready, question N+1" |
| `players` | In the order they joined; the frontend gives player N the Nth character and colour |
| `players[].status` | `CONNECTED`, `DISCONNECTED`, `SITTING_OUT`, `SPECTATING` (joined while a quiz was preparing or running: sees the room and chats, but can't answer or vote, isn't an option and has no score; plays from the next quiz) |
| `players[].dataStatus` | `SYNCED`, `SYNCING`, `NOT_SYNCED`, `NOT_LINKED`, `FAILED`, from Account (ADR-010; replaces `matchDataReady`). Room asks when they join and every 5 seconds in the lobby, and sends `PLAYER_DATA_READY` when it changes (FF-307). Left out if Account couldn't answer yet |
| `players[].dataEstimatedSeconds` | Only while `SYNCING` with an estimate |
| `players[].riotTagLine` | Shown as "Sam (NA1)" when two players share a name; left out if not linked |
| `players[].notEnoughMatches` | Synced, but fewer than 5 Competitive or Unrated matches in the last 30 days: they play, but no question is about them (FF-307). The frontend still reads the older `noRecentMatches` name |
| `players[].look` | Proposed, needs a story: `{icon, colour, expression, random}`. Room hands out favourite icons and colours first come, first served (`random` when someone got a free one instead); `expression` is one of the 8 pickable faces and never changes. Without it the frontend uses the join order |
| `currentQuestion.options[].optionId` | For "pick the player" questions, the player's `userId` |
| `currentQuestion.reveal` | **Only during `REVEAL`**: `{correctOptionId, explanation, results: [{userId, chosenOptionId, correct, points}]}` |
| `phaseDeadline` | When the countdown, question, reveal, vote or pause ends; during `PREPARING` the earliest the countdown can start (Start + 8 s); not set in `LOBBY` and `FINISHED` |
| `scoreboard` | Empty in the lobby; `rank` 1 is first, equal points share a rank; `correctAnswers` from Scoring |
| `activeVote` | During `VOTE`: `{aboutUserId, deadline, pauseVotes, continueVotes, endVotes, votedUserIds, pauseAllowed}`. `votedUserIds` says who voted, never what. `pauseAllowed` is false once the quiz's 3 minutes of pauses are used (ADR-026) |
| `waitingForUserId` | During `PAUSED`: the disconnected player the room is waiting for (ADR-026) |
| `pauseSecondsLeft` | Pause time left in this quiz, out of 180; left out until a quiz starts (ADR-026) |
| `endReason` | When `FINISHED`: `COMPLETED`, `VOTED_TO_END` or `NOT_ENOUGH_PLAYERS` |
| `fastestAnswer` | When `FINISHED`, if anyone answered correctly: `{userId, seconds}` (one decimal), the quickest correct answer, for the podium |

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
