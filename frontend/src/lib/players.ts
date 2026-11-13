import type { DataStatus, PickableExpression, Player, PlayerLook, RoomSnapshot, ScoreEntry } from "@/lib/api-types";
import { CHARACTER_ORDER, COLOUR_ORDER, FACE_ORDER } from "@/lib/characters";
import { formatEstimate, formatPoints, ordinal } from "@/lib/format";

/**
 * Each player's character in a room: the look the server gave them if it sent one,
 * otherwise the default for their place in the join order (player 1 is the pink Blob,
 * player 2 the cyan Ghost...). Join order never changes during a room, so nobody's
 * character jumps around.
 *
 * Expressions can repeat, so the room never changes yours: `mine` is your favourite face,
 * shown on your own character even before the server sends looks.
 */
export function playerLooks(players: Player[], mine?: { userId: string; expression: PickableExpression }): Map<string, PlayerLook> {
  const looks = new Map<string, PlayerLook>();
  players.forEach((player, index) => {
    const look: PlayerLook = player.look ?? {
      icon: CHARACTER_ORDER[index % CHARACTER_ORDER.length],
      colour: COLOUR_ORDER[index % COLOUR_ORDER.length],
    };
    const expression = player.userId === mine?.userId ? mine.expression : (look.expression ?? FACE_ORDER[index % FACE_ORDER.length]);
    looks.set(player.userId, { ...look, expression });
  });
  return looks;
}

/**
 * Names to show: players who share a display name get their Riot tag, "Sam (NA1)".
 * Without a tag, the second Sam is "Sam (2)".
 */
export function displayNames(players: Player[]): Map<string, string> {
  const counts = new Map<string, number>();
  for (const p of players) counts.set(p.displayName, (counts.get(p.displayName) ?? 0) + 1);
  const seen = new Map<string, number>();
  const names = new Map<string, string>();
  for (const p of players) {
    if ((counts.get(p.displayName) ?? 0) < 2) {
      names.set(p.userId, p.displayName);
      continue;
    }
    const nth = (seen.get(p.displayName) ?? 0) + 1;
    seen.set(p.displayName, nth);
    names.set(p.userId, `${p.displayName} (${p.riotTagLine ?? nth})`);
  }
  return names;
}

/**
 * A player's data status. Uses the planned `dataStatus` when the server sends it, and
 * today's `matchDataReady` flag otherwise.
 */
export function dataStatusOf(player: Player): DataStatus {
  if (player.dataStatus) return player.dataStatus;
  return player.matchDataReady ? "SYNCED" : "SYNCING";
}

export function isReady(player: Player): boolean {
  return dataStatusOf(player) === "SYNCED";
}

/** Who the host is waiting on before Start works (Lobby board), with the player when there is one; empty when everyone is ready. */
export function startWaits(snapshot: RoomSnapshot, names: Map<string, string>): { userId?: string; text: string }[] {
  const waits: { userId?: string; text: string }[] = [];
  const present = snapshot.players.filter((p) => p.status !== "SITTING_OUT");
  if (present.length < 2) waits.push({ text: "You need at least 2 players" });
  for (const p of present) {
    const name = names.get(p.userId) ?? p.displayName;
    const say = (text: string) => waits.push({ userId: p.userId, text });
    switch (dataStatusOf(p)) {
      case "SYNCING":
        say(`${name} is syncing (${formatEstimate(p.dataEstimatedSeconds)})`);
        break;
      case "NOT_SYNCED":
        say(`${name}'s matches need a fresh sync`);
        break;
      case "NOT_LINKED":
        say(`${name} needs to link Riot`);
        break;
      case "FAILED":
        say(`${name}'s matches couldn't sync`);
        break;
      case "SYNCED":
        break;
    }
    if (p.status === "DISCONNECTED") say(`${name} is disconnected`);
  }
  return waits;
}

/** The people who can answer: everyone still playing. */
export function activePlayers(players: Player[]): Player[] {
  return players.filter((p) => p.status !== "SITTING_OUT");
}

/**
 * A steady character for someone outside a room (history lists): the same user always
 * gets the same one. Their face then isn't known, so it's Happy (History boards).
 */
export function lookForUser(userId: string): PlayerLook {
  let hash = 0;
  for (const char of userId) hash = (hash * 31 + char.charCodeAt(0)) >>> 0;
  return {
    icon: CHARACTER_ORDER[hash % CHARACTER_ORDER.length],
    colour: COLOUR_ORDER[(hash >>> 4) % COLOUR_ORDER.length],
    expression: "happy",
  };
}

/**
 * Where you stand before the next question (Get ready board): "You're 2nd" and
 * "3,150 points · 30 behind Maya". The gap is to the nearest player ahead of you; at the
 * top, it's who you're level with or ahead of. Null when you're not on the scoreboard.
 */
export function yourStanding(board: ScoreEntry[], meId: string, names: Map<string, string>): { title: string; line: string } | null {
  const me = board.find((e) => e.userId === meId);
  if (!me) return null;
  const others = board.filter((e) => e.userId !== meId);
  const name = (e: ScoreEntry) => names.get(e.userId) ?? e.displayName;
  const ahead = others.filter((e) => e.totalPoints > me.totalPoints).sort((a, b) => a.totalPoints - b.totalPoints)[0];
  const level = others.find((e) => e.totalPoints === me.totalPoints);
  const behind = others.filter((e) => e.totalPoints < me.totalPoints).sort((a, b) => b.totalPoints - a.totalPoints)[0];
  const gap = ahead
    ? `${formatPoints(ahead.totalPoints - me.totalPoints)} behind ${name(ahead)}`
    : level
      ? `level with ${name(level)}`
      : behind
        ? `${formatPoints(me.totalPoints - behind.totalPoints)} ahead of ${name(behind)}`
        : null;
  const points = `${formatPoints(me.totalPoints)} ${me.totalPoints === 1 ? "point" : "points"}`;
  return { title: `You're ${ordinal(me.rank)}`, line: gap ? `${points} · ${gap}` : points };
}

/**
 * A finished quiz that stopped before the last question. Today's room contract has no end
 * reason (it's planned), so a missing one means the quiz finished normally.
 */
export function endedEarly(snapshot: Pick<RoomSnapshot, "endReason">): boolean {
  return Boolean(snapshot.endReason && snapshot.endReason !== "COMPLETED");
}
