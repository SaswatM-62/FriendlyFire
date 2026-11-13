import type {
  ChatMessage,
  EndReason,
  Player,
  RealtimeError,
  RoomAction,
  RoomActions,
  RoomEvent,
  RoomEventType,
  RoomSettings,
  RoomSnapshot,
  ScoreEntry,
  VoteOption,
} from "@/lib/api-types";
import type { RoomTransport, RoomTransportHandlers } from "@/lib/realtime";
import { FAKE_BACKEND_MARKER, FAKE_QUESTIONS, type FakePlayer } from "@/mocks/data";

/**
 * How long the fake's phases last. The get-ready and the reveal match the design (3 s and
 * 10 s, time for the staged reveal); the vote and pause are shorter, to step through quickly.
 */
export const FAKE_TIMING = {
  startingMs: 3_000,
  revealMs: 10_000,
  voteMs: 15_000,
  pauseMs: 12_000,
  /** A quiz may pause for 3 minutes in total (ADR-026). */
  pauseBudgetS: 180,
  pauseStepS: 60,
};

export type FakeScene =
  | "lobby"
  | "countdown"
  | "get-ready"
  | "question"
  | "hurry"
  | "reveal"
  | "vote"
  | "paused"
  | "podium"
  | "ended"
  | "drop"
  | "elsewhere"
  | "deleted"
  | "closed"
  | "chat";

export function fakePlayer(p: FakePlayer, over: Partial<Player> = {}): Player {
  return {
    userId: p.userId,
    displayName: p.displayName,
    riotTagLine: p.riotTagLine,
    status: "CONNECTED",
    dataStatus: "SYNCED",
    connectionQuality: "GOOD",
    answeredCurrentQuestion: false,
    ...over,
  };
}

/** Something that happens in the lobby a while after the room opens. */
export interface Arrival {
  afterMs: number;
  run: (room: FakeRoom) => void;
}

interface Options {
  code: string;
  hostUserId: string;
  meId: string;
  players: Player[];
  settings?: RoomSettings;
  arrivals?: Arrival[];
  /** When the host is a bot: it presses Start this long after everyone is ready. */
  botHostStartsAfterMs?: number;
  /** For tests: replaces Math.random. */
  random?: () => number;
}

interface Connection {
  handlers: RoomTransportHandlers;
  open: boolean;
  timers: Set<ReturnType<typeof setTimeout>>;
  pinger?: ReturnType<typeof setInterval>;
}

const clone = <T,>(value: T): T => JSON.parse(JSON.stringify(value)) as T;

/**
 * One room of the fake backend (FF-513): a scripted quiz with bot players. It answers
 * the same actions as the real room (/app/room/{code}/*) and sends the same events,
 * each with a full snapshot. Timers are plain setTimeout, so tests drive it with
 * Vitest's fake timers.
 */
export class FakeRoom {
  readonly marker = FAKE_BACKEND_MARKER;
  snapshot: RoomSnapshot;
  readonly meId: string;
  private readonly random: () => number;
  private readonly botHostStartsAfterMs: number | undefined;
  private listeners = new Set<(event: RoomEvent) => void>();
  private connections = new Set<Connection>();
  private timers = new Set<ReturnType<typeof setTimeout>>();
  private answers = new Map<string, { optionId: string; at: number }>();
  private correctOptionId = "";
  private questionStartedAt = 0;
  private votes = new Map<string, VoteOption>();
  private followerIds: string[] = [];
  private pauseUsedS = 0;
  private dropUserId: string | null = null;
  private fastest: { userId: string; seconds: number } | null = null;
  private quizCount = 0;
  private chatCount = 0;

  constructor(options: Options) {
    this.meId = options.meId;
    this.random = options.random ?? Math.random;
    this.botHostStartsAfterMs = options.botHostStartsAfterMs;
    this.snapshot = {
      roomCode: options.code,
      hostUserId: options.hostUserId,
      phase: "LOBBY",
      settings: options.settings ?? { questionCount: 5, secondsPerQuestion: 15 },
      players: options.players,
      roundNumber: 0,
      scoreboard: [],
    };
    for (const arrival of options.arrivals ?? []) this.after(arrival.afterMs, () => arrival.run(this));
    this.maybeBotHostStart();
  }

