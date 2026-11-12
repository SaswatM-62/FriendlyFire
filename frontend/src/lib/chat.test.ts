import { describe, expect, it } from "vitest";
import type { RoomEvent, RoomSnapshot } from "@/lib/api-types";
import { chatLinesFor } from "@/lib/chat";
import { FAKE_PLAYERS, ME_ID } from "@/mocks/data";
import { fakePlayer } from "@/mocks/fakeRoom";
import { lobbySnapshot } from "@/test/room";

const p = FAKE_PLAYERS;
const event = (type: RoomEvent["type"], snapshot: RoomSnapshot, extra: Partial<RoomEvent> = {}): RoomEvent => ({
  type,
  occurredAt: new Date().toISOString(),
  snapshot,
  ...extra,
});

describe("chatLinesFor", () => {
  it("passes chat messages through", () => {
    const chat = { messageId: "m1", userId: p.maya.userId, text: "gg", sentAt: new Date().toISOString() };
    expect(chatLinesFor(event("CHAT_MESSAGE", lobbySnapshot(), { chat }), null, ME_ID)).toEqual([
      { kind: "msg", userId: p.maya.userId, text: "gg" },
    ]);
  });

  it("says who joined, left, dropped and came back, but not your own join", () => {
    const s = lobbySnapshot();
    expect(chatLinesFor(event("PLAYER_JOINED", s, { subjectUserId: p.sam.userId }), null, ME_ID)).toEqual([{ kind: "sys", text: "Sam joined the room" }]);
    expect(chatLinesFor(event("PLAYER_JOINED", s, { subjectUserId: ME_ID }), null, ME_ID)).toEqual([]);
    const before = lobbySnapshot({}, [fakePlayer(p.alex), fakePlayer(p.maya, { status: "DISCONNECTED" })]);
    expect(chatLinesFor(event("PLAYER_DISCONNECTED", before, { subjectUserId: p.maya.userId }), null, ME_ID)).toEqual([
      { kind: "sys", text: "Maya disconnected" },
    ]);
    expect(chatLinesFor(event("PLAYER_RECONNECTED", s, { subjectUserId: p.maya.userId }), before, ME_ID)).toEqual([{ kind: "sys", text: "Maya is back" }]);
    // Someone who left is found in the room before the event.
    const after = lobbySnapshot({}, [fakePlayer(p.alex)]);
    expect(chatLinesFor(event("PLAYER_LEFT", after, { subjectUserId: p.maya.userId }), before, ME_ID)).toEqual([{ kind: "sys", text: "Maya left the room" }]);
  });

  it("marks the quiz: who started it, each question, time's up and the winner", () => {
    expect(chatLinesFor(event("QUIZ_STARTING", lobbySnapshot({ phase: "STARTING" })), null, ME_ID)).toEqual([{ kind: "sys", text: "You started the quiz" }]);
    expect(chatLinesFor(event("QUIZ_STARTING", lobbySnapshot({ phase: "STARTING", roundNumber: 2 })), null, ME_ID)).toEqual([]);
    expect(chatLinesFor(event("QUESTION_STARTED", lobbySnapshot({ phase: "QUESTION", roundNumber: 3 })), null, ME_ID)).toEqual([
      { kind: "sys", text: "Question 3 started" },
    ]);
    expect(chatLinesFor(event("QUESTION_REVEALED", lobbySnapshot()), null, ME_ID)).toEqual([{ kind: "sys", text: "Time's up" }]);
    const won = lobbySnapshot({
      phase: "FINISHED",
      endReason: "COMPLETED",
      scoreboard: [
        { userId: p.maya.userId, displayName: "Maya", totalPoints: 900, pointsLastRound: 0, rank: 1 },
        { userId: ME_ID, displayName: "Alex", totalPoints: 500, pointsLastRound: 0, rank: 2 },
      ],
    });
    expect(chatLinesFor(event("QUIZ_FINISHED", won), null, ME_ID)).toEqual([{ kind: "sys", text: "Maya won the quiz" }]);
    expect(chatLinesFor(event("QUIZ_FINISHED", { ...won, endReason: "VOTED_TO_END" }), null, ME_ID)).toEqual([{ kind: "sys", text: "The quiz ended early" }]);
  });

  it("says how a vote went", () => {
    const voting = lobbySnapshot({
      phase: "VOTE",
      activeVote: { aboutUserId: p.maya.userId, deadline: new Date().toISOString(), pauseVotes: 0, continueVotes: 2, endVotes: 0, votedUserIds: [] },
    });
    expect(chatLinesFor(event("VOTE_RESOLVED", lobbySnapshot({ phase: "STARTING" })), voting, ME_ID)).toEqual([
      { kind: "sys", text: "Vote: continue without Maya" },
    ]);
    expect(chatLinesFor(event("VOTE_RESOLVED", lobbySnapshot({ phase: "PAUSED" })), voting, ME_ID)).toEqual([{ kind: "sys", text: "Vote: wait 1 min" }]);
  });
});
