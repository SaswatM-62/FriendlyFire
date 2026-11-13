import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import type { RealtimeError, RoomEvent } from "@/lib/api-types";
import { FAKE_PLAYERS, ME_ID } from "@/mocks/data";
import { FAKE_TIMING, FakeRoom, fakePlayer } from "@/mocks/fakeRoom";

const p = FAKE_PLAYERS;

/** A room you host with four bots (like the browser's); bots answer right at 1.5 s, so nothing is random. */
function hostedRoom(questionCount = 5) {
  const room = new FakeRoom({
    code: "TEST22",
    hostUserId: ME_ID,
    meId: ME_ID,
    players: [fakePlayer(p.alex), fakePlayer(p.maya), fakePlayer(p.sam), fakePlayer(p.jordan), fakePlayer(p.riley)],
    settings: { questionCount, secondsPerQuestion: 10 },
    random: () => 0,
  });
  const events: RoomEvent[] = [];
  const errors: RealtimeError[] = [];
  room.subscribe((e) => events.push(e));
  const transport = room.connect({
    onEvent: () => {},
    onError: (e) => errors.push(e),
    onState: () => {},
    onPong: () => {},
    onMissedPing: () => {},
  });
  vi.advanceTimersByTime(300);
  return { room, events, errors, transport };
}

