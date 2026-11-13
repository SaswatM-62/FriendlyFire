/**
 * Every JSON body the frontend exchanges with the Gateway: REST responses and the
 * STOMP messages on /topic/room/{code}, /user/queue/* and /app/room/{code}/*.
 * docs/realtime-messages.md describes the same shapes for the Gateway side (FF-005).
 *
 * Field names are camelCase, matching the proto fields. Enum values drop their proto
 * prefix (ROOM_PHASE_LOBBY -> "LOBBY"). Timestamps are ISO-8601 strings. Fields marked
 * "planned" follow the Architecture page's target design and aren't in the .proto
 * contracts yet: the story named next to each one adds them.
 */

// ---------- Errors (FF-006) ----------

/** Every Gateway error response: {"error": {"code": "ROOM_FULL", "message": "..."}}. */
export interface ApiErrorBody {
  error: { code: string; message: string };
}

// ---------- Account ----------

/** What the app may show about a linked Riot account (never the PUUID). */
export interface RiotLink {
  gameName: string;
  tagLine: string;
  /** VALORANT region, e.g. "na", "eu", "ap". */
  region: string;
  linkedAt: string;
}

/** GET /api/me and PATCH /api/me. */
export interface Me {
  userId: string;
  displayName: string;
  email: string;
  createdAt: string;
  /** Absent until the player links a Riot account. */
  riotLink?: RiotLink | null;
}

/** PATCH /api/me body. 1–24 characters after trimming. */
export interface UpdateDisplayNameRequest {
  displayName: string;
}

/**
 * A player's match data, as Account judges it (planned in FF-202): SYNCED is under
 * 24 hours old; SYNCING has an estimate; NOT_SYNCED is older than 24 hours.
 */
export type DataStatus = "SYNCED" | "SYNCING" | "NOT_SYNCED" | "NOT_LINKED" | "FAILED";

/** GET /api/link/riot/status: feeds the sync banner and the profile. */
export interface SyncStatusResponse {
  status: DataStatus;
  /** While SYNCING: about how long until it's done. */
  estimatedSeconds?: number | null;
  /** When the last successful sync finished. */
  lastSyncedAt?: string | null;
  /** Matches stored by the last successful sync. */
  matchesStored?: number;
  /** When FAILED: a short reason that's safe to show. */
  errorMessage?: string;
}

/** POST /api/link/riot/start. */
export interface StartRiotLinkResponse {
  authorizeUrl: string;
}

/** POST /api/link/riot/complete body (the /link/riot/callback page sends it). */
export interface CompleteRiotLinkRequest {
  code: string;
  state: string;
}

// ---------- Rooms ----------

export type RoomPhase =
  | "LOBBY"
  | "STARTING"
  | "QUESTION"
  | "REVEAL"
  | "VOTE"
  /** Planned (ADR-026): the room waits up to a minute for a disconnected player. */
  | "PAUSED"
  | "FINISHED";

export type PlayerStatus = "CONNECTED" | "DISCONNECTED" | "SITTING_OUT";

export type ConnectionQuality = "GOOD" | "FAIR" | "POOR";

/** Planned (ADR-026): "PAUSE" replaces the contract's VOTE_OPTION_WAIT. */
export type VoteOption = "PAUSE" | "CONTINUE_WITHOUT_PLAYER" | "END_QUIZ";

export type EndReason = "COMPLETED" | "VOTED_TO_END" | "NOT_ENOUGH_PLAYERS";

export type CharacterId =
  | "blob"
  | "ghost"
  | "boxy"
  | "onigiri"
  | "star"
  | "cloud"
  | "shield"
  | "flower"
  | "gem"
  | "drop";

/** The eight faces a player can pick (shared freely: two players may have the same one). */
export type PickableExpression = "happy" | "smirk" | "cocky" | "locked-in" | "fierce" | "wink" | "gloat" | "shades";

/** Every face: the pickable ones, plus the three the game sets for moments. */
export type ExpressionId =
  | PickableExpression
  /** You're the answer (the correct tile at the reveal). */
  | "wow"
  /** Hasn't answered yet, or a dimmed tile. */
  | "thinking"
  /** Disconnected. */
  | "away";

