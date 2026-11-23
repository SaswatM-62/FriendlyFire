import { describe, expect, it } from "vitest";
import type { Me } from "@/lib/api-types";
import { isPlaceholderName, needsWelcome, roomCodeIn } from "@/lib/onboarding";

const newPlayer: Me = { userId: "u1", displayName: "Player 4821", email: "a@example.com", createdAt: "2026-11-18T10:00:00Z" };
const done: Me = { ...newPlayer, onboardedAt: "2026-11-18T10:05:00Z" };

describe("needsWelcome", () => {
  it("sends a new player to the tutorial from pages that need sign-in", () => {
    for (const page of ["/", "/profile", "/history", "/room/K7QM2X"]) expect(needsWelcome(page, newPlayer)).toBe(true);
  });

  it("never redirects the tutorial, the open pages or the Riot callback", () => {
    for (const page of ["/welcome", "/terms", "/privacy", "/how-it-works", "/link/riot/callback", "/verify-email"]) {
      expect(needsWelcome(page, newPlayer)).toBe(false);
    }
  });

  it("leaves finished players and unknown users alone", () => {
    expect(needsWelcome("/profile", done)).toBe(false);
    expect(needsWelcome("/profile", null)).toBe(false);
  });
});

describe("roomCodeIn", () => {
  it("finds the code in a room link only", () => {
    expect(roomCodeIn("/room/k7qm2x")).toBe("K7QM2X");
    expect(roomCodeIn("/profile")).toBeNull();
  });
});

describe("isPlaceholderName", () => {
  it("spots an email sign-up's suggested name", () => {
    expect(isPlaceholderName("Player 4821")).toBe(true);
    expect(isPlaceholderName("Sam")).toBe(false);
    expect(isPlaceholderName("Player 4821x")).toBe(false);
  });
});