describe("FakeRoom", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => vi.useRealTimers());

  it("plays countdown, questions and reveals to the final standings", () => {
    const { room, events, transport } = hostedRoom(3);

    transport.send("start", {});
    expect(room.snapshot.phase).toBe("STARTING");

    vi.advanceTimersByTime(FAKE_TIMING.startingMs);
    expect(room.snapshot.phase).toBe("QUESTION");
    expect(room.snapshot.currentQuestion?.reveal).toBeUndefined();

    const q = room.snapshot.currentQuestion!;
    transport.send("answer", { questionId: q.questionId, optionId: q.options[1].optionId });
    // Everyone has answered by 1.5 s, so the reveal comes before the timer runs out.
    vi.advanceTimersByTime(2_500);
    expect(room.snapshot.phase).toBe("REVEAL");
    expect(room.snapshot.currentQuestion?.reveal?.results).toHaveLength(5);

    vi.advanceTimersByTime(60_000);
    expect(room.snapshot.phase).toBe("FINISHED");
    expect(room.snapshot.endReason).toBe("COMPLETED");
    expect(events.map((e) => e.type)).toContain("QUIZ_FINISHED");
    // Bots always answer right (random() is 0): their points add up, ranks are set.
    expect(room.snapshot.scoreboard[0].totalPoints).toBeGreaterThan(0);
    expect(room.snapshot.scoreboard.map((s) => s.rank)).toEqual([...room.snapshot.scoreboard].map((s) => s.rank).sort());
  });

  it("has a get-ready before every question after the first", () => {
    const { room, transport } = hostedRoom(3);
    transport.send("start", {});
    vi.advanceTimersByTime(FAKE_TIMING.startingMs);
    const q = room.snapshot.currentQuestion!;
    transport.send("answer", { questionId: q.questionId, optionId: q.options[0].optionId });
    vi.advanceTimersByTime(2_500);
    expect(room.snapshot.phase).toBe("REVEAL");

    vi.advanceTimersByTime(FAKE_TIMING.revealMs);
    expect(room.snapshot.phase).toBe("STARTING");
    expect(room.snapshot.roundNumber).toBe(1);
    expect(room.snapshot.currentQuestion).toBeUndefined();

    vi.advanceTimersByTime(FAKE_TIMING.startingMs);
    expect(room.snapshot.phase).toBe("QUESTION");
    expect(room.snapshot.roundNumber).toBe(2);
  });

  it("refuses a second answer and answers outside the question", () => {
    const { room, errors, transport } = hostedRoom();
    transport.send("start", {});
    vi.advanceTimersByTime(FAKE_TIMING.startingMs);
    const q = room.snapshot.currentQuestion!;

    transport.send("answer", { questionId: q.questionId, optionId: q.options[0].optionId });
    transport.send("answer", { questionId: q.questionId, optionId: q.options[1].optionId });
    transport.send("answer", { questionId: "old-question", optionId: q.options[1].optionId });

    expect(errors.map((e) => e.error.code)).toEqual(["ALREADY_EXISTS", "FAILED_PRECONDITION"]);
    expect(errors[0].destination).toBe("/app/room/TEST22/answer");
  });

  it("won't start while someone's matches are syncing", () => {
    const room = new FakeRoom({
      code: "TEST22",
      hostUserId: ME_ID,
      meId: ME_ID,
      players: [fakePlayer(p.alex), fakePlayer(p.maya, { dataStatus: "SYNCING" })],
    });
    const errors: RealtimeError[] = [];
    room.connect({ onEvent: () => {}, onError: (e) => errors.push(e), onState: () => {}, onPong: () => {}, onMissedPing: () => {} });
    vi.advanceTimersByTime(300);

    room.handle("start", {}, ME_ID);

    expect(room.snapshot.phase).toBe("LOBBY");
    expect(errors[0].error).toEqual({ code: "FAILED_PRECONDITION", message: "Maya still syncing." });
  });

  it("only lets the host change settings, within the limits", () => {
    const { room, errors } = hostedRoom();

    room.handle("settings", { questionCount: 12, secondsPerQuestion: 30 }, ME_ID);
    room.handle("settings", { questionCount: 50, secondsPerQuestion: 30 }, ME_ID);
    room.handle("settings", { questionCount: 6, secondsPerQuestion: 15 }, p.maya.userId);

    expect(room.snapshot.settings).toEqual({ questionCount: 12, secondsPerQuestion: 30 });
    expect(errors.map((e) => e.error.code)).toEqual(["INVALID_ARGUMENT"]);
  });

  it("lets the host change settings after a quiz too (proposed, for Back to lobby)", () => {
    const { room, transport, errors } = hostedRoom(5);
    transport.send("start", {});
    vi.advanceTimersByTime(5 * 40_000);
    expect(room.snapshot.phase).toBe("FINISHED");

    room.handle("settings", { questionCount: 8, secondsPerQuestion: 20 }, ME_ID);

    expect(room.snapshot.settings).toEqual({ questionCount: 8, secondsPerQuestion: 20 });
    expect(errors).toEqual([]);
  });

  it("sends chat messages to everyone and refuses empty or long ones (planned)", () => {
    const { room, events, errors, transport } = hostedRoom();

    transport.send("chat", { text: "  gg all  " });
    transport.send("chat", { text: "   " });
    transport.send("chat", { text: "x".repeat(201) });

    const chats = events.filter((e) => e.type === "CHAT_MESSAGE");
    expect(chats).toHaveLength(1);
    expect(chats[0].chat).toMatchObject({ userId: ME_ID, text: "gg all" });
    expect(errors.map((e) => e.error.code)).toEqual(["INVALID_ARGUMENT", "INVALID_ARGUMENT"]);
    expect(room.snapshot).not.toHaveProperty("chat");
  });

  it("has the bots chat now and then", () => {
    const { events, transport } = hostedRoom(3);
    transport.send("start", {});
    vi.advanceTimersByTime(3 * 40_000);

    const lines = events.filter((e) => e.type === "CHAT_MESSAGE").map((e) => e.chat!);
    expect(lines.length).toBeGreaterThan(0);
    expect(lines.every((c) => c.userId !== ME_ID && c.text.length > 0)).toBe(true);
  });

  it("hands the host to the next player when the host leaves", () => {
    const { room, events } = hostedRoom();

    room.handle("leave", {}, ME_ID);

    expect(room.snapshot.hostUserId).toBe(p.maya.userId);
    expect(events.slice(-2).map((e) => e.type)).toEqual(["PLAYER_LEFT", "HOST_CHANGED"]);
  });

  describe("the vote after a disconnect", () => {
    function voting() {
      const setup = hostedRoom();
      setup.transport.send("start", {});
      vi.advanceTimersByTime(FAKE_TIMING.startingMs);
      setup.room.jumpTo("vote");
      expect(setup.room.snapshot.phase).toBe("VOTE");
      return setup;
    }

    it("continues without them when that wins", () => {
      const { room, transport } = voting();
      const about = room.snapshot.activeVote!.aboutUserId;

      transport.send("vote", { option: "CONTINUE_WITHOUT_PLAYER" });
      vi.advanceTimersByTime(FAKE_TIMING.voteMs);

      expect(room.snapshot.players.find((x) => x.userId === about)?.status).toBe("SITTING_OUT");
      expect(["STARTING", "QUESTION"]).toContain(room.snapshot.phase);
    });

    it("ends the quiz when that wins", () => {
      const { room, transport } = voting();

      transport.send("vote", { option: "END_QUIZ" });
      vi.advanceTimersByTime(FAKE_TIMING.voteMs);

      expect(room.snapshot.phase).toBe("FINISHED");
      expect(room.snapshot.endReason).toBe("VOTED_TO_END");
    });

    it("pauses, and carries on with a countdown when they come back", () => {
      const { room, transport } = voting();

      transport.send("vote", { option: "PAUSE" });
      vi.advanceTimersByTime(5_000);
      expect(room.snapshot.phase).toBe("PAUSED");
      expect(room.snapshot.pauseSecondsLeft).toBe(FAKE_TIMING.pauseBudgetS - FAKE_TIMING.pauseStepS);

      vi.advanceTimersByTime(FAKE_TIMING.pauseMs);
      expect(["STARTING", "QUESTION"]).toContain(room.snapshot.phase);
    });
  });
});