export type ColourId =
  | "pink"
  | "cyan"
  | "mint"
  | "orange"
  | "violet"
  | "yellow"
  | "blue"
  | "lime"
  | "fuchsia"
  | "silver";

/**
 * A player's character in this room. Proposed (Profile "Your look"); needs a story.
 * Room hands out favourite icons and colours first come, first served; expressions can
 * repeat, so everyone keeps theirs.
 */
export interface PlayerLook {
  icon: CharacterId;
  colour: ColourId;
  /** Their favourite face; "happy" when not set. */
  expression?: PickableExpression;
  /** True when their favourite icon or colour was taken, so they got a random free one. */
  random?: boolean;
}

export interface RoomSettings {
  /** 5–20, default 10. */
  questionCount: number;
  /** 10–45, default 20. */
  secondsPerQuestion: number;
}

export interface Player {
  userId: string;
  displayName: string;
  /** Planned (FF-202): tells two players with the same name apart, "Sam (NA1)". */
  riotTagLine?: string;
  status: PlayerStatus;
  /** Planned (FF-202, replaces matchDataReady). */
  dataStatus?: DataStatus;
  /** Planned (FF-202): while SYNCING. */
  dataEstimatedSeconds?: number;
  /** Today's contract: true once their sync is COMPLETE. */
  matchDataReady?: boolean;
  /** Planned (FF-307): SYNCED but no Competitive or Unrated match in 30 days. */
  noRecentMatches?: boolean;
  connectionQuality?: ConnectionQuality;
  answeredCurrentQuestion: boolean;
  look?: PlayerLook;
}

export interface QuestionOption {
  /** For "pick the player" questions this is the player's userId (quiz contract). */
  optionId: string;
  label: string;
}

export interface PlayerResult {
  userId: string;
  /** Empty if they didn't answer. */
  chosenOptionId?: string;
  correct: boolean;
  points: number;
}

/** Only during REVEAL: the answer never reaches a browser early. */
export interface Reveal {
  correctOptionId: string;
  explanation: string;
  results: PlayerResult[];
}

export interface QuestionView {
  questionId: string;
  text: string;
  options: QuestionOption[];
  reveal?: Reveal;
}

export interface ScoreEntry {
  userId: string;
  displayName: string;
  totalPoints: number;
  /** 1 = first place. Equal points share a rank. */
  rank: number;
  pointsLastRound: number;
  /** From ScoringService's scoreboard; shown on the final standings. */
  correctAnswers?: number;
}

export interface Vote {
  /** The disconnected player the vote is about. */
  aboutUserId: string;
  deadline: string;
  pauseVotes: number;
  continueVotes: number;
  endVotes: number;
  /** Who has voted so far (not what they voted). */
  votedUserIds: string[];
  /** Planned (ADR-026): false once 3 minutes of pauses are used up. */
  pauseAllowed?: boolean;
}

/** Everything a browser needs to draw the room. Every event carries a full one. */
export interface RoomSnapshot {
  /** 6 characters, e.g. "K7QM2X". */
  roomCode: string;
  hostUserId: string;
  phase: RoomPhase;
  settings: RoomSettings;
  /** In the order they joined. */
  players: Player[];
  /** Set once a quiz has started. */
  quizId?: string;
  /** 1-based; 0 in the lobby. */
  roundNumber: number;
  /** During QUESTION and REVEAL. */
  currentQuestion?: QuestionView;
  /** When the current phase ends. Not set in LOBBY and FINISHED. */
  phaseDeadline?: string;
  /** Empty in the lobby. */
  scoreboard: ScoreEntry[];
  /** During VOTE. */
  activeVote?: Vote;
  /** Planned (ADR-026): during PAUSED, who the room is waiting for. */
  waitingForUserId?: string;
  /** Planned (ADR-026): pause time left in this quiz (3 minutes in total). */
  pauseSecondsLeft?: number;
  /** Planned: set when FINISHED, so an early end can say why. */
  endReason?: EndReason;
  /** Planned: the quickest correct answer of the quiz, for the podium. */
  fastestAnswer?: { userId: string; seconds: number };
}

