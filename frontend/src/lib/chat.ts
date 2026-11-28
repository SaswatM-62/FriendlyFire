import type { ChatMessage, RoomEvent, RoomSnapshot } from "@/lib/api-types";
import { endedEarly } from "@/lib/players";

/** A chat line before it gets an id: something a player said, or what just happened in the room. */
export type NewChatLine =
  | {
      kind: "msg";
      userId: string;
      text: string;
      /** The sender's name when they sent it, for someone no longer in the room. */
      name?: string;
      /** The server's ID, so a message isn't shown twice after the history loads. */
      messageId?: string;
    }
  | { kind: "sys"; text: string };

/** One line in the room chat, with an id for React. */
export type ChatLine = NewChatLine & { id: string };

/** The most lines the chat keeps; older ones scroll away for good. */
export const CHAT_KEEP = 200;

/** Every connected player answered, so the reveal came early (Room, FF-418). */
function everyoneAnswered(s: RoomSnapshot): boolean {
  const connected = s.players.filter((p) => p.status === "CONNECTED");
  return connected.length > 0 && connected.every((p) => p.answeredCurrentQuestion);
}

/**
 * The chat lines for one room event: the message itself on CHAT_MESSAGE, and a short
 * system line for joins, leaves, questions, votes and the result (from events the room
 * already sends).
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
      return event.chat ? [messageLine(event.chat)] : [];
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
      return sys(everyoneAnswered(s) ? "Everyone locked in" : "Time's up");
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

function messageLine(message: ChatMessage): NewChatLine {
  return {
    kind: "msg",
    userId: message.userId,
    text: message.text,
    messageId: message.messageId,
    ...(message.displayName ? { name: message.displayName } : {}),
  };
}

/**
 * Adds the room's kept messages (GET /api/rooms/{code}/chat) to the chat, skipping any
 * already shown. On the first load they go before everything else (they're older);
 * after a rejoin, the ones missed while away go at the end.
 */
export function withHistory(
  lines: ChatLine[],
  messages: ChatMessage[],
  newId: () => string,
  where: "before" | "after",
): ChatLine[] {
  const shown = new Set(lines.flatMap((line) => (line.kind === "msg" && line.messageId ? [line.messageId] : [])));
  const missing = messages.filter((m) => !shown.has(m.messageId)).map((m): ChatLine => ({ ...messageLine(m), id: newId() }));
  if (missing.length === 0) return lines;
  return (where === "before" ? [...missing, ...lines] : [...lines, ...missing]).slice(-CHAT_KEEP);
}