  // ---------- plumbing ----------

  subscribe(listener: (event: RoomEvent) => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private emit(type: RoomEventType, subjectUserId?: string, chat?: ChatMessage) {
    const event: RoomEvent = {
      type,
      subjectUserId,
      occurredAt: new Date().toISOString(),
      snapshot: clone(this.snapshot),
      ...(chat ? { chat } : {}),
    };
    for (const listener of this.listeners) listener(event);
    for (const c of this.connections) if (c.open) c.handlers.onEvent(event);
  }

  private after(ms: number, run: () => void) {
    const timer = setTimeout(() => {
      this.timers.delete(timer);
      run();
    }, ms);
    this.timers.add(timer);
  }

  private clearTimers() {
    for (const timer of this.timers) clearTimeout(timer);
    this.timers.clear();
  }

  private errorTo(userId: string, code: string, message: string, action: RoomAction) {
    if (userId !== this.meId) return;
    const error: RealtimeError = {
      error: { code, message },
      destination: `/app/room/${this.snapshot.roomCode}/${action}`,
    };
    for (const c of this.connections) if (c.open) c.handlers.onError(error);
  }

  private deadlineIn(ms: number) {
    return new Date(Date.now() + ms).toISOString();
  }

  private player(userId: string) {
    return this.snapshot.players.find((p) => p.userId === userId);
  }

  private bots() {
    return this.snapshot.players.filter((p) => p.userId !== this.meId && p.status === "CONNECTED");
  }

  /** The lobby, or after a quiz (proposed: settings and kicks work there too, for "Back to lobby"). */
  private betweenQuizzes() {
    return this.snapshot.phase === "LOBBY" || this.snapshot.phase === "FINISHED";
  }

  private active() {
    return this.snapshot.players.filter((p) => p.status !== "SITTING_OUT");
  }

  // ---------- room chat (planned) ----------

  /** A chat message from someone in the room, to everyone. */
  say(userId: string, text: string) {
    if (!this.player(userId)) return;
    this.chatCount += 1;
    const chat: ChatMessage = { messageId: `m${this.chatCount}`, userId, text, sentAt: new Date().toISOString() };
    this.emit("CHAT_MESSAGE", userId, chat);
  }

  /** Now and then a bot says something about what just happened. `{name}` is the answer. */
  private chatter(lines: string[], afterMs: number, name = "") {
    const bots = this.bots().filter((p) => p.userId !== this.snapshot.activeVote?.aboutUserId);
    if (bots.length === 0 || this.random() > 0.7) return;
    const bot = bots[Math.floor(this.random() * bots.length)];
    const line = lines[Math.floor(this.random() * lines.length)].replace("{name}", name);
    this.after(afterMs, () => this.say(bot.userId, line));
  }

  // ---------- the live connection ----------

  /** The browser's "WebSocket": opens after a moment, then gets a SNAPSHOT and pongs. */
  connect(handlers: RoomTransportHandlers): RoomTransport {
    const connection: Connection = { handlers, open: false, timers: new Set() };
    this.connections.add(connection);
    const open = () => {
      connection.open = true;
      handlers.onState("open");
      handlers.onEvent({ type: "SNAPSHOT", occurredAt: new Date().toISOString(), snapshot: clone(this.snapshot) });
      const pong = () => {
        const rtt = 30 + Math.round(this.random() * 40);
        const now = Date.now();
        handlers.onPong({ sentAt: now - rtt, receivedAt: now, serverTime: now - rtt / 2 });
      };
      pong();
      connection.pinger = setInterval(pong, 3_000);
    };
    handlers.onState("connecting");
    const t = setTimeout(open, 250);
    connection.timers.add(t);
    return {
      send: (action, body) => {
        if (connection.open) this.handle(action, body, this.meId);
      },
      close: () => {
        connection.open = false;
        clearInterval(connection.pinger);
        for (const timer of connection.timers) clearTimeout(timer);
        this.connections.delete(connection);
        handlers.onState("closed");
      },
    };
  }

  /** The player's network drops for a few seconds, then comes back (FF-309). */
  simulateDrop(ms = 4_000) {
    for (const c of this.connections) {
      c.open = false;
      clearInterval(c.pinger);
      c.handlers.onState("reconnecting");
      const t = setTimeout(() => {
        c.open = true;
        c.handlers.onState("open");
        c.handlers.onEvent({ type: "SNAPSHOT", occurredAt: new Date().toISOString(), snapshot: clone(this.snapshot) });
      }, ms);
      c.timers.add(t);
    }
  }

  /** The Gateway closes the connection: the player opened the room somewhere else (ADR-017). */
  simulateElsewhere() {
    this.endConnections("elsewhere");
  }

  /** The Gateway closes the connection: the player deleted their account on another device (ADR-022). */
  simulateDeleted() {
    this.endConnections("deleted");
  }

  private endConnections(state: "elsewhere" | "deleted") {
    for (const c of this.connections) {
      c.open = false;
      clearInterval(c.pinger);
      c.handlers.onState(state);
    }
  }

  // ---------- actions from the browser (/app/room/{code}/...) ----------

  handle<A extends RoomAction>(action: A, body: RoomActions[A], from: string) {
    const isHost = from === this.snapshot.hostUserId;
    switch (action) {
      case "leave":
        return this.removePlayer(from, "PLAYER_LEFT");
      case "kick": {
        if (!isHost || !this.betweenQuizzes()) {
          return this.errorTo(from, "PERMISSION_DENIED", "Only the host can remove players.", action);
        }
        return this.removePlayer((body as RoomActions["kick"]).targetUserId, "PLAYER_KICKED");
      }
      case "settings": {
        if (!isHost || !this.betweenQuizzes()) {
          return this.errorTo(from, "PERMISSION_DENIED", "Only the host can change the settings.", action);
        }
        const s = body as RoomActions["settings"];
        if (s.questionCount < 5 || s.questionCount > 20 || s.secondsPerQuestion < 10 || s.secondsPerQuestion > 45) {
          return this.errorTo(from, "INVALID_ARGUMENT", "5–20 questions, 10–45 seconds each.", action);
        }
        this.snapshot.settings = { ...s };
        return this.emit("SETTINGS_CHANGED", from);
      }
      case "start": {
        if (!isHost) return this.errorTo(from, "PERMISSION_DENIED", "Only the host can start.", action);
        const blocker = this.startBlocker();
        if (blocker) return this.errorTo(from, "FAILED_PRECONDITION", blocker, action);
        return this.startQuiz();
      }
      case "answer":
        return this.answer(from, body as RoomActions["answer"]);
      case "vote":
        return this.castVote(from, (body as RoomActions["vote"]).option);
      case "chat": {
        const text = (body as RoomActions["chat"]).text.trim();
        if (text.length < 1 || text.length > 200) {
          return this.errorTo(from, "INVALID_ARGUMENT", "Messages are 1 to 200 characters.", action);
        }
        return this.say(from, text);
      }
      case "connection-quality": {
        const p = this.player(from);
        const quality = (body as RoomActions["connection-quality"]).quality;
        if (p && p.connectionQuality !== quality) {
          p.connectionQuality = quality;
          this.emit("CONNECTION_QUALITY_CHANGED", from);
        }
        return;
      }
    }
  }

  // ---------- lobby ----------

  addPlayer(player: Player) {
    if (this.player(player.userId)) {
      this.player(player.userId)!.status = "CONNECTED";
      return this.emit("PLAYER_RECONNECTED", player.userId);
    }
    this.snapshot.players.push(player);
    this.emit("PLAYER_JOINED", player.userId);
    if (player.userId !== this.meId && this.snapshot.phase === "LOBBY") {
      this.after(1_200, () => this.say(player.userId, ["yo", "hey all", "what are we playing to", "ready when you are"][this.snapshot.players.length % 4]));
    }
    this.maybeBotHostStart();
  }

  /** A player's sync estimate changes, or their sync finishes. */
  setDataStatus(userId: string, status: Player["dataStatus"], estimatedSeconds?: number) {
    const p = this.player(userId);
    if (!p) return;
    p.dataStatus = status;
    p.dataEstimatedSeconds = estimatedSeconds;
    this.emit(status === "SYNCED" ? "PLAYER_DATA_READY" : "SNAPSHOT", userId);
    this.maybeBotHostStart();
  }

  setQuality(userId: string, quality: Player["connectionQuality"]) {
    const p = this.player(userId);
    if (!p) return;
    p.connectionQuality = quality;
    this.emit("CONNECTION_QUALITY_CHANGED", userId);
  }

  private removePlayer(userId: string, type: "PLAYER_LEFT" | "PLAYER_KICKED") {
    const index = this.snapshot.players.findIndex((p) => p.userId === userId);
    if (index < 0) return;
    this.snapshot.players.splice(index, 1);
    this.snapshot.scoreboard = this.snapshot.scoreboard.filter((s) => s.userId !== userId);
    if (this.snapshot.players.length === 0) {
      this.clearTimers();
      return this.emit("ROOM_CLOSED");
    }
    if (this.snapshot.hostUserId === userId) {
      this.snapshot.hostUserId = this.snapshot.players[0].userId;
      this.emit(type, userId);
      return this.emit("HOST_CHANGED", this.snapshot.hostUserId);
    }
    this.emit(type, userId);
  }

  private startBlocker(): string | null {
    const present = this.active();
    if (present.length < 2) return "You need at least 2 players to start.";
    const syncing = present.filter((p) => p.dataStatus !== "SYNCED");
    if (syncing.length > 0) return `${syncing.map((p) => p.displayName).join(" and ")} still syncing.`;
    return null;
  }

  private maybeBotHostStart() {
    if (this.botHostStartsAfterMs === undefined) return;
    if (this.snapshot.hostUserId === this.meId || this.snapshot.phase !== "LOBBY") return;
    if (this.startBlocker()) return;
    this.after(this.botHostStartsAfterMs, () => {
      if (this.snapshot.phase === "LOBBY" && !this.startBlocker()) this.startQuiz();
    });
  }

  // ---------- the quiz ----------

  startQuiz() {
    this.clearTimers();
    this.quizCount += 1;
    this.pauseUsedS = 0;
    this.fastest = null;
    for (const p of this.snapshot.players) {
      if (p.status === "SITTING_OUT") p.status = "CONNECTED";
    }
    const connected = this.snapshot.players.filter((p) => p.status === "CONNECTED");
    // In a quiz of 4+ questions, the second player drops during question 3 (once).
    this.dropUserId = this.snapshot.settings.questionCount >= 4 && this.quizCount === 1 ? (this.bots()[0]?.userId ?? null) : null;
    Object.assign(this.snapshot, {
      phase: "STARTING",
      quizId: `fake-quiz-${this.snapshot.roomCode}-${this.quizCount}`,
      roundNumber: 0,
      currentQuestion: undefined,
      activeVote: undefined,
      endReason: undefined,
      fastestAnswer: undefined,
      waitingForUserId: undefined,
      pauseSecondsLeft: FAKE_TIMING.pauseBudgetS,
      phaseDeadline: this.deadlineIn(FAKE_TIMING.startingMs),
      scoreboard: connected.map<ScoreEntry>((p) => ({
        userId: p.userId,
        displayName: p.displayName,
        totalPoints: 0,
        rank: 1,
        pointsLastRound: 0,
        correctAnswers: 0,
      })),
    } satisfies Partial<RoomSnapshot>);
    this.emit("QUIZ_STARTING");
    this.after(FAKE_TIMING.startingMs, () => this.startQuestion(1));
  }

  /** Get ready: a short countdown before every question after the first (and after a pause). */
  private countdownTo(round: number) {
    this.snapshot.phase = "STARTING";
    this.snapshot.phaseDeadline = this.deadlineIn(FAKE_TIMING.startingMs);
    this.snapshot.currentQuestion = undefined;
    this.snapshot.waitingForUserId = undefined;
    this.emit("QUIZ_STARTING");
    this.after(FAKE_TIMING.startingMs, () => this.startQuestion(round));
  }

  startQuestion(round: number, durationMs?: number) {
    this.clearTimers();
    const limitMs = this.snapshot.settings.secondsPerQuestion * 1000;
    const question = FAKE_QUESTIONS[(round - 1 + (this.quizCount - 1) * 3) % FAKE_QUESTIONS.length];
    const active = this.active();
    const candidates = active.filter((p) => p.userId !== this.meId);
    const correct = candidates.length > 0 ? candidates[(round * 3 + this.quizCount) % candidates.length] : active[0];
    this.correctOptionId = correct.userId;
    this.questionStartedAt = Date.now();
    this.answers.clear();
    for (const p of this.snapshot.players) p.answeredCurrentQuestion = false;
    Object.assign(this.snapshot, {
      phase: "QUESTION",
      roundNumber: round,
      activeVote: undefined,
      waitingForUserId: undefined,
      phaseDeadline: this.deadlineIn(durationMs ?? limitMs),
      currentQuestion: {
        questionId: `${this.snapshot.quizId}-q${round}`,
        text: question.text,
        options: active.map((p) => ({ optionId: p.userId, label: p.displayName })),
      },
    } satisfies Partial<RoomSnapshot>);
    this.emit("QUESTION_STARTED");
    this.chatter(["no spoilers", "I know this one", "hmm", "this is so me"], 3_000);

    for (const bot of this.bots()) {
      if (bot.status !== "CONNECTED") continue;
      const delay = 1_500 + this.random() * Math.min(limitMs * 0.7, 9_000);
      this.after(delay, () => {
        if (this.snapshot.phase !== "QUESTION" || this.player(bot.userId)?.status !== "CONNECTED") return;
        const options = this.snapshot.currentQuestion!.options;
        const right = this.random() < 0.6;
        const wrong = options.filter((o) => o.optionId !== this.correctOptionId);
        const pick = right || wrong.length === 0 ? this.correctOptionId : wrong[Math.floor(this.random() * wrong.length)].optionId;
        this.answer(bot.userId, { questionId: this.snapshot.currentQuestion!.questionId, optionId: pick });
      });
    }
    if (this.dropUserId && round === 3) {
      const dropping = this.dropUserId;
      this.after(2_500, () => {
        const p = this.player(dropping);
        if (p && p.status === "CONNECTED") {
          p.status = "DISCONNECTED";
          this.emit("PLAYER_DISCONNECTED", dropping);
        }
      });
    }
    this.after(durationMs ?? limitMs, () => this.reveal());
  }

  private answer(from: string, body: RoomActions["answer"]) {
    const q = this.snapshot.currentQuestion;
    if (this.snapshot.phase !== "QUESTION" || !q || body.questionId !== q.questionId) {
      return this.errorTo(from, "FAILED_PRECONDITION", "Time's up for that question.", "answer");
    }
    if (this.answers.has(from)) return this.errorTo(from, "ALREADY_EXISTS", "You already answered.", "answer");
    if (!q.options.some((o) => o.optionId === body.optionId)) {
      return this.errorTo(from, "INVALID_ARGUMENT", "That isn't one of the answers.", "answer");
    }
    this.answers.set(from, { optionId: body.optionId, at: Date.now() });
    const p = this.player(from);
    if (p) p.answeredCurrentQuestion = true;
    this.emit("PLAYER_ANSWERED", from);
    const waiting = this.snapshot.players.filter((x) => x.status === "CONNECTED" && !x.answeredCurrentQuestion);
    if (waiting.length === 0) {
      this.clearTimers();
      this.after(600, () => this.reveal());
    }
  }

  reveal() {
    this.clearTimers();
    const q = this.snapshot.currentQuestion;
    if (!q) return;
    const limitMs = this.snapshot.settings.secondsPerQuestion * 1000;
    const deadline = new Date(this.snapshot.phaseDeadline ?? Date.now()).getTime();
    const question = FAKE_QUESTIONS[(this.snapshot.roundNumber - 1 + (this.quizCount - 1) * 3) % FAKE_QUESTIONS.length];
    const results = this.active().map((p) => {
      const a = this.answers.get(p.userId);
      const correct = a?.optionId === this.correctOptionId;
      const left = a ? Math.min(limitMs, Math.max(0, deadline - a.at)) : 0;
      if (correct && a) {
        const seconds = Math.max(0.1, Math.round((a.at - this.questionStartedAt) / 100) / 10);
        if (!this.fastest || seconds < this.fastest.seconds) this.fastest = { userId: p.userId, seconds };
      }
      return {
        userId: p.userId,
        chosenOptionId: a?.optionId,
        correct,
        points: correct ? 500 + Math.round((500 * left) / limitMs) : 0,
      };
    });
    for (const entry of this.snapshot.scoreboard) {
      const r = results.find((x) => x.userId === entry.userId);
      entry.pointsLastRound = r?.points ?? 0;
      entry.totalPoints += entry.pointsLastRound;
      if (r?.correct) entry.correctAnswers = (entry.correctAnswers ?? 0) + 1;
    }
    this.rank();
    const correctName = this.player(this.correctOptionId)?.displayName ?? "They";
    q.reveal = {
      correctOptionId: this.correctOptionId,
      explanation: question.explanation.replace("{name}", correctName),
      results,
    };
    this.snapshot.phase = "REVEAL";
    this.snapshot.phaseDeadline = this.deadlineIn(FAKE_TIMING.revealMs);
    this.emit("QUESTION_REVEALED");
    this.chatter(["HOW was it {name}", "called it", "no way", "gg, fastest finger"], 1_800, correctName);
    this.after(FAKE_TIMING.revealMs, () => this.next());
  }

  private rank() {
    const board = this.snapshot.scoreboard;
    board.sort((a, b) => b.totalPoints - a.totalPoints);
    for (const entry of board) entry.rank = board.findIndex((e) => e.totalPoints === entry.totalPoints) + 1;
  }

  private next() {
    const connected = this.snapshot.players.filter((p) => p.status === "CONNECTED");
    const gone = this.snapshot.players.find((p) => p.status === "DISCONNECTED");
    if (connected.length < 2) return this.finish("NOT_ENOUGH_PLAYERS");
    if (gone) return this.startVote(gone.userId);
    if (this.snapshot.roundNumber < this.snapshot.settings.questionCount) {
      return this.countdownTo(this.snapshot.roundNumber + 1);
    }
    this.finish("COMPLETED");
  }

  // ---------- disconnects: vote, pause ----------

  startVote(aboutUserId: string) {
    this.clearTimers();
    this.votes.clear();
    const pauseAllowed = this.pauseUsedS < FAKE_TIMING.pauseBudgetS;
    Object.assign(this.snapshot, {
      phase: "VOTE",
      currentQuestion: undefined,
      phaseDeadline: this.deadlineIn(FAKE_TIMING.voteMs),
      activeVote: {
        aboutUserId,
        deadline: this.deadlineIn(FAKE_TIMING.voteMs),
        pauseVotes: 0,
        continueVotes: 0,
        endVotes: 0,
        votedUserIds: [],
        pauseAllowed,
      },
    } satisfies Partial<RoomSnapshot>);
    this.emit("VOTE_STARTED", aboutUserId);
    this.chatter(["their wifi again", "give them a min", "nah keep going"], 1_500);
    // The first bot wants to wait and the second to carry on; the rest copy your vote
    // (or carry on if you don't vote), so each outcome can be reached from the browser.
    const voters = this.bots().filter((p) => p.userId !== aboutUserId);
    this.followerIds = voters.slice(2).map((p) => p.userId);
    if (voters[0]) this.after(2_500, () => this.castVote(voters[0].userId, pauseAllowed ? "PAUSE" : "CONTINUE_WITHOUT_PLAYER"));
    if (voters[1]) this.after(4_000, () => this.castVote(voters[1].userId, "CONTINUE_WITHOUT_PLAYER"));
    this.after(FAKE_TIMING.voteMs - 3_000, () => {
      for (const id of this.followerIds) this.castVote(id, "CONTINUE_WITHOUT_PLAYER");
    });
    this.after(FAKE_TIMING.voteMs, () => this.resolveVote());
  }

  private castVote(from: string, option: VoteOption) {
    const vote = this.snapshot.activeVote;
    if (this.snapshot.phase !== "VOTE" || !vote || from === vote.aboutUserId || this.votes.has(from)) return;
    if (option === "PAUSE" && vote.pauseAllowed === false) {
      return this.errorTo(from, "FAILED_PRECONDITION", "No pause time left.", "vote");
    }
    this.votes.set(from, option);
    vote.votedUserIds.push(from);
    vote.pauseVotes = [...this.votes.values()].filter((v) => v === "PAUSE").length;
    vote.continueVotes = [...this.votes.values()].filter((v) => v === "CONTINUE_WITHOUT_PLAYER").length;
    vote.endVotes = [...this.votes.values()].filter((v) => v === "END_QUIZ").length;
    this.emit("VOTE_CAST", from);
    if (from === this.meId) {
      for (const id of this.followerIds) this.after(1_200, () => this.castVote(id, option));
    }
    const eligible = this.snapshot.players.filter((p) => p.status === "CONNECTED" && p.userId !== vote.aboutUserId);
    if (eligible.every((p) => this.votes.has(p.userId))) {
      this.clearTimers();
      this.after(800, () => this.resolveVote());
    }
  }

  private resolveVote() {
    const vote = this.snapshot.activeVote;
    if (!vote) return;
    this.clearTimers();
    const top = Math.max(vote.pauseVotes, vote.continueVotes, vote.endVotes);
    const winners: VoteOption[] = [];
    if (vote.pauseVotes === top) winners.push("PAUSE");
    if (vote.continueVotes === top) winners.push("CONTINUE_WITHOUT_PLAYER");
    if (vote.endVotes === top) winners.push("END_QUIZ");
    // Most votes wins; a tie pauses if pause time is left, otherwise continues (ADR-026).
    const outcome: VoteOption =
      winners.length === 1 ? winners[0] : vote.pauseAllowed !== false ? "PAUSE" : "CONTINUE_WITHOUT_PLAYER";
    const about = vote.aboutUserId;
    this.snapshot.activeVote = undefined;

    if (outcome === "END_QUIZ") {
      this.finish("VOTED_TO_END", "VOTE_RESOLVED");
      return;
    }
    if (outcome === "CONTINUE_WITHOUT_PLAYER") {
      const p = this.player(about);
      if (p) p.status = "SITTING_OUT";
      this.snapshot.phase = "STARTING";
      this.snapshot.phaseDeadline = this.deadlineIn(FAKE_TIMING.startingMs);
      this.emit("VOTE_RESOLVED", about);
      const round = this.snapshot.roundNumber + 1;
      if (round > this.snapshot.settings.questionCount) this.after(1_500, () => this.finish("COMPLETED"));
      else this.after(FAKE_TIMING.startingMs, () => this.startQuestion(round));
      return;
    }
    this.pause(about, "VOTE_RESOLVED");
  }

  pause(about: string, type: RoomEventType = "QUIZ_PAUSED") {
    this.clearTimers();
    this.pauseUsedS += FAKE_TIMING.pauseStepS;
    Object.assign(this.snapshot, {
      phase: "PAUSED",
      currentQuestion: undefined,
      activeVote: undefined,
      waitingForUserId: about,
      pauseSecondsLeft: Math.max(0, FAKE_TIMING.pauseBudgetS - this.pauseUsedS),
      phaseDeadline: this.deadlineIn(FAKE_TIMING.pauseMs),
    } satisfies Partial<RoomSnapshot>);
    this.emit(type, about);
    // They come back partway through the pause.
    this.after(FAKE_TIMING.pauseMs * 0.6, () => {
      const p = this.player(about);
      if (p) p.status = "CONNECTED";
      this.emit("PLAYER_RECONNECTED", about);
      const round = this.snapshot.roundNumber + 1;
      this.after(1_000, () =>
        round > this.snapshot.settings.questionCount ? this.finish("COMPLETED") : this.countdownTo(round),
      );
    });
  }

  finish(reason: EndReason, type: RoomEventType = "QUIZ_FINISHED") {
    this.clearTimers();
    this.rank();
    Object.assign(this.snapshot, {
      phase: "FINISHED",
      currentQuestion: undefined,
      activeVote: undefined,
      waitingForUserId: undefined,
      phaseDeadline: undefined,
      endReason: reason,
      fastestAnswer: this.fastest ?? undefined,
    } satisfies Partial<RoomSnapshot>);
    this.emit(type);
    this.chatter(["rematch?", "gg all", "same room, go"], 2_000);
  }

  close() {
    this.clearTimers();
    this.emit("ROOM_CLOSED");
  }

  // ---------- the dev panel's shortcuts ----------

  /** Jumps straight to a screen, with believable scores, for building and reviewing it. */
  jumpTo(scene: FakeScene) {
    const ensureQuiz = () => {
      if (!this.snapshot.quizId || this.snapshot.phase === "LOBBY") {
        this.startQuiz();
        this.clearTimers();
      }
      if (this.snapshot.scoreboard.every((s) => s.totalPoints === 0)) {
        this.snapshot.scoreboard.forEach((s, i) => {
          s.totalPoints = 3180 - i * 430;
          s.correctAnswers = 4 - Math.min(i, 3);
        });
        this.snapshot.roundNumber = Math.max(this.snapshot.roundNumber, 2);
        this.rank();
      }
    };
    const restoreEveryone = () => {
      for (const p of this.snapshot.players) {
        if (p.status !== "CONNECTED") p.status = "CONNECTED";
        p.dataStatus = "SYNCED";
      }
    };
    switch (scene) {
      case "lobby":
        this.clearTimers();
        restoreEveryone();
        Object.assign(this.snapshot, {
          phase: "LOBBY",
          roundNumber: 0,
          quizId: undefined,
          currentQuestion: undefined,
          activeVote: undefined,
          phaseDeadline: undefined,
          scoreboard: [],
          endReason: undefined,
        } satisfies Partial<RoomSnapshot>);
        return this.emit("SNAPSHOT");
      case "countdown":
        restoreEveryone();
        return this.startQuiz();
      case "get-ready":
        restoreEveryone();
        ensureQuiz();
        this.clearTimers();
        return this.countdownTo(this.snapshot.roundNumber + 1);
      case "question":
        ensureQuiz();
        return this.startQuestion(this.snapshot.roundNumber + 1, 60_000);
      case "hurry":
        ensureQuiz();
        return this.startQuestion(this.snapshot.roundNumber + 1, 5_000);
      case "reveal": {
        ensureQuiz();
        this.startQuestion(this.snapshot.roundNumber + 1, 20_000);
        this.clearTimers();
        const q = this.snapshot.currentQuestion!;
        this.active().forEach((p, i) => {
          if (p.userId === this.meId) return;
          const pick = i % 2 === 0 ? this.correctOptionId : q.options[(i + 1) % q.options.length].optionId;
          this.answers.set(p.userId, { optionId: pick, at: this.questionStartedAt + 1_200 + i * 900 });
        });
        return this.reveal();
      }
      case "vote": {
        ensureQuiz();
        const about = this.bots()[0];
        if (!about) return;
        about.status = "DISCONNECTED";
        return this.startVote(about.userId);
      }
      case "paused": {
        ensureQuiz();
        const about = this.bots()[0];
        if (!about) return;
        about.status = "DISCONNECTED";
        return this.pause(about.userId);
      }
      case "podium":
        ensureQuiz();
        this.snapshot.roundNumber = this.snapshot.settings.questionCount;
        this.fastest = this.fastest ?? { userId: this.meId, seconds: 1.2 };
        return this.finish("COMPLETED");
      case "ended":
        ensureQuiz();
        this.snapshot.roundNumber = 3;
        return this.finish("VOTED_TO_END");
      case "drop":
        return this.simulateDrop();
      case "elsewhere":
        return this.simulateElsewhere();
      case "deleted":
        return this.simulateDeleted();
      case "closed":
        return this.close();
      case "chat": {
        const bot = this.bots()[0];
        if (bot) this.say(bot.userId, ["gg", "this one's hard", "who picked me??", "lol", "ok that was obvious"][this.chatCount % 5]);
        return;
      }
    }
  }
}
