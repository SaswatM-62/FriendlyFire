import type { Me, QuizSummary, ScoreboardEntry } from "@/lib/api-types";

/**
 * Test data for the fake backend (FF-513) and the Vitest tests. Made-up players and
 * stats: nothing here comes from real Riot accounts.
 */

/** Marks the fake backend's code, so a test can check production builds don't contain it. */
export const FAKE_BACKEND_MARKER = "ff-fake-backend-v1";

export const ME_ID = "u-alex";

export const FAKE_ME: Me = {
  userId: ME_ID,
  displayName: "Alex",
  email: "alex@example.com",
  createdAt: "2026-10-12T18:20:00Z",
  riotLink: { gameName: "Alex", tagLine: "NA1", region: "na", linkedAt: "2026-10-12T18:24:00Z" },
};

export interface FakePlayer {
  userId: string;
  displayName: string;
  riotTagLine: string;
}

export const FAKE_PLAYERS: Record<string, FakePlayer> = {
  alex: { userId: ME_ID, displayName: "Alex", riotTagLine: "NA1" },
  maya: { userId: "u-maya", displayName: "Maya", riotTagLine: "NA1" },
  sam: { userId: "u-sam", displayName: "Sam", riotTagLine: "EUW" },
  jordan: { userId: "u-jordan", displayName: "Jordan", riotTagLine: "7777" },
  riley: { userId: "u-riley", displayName: "Riley", riotTagLine: "RLY" },
  casey: { userId: "u-casey", displayName: "Casey", riotTagLine: "CSY" },
};

export interface FakeQuestion {
  text: string;
  /** {name} becomes the correct player's name. */
  explanation: string;
}

export const FAKE_QUESTIONS: FakeQuestion[] = [
  {
    text: "Who is most likely to hit the leg of an opponent in the game?",
    explanation: "{name} hit 18% of their shots in the legs over 41 matches. Nobody else came close.",
  },
  {
    text: "Who had the most clutches in the last month?",
    explanation: "{name} won 9 clutches in 34 matches.",
  },
  {
    text: "Who has the highest headshot rate?",
    explanation: "{name} landed 31% headshots across their last 38 matches.",
  },
  {
    text: "Who locked in a Duelist most often?",
    explanation: "{name} played a Duelist in 27 of their 40 matches.",
  },
  {
    text: "Who dealt the most damage in a single round?",
    explanation: "{name} dealt 612 damage in one round on Ascent.",
  },
  {
    text: "Who got the most first bloods?",
    explanation: "{name} got the first kill 64 times in the last 30 days.",
  },
  {
    text: "Who bought the Operator most often?",
    explanation: "{name} bought an Operator 41 times. Expensive habit.",
  },
  {
    text: "Who has the best win rate on Lotus?",
    explanation: "{name} won 7 of their 9 matches on Lotus.",
  },
];

const DAY = 24 * 60 * 60 * 1000;

function board(entries: [string, string, number, number][]): ScoreboardEntry[] {
  const sorted = [...entries].sort((a, b) => b[2] - a[2]);
  return sorted.map(([userId, displayName, totalPoints, correctAnswers]) => ({
    userId,
    displayName,
    totalPoints,
    correctAnswers,
    // Equal points share a rank (1, 1, 3).
    rank: sorted.findIndex((e) => e[2] === totalPoints) + 1,
  }));
}

/** Fourteen past quizzes, newest first, so the history page has a second page. */
export function fakeHistory(now: number): QuizSummary[] {
  const p = FAKE_PLAYERS;
  const quizzes: Omit<QuizSummary, "startedAt" | "finishedAt">[] = [
    {
      quizId: "q-014",
      hostDisplayName: "Maya",
      totalRounds: 10,
      roundsPlayed: 10,
      secondsPerQuestion: 20,
      endReason: "COMPLETED",
      finalScoreboard: board([
        [p.maya.userId, "Maya", 7420, 8],
        [ME_ID, "Alex", 6980, 7],
        [p.sam.userId, "Sam", 6150, 6],
        ["u-deleted", "Deleted player", 5300, 5],
        [p.riley.userId, "Riley", 4210, 4],
      ]),
    },
    {
      quizId: "q-013",
      hostDisplayName: "Alex",
      totalRounds: 15,
      roundsPlayed: 15,
      secondsPerQuestion: 20,
      endReason: "COMPLETED",
      finalScoreboard: board([
        [ME_ID, "Alex", 10340, 12],
        [p.maya.userId, "Maya", 9120, 11],
        [p.riley.userId, "Riley", 7010, 9],
      ]),
    },
    {
      quizId: "q-012",
      hostDisplayName: "Jordan",
      totalRounds: 10,
      roundsPlayed: 10,
      secondsPerQuestion: 25,
      endReason: "COMPLETED",
      finalScoreboard: board([
        [p.sam.userId, "Sam", 7810, 9],
        [p.jordan.userId, "Jordan", 6520, 7],
        [p.maya.userId, "Maya", 6100, 7],
        [ME_ID, "Alex", 5880, 6],
        [p.casey.userId, "Casey", 3010, 4],
      ]),
    },
    {
      quizId: "q-011",
      hostDisplayName: "Sam",
      totalRounds: 5,
      roundsPlayed: 3,
      secondsPerQuestion: 15,
      endReason: "VOTED_TO_END",
      finalScoreboard: board([
        [p.casey.userId, "Casey", 2140, 3],
        [p.sam.userId, "Sam", 1820, 2],
        [ME_ID, "Alex", 1500, 2],
        [p.maya.userId, "Maya", 610, 1],
      ]),
    },
  ];
  // Ten more, older, so "Load more" has something to load.
  for (let i = 10; i >= 1; i--) {
    const winner = i % 3 === 0;
    quizzes.push({
      quizId: `q-${String(i).padStart(3, "0")}`,
      hostDisplayName: i % 2 === 0 ? "Riley" : "Alex",
      totalRounds: 10,
      roundsPlayed: 10,
      secondsPerQuestion: 20,
      endReason: "COMPLETED",
      finalScoreboard: board([
        [ME_ID, "Alex", winner ? 7200 + i * 10 : 5100 + i * 10, winner ? 8 : 6],
        [p.riley.userId, "Riley", 6400 + i * 7, 7],
        [p.maya.userId, "Maya", 4300 + i * 5, 5],
      ]),
    });
  }
  const ages = [2, 5, 9, 21, 30, 33, 37, 41, 45, 49, 52, 56, 60, 64];
  return quizzes.map((quiz, i) => {
    const finishedAt = now - ages[i] * DAY;
    return {
      ...quiz,
      startedAt: new Date(finishedAt - 12 * 60 * 1000).toISOString(),
      finishedAt: new Date(finishedAt).toISOString(),
    };
  });
}