export type RoomEventType =
  | "SNAPSHOT"
  | "PLAYER_JOINED"
  | "PLAYER_LEFT"
  | "PLAYER_KICKED"
  | "PLAYER_DISCONNECTED"
  | "PLAYER_RECONNECTED"
  | "HOST_CHANGED"
  | "SETTINGS_CHANGED"
  | "PLAYER_DATA_READY"
  | "CONNECTION_QUALITY_CHANGED"
  | "QUIZ_STARTING"
  | "QUESTION_STARTED"
  | "PLAYER_ANSWERED"
  | "QUESTION_REVEALED"
  | "VOTE_STARTED"
  /** Planned: someone voted, so the counts changed (the Vote board shows live counts). */
  | "VOTE_CAST"
  | "VOTE_RESOLVED"
  /** Planned (ADR-026). */
  | "QUIZ_PAUSED"
  | "QUIZ_FINISHED"
  | "ROOM_CLOSED"
  /** Planned (Calmer revamp, room chat): someone sent a chat message; it's in `chat`. */
  | "CHAT_MESSAGE";

/**
 * Planned (Calmer revamp): one room chat message. Messages aren't in the snapshot: the
 * browser keeps the ones it saw since it joined (docs/realtime-messages.md, proposed).
 */
export interface ChatMessage {
  messageId: string;
  userId: string;
  /** 1 to 200 characters, trimmed by the server. */
  text: string;
  sentAt: string;
}

/** Every message on /topic/room/{code}. Use `type` only for effects (toasts, sounds). */
export interface RoomEvent {
  type: RoomEventType;
  /** The player the event is about, if any. */
  subjectUserId?: string;
  occurredAt: string;
  /** The room right after the event: replace the stored state with it. */
  snapshot: RoomSnapshot;
  /** Planned: the message, on CHAT_MESSAGE only. */
  chat?: ChatMessage;
}

// ---------- STOMP: browser -> server (/app/room/{code}/...) ----------

export interface RoomActions {
  leave: Record<string, never>;
  kick: { targetUserId: string };
  settings: RoomSettings;
  start: Record<string, never>;
  answer: { questionId: string; optionId: string };
  vote: { option: VoteOption };
  "connection-quality": { quality: ConnectionQuality };
  /** Planned (Calmer revamp): send a room chat message. */
  chat: { text: string };
}

export type RoomAction = keyof RoomActions;

/** /app/ping body, echoed back on /user/queue/pong. */
export interface Ping {
  /** The browser's Date.now() when it sent the ping. */
  t: number;
}

/** /user/queue/pong. `serverTime` (the Gateway's clock, in ms) lets the browser correct its clock. */
export interface Pong extends Ping {
  serverTime?: number;
}

/** /user/queue/errors: an action of yours that failed, in the FF-006 format. */
export interface RealtimeError extends ApiErrorBody {
  /** The /app destination that failed, e.g. "/app/room/K7QM2X/answer". */
  destination?: string;
}

// ---------- History ----------

export interface ScoreboardEntry {
  userId: string;
  /** As it was in that quiz; "Deleted player" once they delete their account. */
  displayName: string;
  totalPoints: number;
  correctAnswers: number;
  rank: number;
}

/** GET /api/quizzes/{quizId}, and each item of GET /api/history. */
export interface QuizSummary {
  quizId: string;
  startedAt: string;
  finishedAt: string;
  totalRounds: number;
  roundsPlayed: number;
  endReason: EndReason;
  finalScoreboard: ScoreboardEntry[];
  /** Planned: who hosted, for "Maya's room". Not in ScoringService yet. */
  hostDisplayName?: string;
  /** Planned: shown on the detail page. Not in ScoringService yet. */
  secondsPerQuestion?: number;
}

/**
 * Planned (Calmer revamp, Home board): GET /api/stats/live, the "Live now" strip.
 * Not in any contract yet; Home hides the strip when the Gateway doesn't answer.
 */
export interface LiveStats {
  roomsPlaying: number;
  playersOnline: number;
}

/** GET /api/history?pageSize=&pageToken= */
export interface HistoryPage {
  quizzes: QuizSummary[];
  /** Empty when there are no more pages. */
  nextPageToken?: string;
}
