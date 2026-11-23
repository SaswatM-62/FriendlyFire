import { describe, expect, it } from "vitest";
import { ClockOffset, msUntil } from "@/lib/clock";
import { formatClock, formatEstimate, formatPoints, ordinal, relativeTime, roomTitle } from "@/lib/format";
import { backoffMs, qualityFromRtt } from "@/lib/net";
import { displayNames, endedEarly, lookForUser, playerLooks, startWaits, yourStanding } from "@/lib/players";
import { FAKE_PLAYERS } from "@/mocks/data";
import { fakePlayer } from "@/mocks/fakeRoom";
import { lobbySnapshot } from "@/test/room";

const p = FAKE_PLAYERS;

describe("format", () => {
  it("writes ordinals", () => {
    expect([1, 2, 3, 4, 11, 12, 13, 21, 22, 23].map(ordinal)).toEqual([
      "1st",
      "2nd",
      "3rd",
      "4th",
      "11th",
      "12th",
      "13th",
      "21st",
      "22nd",
      "23rd",
    ]);
  });

  it("writes points, clocks and estimates", () => {
    expect(formatPoints(3180)).toBe("3,180");
    expect(formatClock(12)).toBe("0:12");
    expect(formatClock(125)).toBe("2:05");
    expect(formatClock(-3)).toBe("0:00");
    expect(formatEstimate(170)).toBe("~3 min");
    expect(formatEstimate(20)).toBe("~1 min");
    expect(formatEstimate(undefined)).toBe("a few min");
  });

  it("says how long ago", () => {
    const now = Date.parse("2026-11-09T12:00:00Z");
    const ago = (ms: number) => relativeTime(new Date(now - ms).toISOString(), now);
    expect(ago(10_000)).toBe("just now");
    expect(ago(5 * 60_000)).toBe("5 min ago");
    expect(ago(3 * 3_600_000)).toBe("3 hours ago");
    expect(ago(30 * 3_600_000)).toBe("yesterday");
    expect(ago(2 * 86_400_000)).toBe("2 days ago");
    expect(ago(9 * 86_400_000)).toBe("last week");
    expect(ago(21 * 86_400_000)).toBe("3 weeks ago");
  });

  it("names rooms after their host", () => {
    expect(roomTitle("Maya")).toBe("Maya's room");
    expect(roomTitle("Chris")).toBe("Chris' room");
    expect(roomTitle(undefined)).toBe("Quiz");
  });
});

describe("network bar", () => {
  it("grades round trips: GOOD under 150 ms, FAIR to 300 ms, POOR above or after a missed ping", () => {
    expect(qualityFromRtt(149, 0)).toBe("GOOD");
    expect(qualityFromRtt(150, 0)).toBe("FAIR");
    expect(qualityFromRtt(300, 0)).toBe("FAIR");
    expect(qualityFromRtt(301, 0)).toBe("POOR");
    expect(qualityFromRtt(40, 1)).toBe("POOR");
  });

  it("backs off 1, 2, 4… seconds, capped at 30", () => {
    expect([0, 1, 2, 3, 4, 5, 9].map(backoffMs)).toEqual([1000, 2000, 4000, 8000, 16000, 30000, 30000]);
  });
});

describe("clock offset", () => {
  it("trusts the fastest round trip", () => {
    const clock = new ClockOffset();
    // Server clock is 5 s ahead. A slow round trip skews the estimate; a fast one doesn't.
    clock.addSample(1_000, 1_600, 6_500); // rtt 600 -> offset 5_200
    clock.addSample(2_000, 2_040, 7_020); // rtt 40 -> offset 5_000
    expect(clock.offsetMs).toBe(5_000);
  });

  it("counts down to a deadline by the server's clock", () => {
    const deadline = new Date(10_000).toISOString();
    expect(msUntil(deadline, 4_000, 1_000)).toBe(5_000);
    expect(msUntil(deadline, 20_000, 0)).toBe(0);
    expect(msUntil(undefined, 0, 0)).toBe(0);
  });
});

