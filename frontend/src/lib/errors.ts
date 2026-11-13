/**
 * Error codes the frontend reacts to, and what to tell the player. The Gateway sends
 * {"error": {"code", "message"}} (FF-006); its message is safe to show, but these read
 * better where we know the code. Codes not listed fall back to the Gateway's message.
 */
export const ERROR_CODES = {
  network: "NETWORK",
  unauthenticated: "UNAUTHENTICATED",
  notImplemented: "NOT_IMPLEMENTED",
  /** FF-103: email/password accounts must verify their email first. */
  emailNotVerified: "EMAIL_NOT_VERIFIED",
  /** FF-618: the live site only lets invited testers in until Riot approves it. */
  inviteOnly: "INVITE_ONLY",
  /** ADR-017: one live WebSocket per player; a newer one replaced this one. */
  alreadyConnected: "ALREADY_CONNECTED",
  /** ADR-022: the account was deleted (on another tab or device), so its WebSocket closed. */
  accountDeleted: "ACCOUNT_DELETED",
  roomNotFound: "ROOM_NOT_FOUND",
  roomFull: "ROOM_FULL",
  quizAlreadyStarted: "QUIZ_ALREADY_STARTED",
  notInRoom: "NOT_IN_ROOM",
  riotAlreadyLinked: "RIOT_ALREADY_LINKED",
  invalidLinkState: "INVALID_LINK_STATE",
} as const;

const MESSAGES: Record<string, string> = {
  [ERROR_CODES.network]: "Can't reach FriendlyFire. Check your connection and try again.",
  [ERROR_CODES.unauthenticated]: "Your sign-in has expired. Sign in again.",
  [ERROR_CODES.notImplemented]: "That part of FriendlyFire isn't ready yet.",
  [ERROR_CODES.roomNotFound]: "Room not found. Check the code and try again.",
  [ERROR_CODES.roomFull]: "That room is full (8 players).",
  [ERROR_CODES.quizAlreadyStarted]: "That quiz has already started. Ask them to play again after it ends.",
  [ERROR_CODES.riotAlreadyLinked]: "That Riot account is already linked to another FriendlyFire account.",
  [ERROR_CODES.invalidLinkState]: "That link request expired. Start linking again.",
};

/** A code for an HTTP status, when the Gateway didn't send the standard error body. */
export function codeForStatus(status: number): string {
  switch (status) {
    case 0:
      return ERROR_CODES.network;
    case 401:
      return ERROR_CODES.unauthenticated;
    case 403:
      return "PERMISSION_DENIED";
    case 404:
      return "NOT_FOUND";
    case 409:
      return "CONFLICT";
    case 501:
      return ERROR_CODES.notImplemented;
    case 503:
      return "UNAVAILABLE";
    default:
      return status >= 500 ? "INTERNAL" : "BAD_REQUEST";
  }
}

/** What to tell the player about an error. */
export function messageFor(code: string, serverMessage?: string): string {
  return (
    MESSAGES[code] ??
    (serverMessage && serverMessage.trim() !== "" ? serverMessage : "Something went wrong. Try again in a moment.")
  );
}
