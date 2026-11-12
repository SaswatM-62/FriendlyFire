import type { RoomEvent, RoomSnapshot } from "@/lib/api-types";
import { endedEarly } from "@/lib/players";

/** A chat line before it gets an id: something a player said, or what just happened in the room. */
export type NewChatLine = { kind: "msg"; userId: string; text: string } | { kind: "sys"; text: string };

/** One line in the room chat, with an id for React. */
export type ChatLine = NewChatLine & { id: string };

/** The most lines the chat keeps; older ones scroll away for good. */
export const CHAT_KEEP = 200;

/**
 * The chat lines for one room event: the message itself on CHAT_MESSAGE (planned), and a
 * short system line for joins, leaves, questions, votes and the result. These come
 * from events the room already sends, so they work before chat reaches the server.
 */
export function chatLinesFor(event: RoomEvent, previous: RoomSnapshot | null, meId: string): NewChatLine[] {
  const s = event.snapshot;
  const nameOf = (id?: string) =>
    id === meId
      ? "You"
      : (s.players.find((p) => p.userId === id)?.displayName ?? previous?.players.find((p) => p.userId === id)?.displayName ?? "Someone");
  const who = nameOf(event.subjectUserId);
  const sys = (text: string): NewChatLine[] => [{ kind: "sys", text }];

  switch (event.type) {
    case "CHAT_MESSAGE":
      return event.chat ? [{ kind: "msg", userId: event.chat.userId, text: event.chat.text }] : [];
    case "PLAYER_JOINED":
      return event.subjectUserId === meId ? [] : sys(`${who} joined the room`);
    case "PLAYER_LEFT":
      return sys(`${who} left the room`);
    case "PLAYER_KICKED":
      return sys(event.subjectUserId === meId ? "You were removed from the room" : `${who} was removed from the room`);
    case "PLAYER_DISCONNECTED":
      return sys(`${who} disconnected`);
    case "PLAYER_RECONNECTED": {
      const was = previous?.players.find((p) => p.userId === event.subjectUserId)?.status;
      return was === "DISCONNECTED" ? sys(`${who} is back`) : [];
    }
    case "HOST_CHANGED":
      return sys(event.subjectUserId === meId ? "You're the host now" : `${who} is now the host`);
    case "QUIZ_STARTING":
      return s.roundNumber === 0 ? sys(`${nameOf(s.hostUserId)} started the quiz`) : [];
    case "QUESTION_STARTED":
      return sys(`Question ${s.roundNumber} started`);
    case "QUESTION_REVEALED":
      return sys("Time's up");
    case "VOTE_RESOLVED": {
      const about = nameOf(previous?.activeVote?.aboutUserId ?? event.subjectUserId);
      if (s.phase === "PAUSED") return sys("Vote: wait 1 min");
      if (s.phase === "FINISHED") return sys("Vote: end the quiz");
      return sys(`Vote: continue without ${about}`);
    }
    case "QUIZ_PAUSED":
      return sys(`Paused: waiting for ${nameOf(s.waitingForUserId ?? event.subjectUserId)}`);
    case "QUIZ_FINISHED": {
      if (endedEarly(s)) return sys("The quiz ended early");
      const winners = s.scoreboard.filter((e) => e.rank === 1);
      if (winners.length > 1) return sys("It's a tie!");
      return winners[0] ? sys(`${nameOf(winners[0].userId)} won the quiz`) : [];
    }
    default:
      return [];
  }
}
