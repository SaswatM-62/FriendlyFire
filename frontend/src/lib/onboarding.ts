import type { Me } from "@/lib/api-types";

/**
 * Pages a new player can open before finishing the tutorial: the tutorial itself, the
 * pages that don't need sign-in, the Riot callback (linking happens inside the
 * tutorial) and the sign-in gates.
 */
const OPEN_BEFORE_WELCOME = ["/welcome", "/terms", "/privacy", "/how-it-works", "/link/riot/callback", "/login", "/verify-email", "/invite-only", "/status"];

/** Whether this signed-in player must go through /welcome before seeing `pathname`. */
export function needsWelcome(pathname: string, me: Me | null): boolean {
  if (!me || me.onboardedAt) return false;
  return !OPEN_BEFORE_WELCOME.some((open) => pathname === open || pathname.startsWith(`${open}/`));
}

/** The room code in a /room/K7QM2X link, kept so the tutorial's last step can offer it. */
export function roomCodeIn(pathname: string): string | null {
  const match = /^\/room\/([A-Za-z0-9]{6})(?:\/|$)/.exec(pathname);
  return match ? match[1].toUpperCase() : null;
}

/** "Player 4821": the name an email sign-up gets until they pick one (Account's NewUserNames). */
export function isPlaceholderName(name: string): boolean {
  return /^Player \d{4}$/.test(name);
}