describe("players", () => {
  it("tells apart players with the same name by their Riot tag", () => {
    const names = displayNames([
      fakePlayer(p.sam),
      fakePlayer({ ...p.maya, displayName: "Sam", riotTagLine: "NA1" }),
      fakePlayer(p.jordan),
    ]);
    expect([...names.values()]).toEqual(["Sam (EUW)", "Sam (NA1)", "Jordan"]);
  });

  it("gives player N the Nth default character and face, so nobody's changes mid-room", () => {
    const looks = playerLooks([fakePlayer(p.alex), fakePlayer(p.maya)]);
    expect(looks.get(p.alex.userId)).toEqual({ icon: "blob", colour: "pink", expression: "cocky" });
    expect(looks.get(p.maya.userId)).toEqual({ icon: "ghost", colour: "cyan", expression: "smirk" });
  });

  it("keeps your favourite face (faces can repeat) and the server's looks", () => {
    const looks = playerLooks(
      [fakePlayer(p.alex), fakePlayer(p.maya, { look: { icon: "gem", colour: "lime", expression: "shades", random: true } })],
      { userId: p.alex.userId, expression: "wink" },
    );
    expect(looks.get(p.alex.userId)).toEqual({ icon: "blob", colour: "pink", expression: "wink" });
    expect(looks.get(p.maya.userId)).toEqual({ icon: "gem", colour: "lime", expression: "shades", random: true });
  });

  it("shows people from past quizzes with a steady character and a Happy face", () => {
    expect(lookForUser("u-maya")).toEqual(lookForUser("u-maya"));
    expect(lookForUser("u-maya").expression).toBe("happy");
  });

  it("says where you stand: the gap to the player ahead, or who you lead", () => {
    const entry = (userId: string, displayName: string, totalPoints: number, rank: number) => ({ userId, displayName, totalPoints, rank, pointsLastRound: 0 });
    const names = new Map([[p.maya.userId, "Maya"]]);
    const board = [entry(p.maya.userId, "Maya", 3180, 1), entry(p.alex.userId, "Alex", 3150, 2), entry(p.sam.userId, "Sam", 1, 3)];

    expect(yourStanding(board, p.alex.userId, names)).toEqual({ title: "You're 2nd", line: "3,150 points · 30 behind Maya" });
    // Sam has left the room: their name comes from the scoreboard.
    expect(yourStanding(board, p.maya.userId, names)?.line).toBe("3,180 points · 30 ahead of Alex");
    expect(yourStanding(board, p.sam.userId, names)?.line).toBe("1 point · 3,149 behind Alex");
    expect(yourStanding([entry(p.alex.userId, "Alex", 500, 1), entry(p.maya.userId, "Maya", 500, 1)], p.alex.userId, names)).toEqual({
      title: "You're 1st",
      line: "500 points · level with Maya",
    });
    expect(yourStanding(board, "someone-else", names)).toBeNull();
  });

  it("treats a quiz with no end reason (today's contract) as finished normally", () => {
    expect(endedEarly({})).toBe(false);
    expect(endedEarly({ endReason: "COMPLETED" })).toBe(false);
    expect(endedEarly({ endReason: "VOTED_TO_END" })).toBe(true);
    expect(endedEarly({ endReason: "NOT_ENOUGH_PLAYERS" })).toBe(true);
  });

  it("says who the host is waiting on before Start works", () => {
    const snapshot = lobbySnapshot({}, [
      fakePlayer(p.alex),
      fakePlayer(p.maya, { dataStatus: "SYNCING", dataEstimatedSeconds: 120 }),
      fakePlayer(p.casey, { dataStatus: "NOT_LINKED" }),
    ]);
    const names = displayNames(snapshot.players);
    expect(startWaits(snapshot, names)).toEqual([
      { userId: p.maya.userId, text: "Maya is syncing (~2 min)" },
      { userId: p.casey.userId, text: "Casey needs to link Riot" },
    ]);
    expect(startWaits(lobbySnapshot({}, [fakePlayer(p.alex)]), names)).toEqual([{ text: "You need at least 2 players" }]);
    expect(startWaits(lobbySnapshot(), names)).toEqual([]);
  });

  it("reads today's matchDataReady flag when the planned dataStatus isn't there", () => {
    const snapshot = lobbySnapshot({}, [
      { ...fakePlayer(p.alex), dataStatus: undefined, matchDataReady: true },
      { ...fakePlayer(p.maya), dataStatus: undefined, matchDataReady: false },
    ]);
    expect(startWaits(snapshot, displayNames(snapshot.players))).toEqual([{ userId: p.maya.userId, text: "Maya is syncing (a few min)" }]);
  });
});
