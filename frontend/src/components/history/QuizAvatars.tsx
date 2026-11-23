"use client";

import { useFavouriteLook } from "@/components/chrome/useFavouriteLook";
import { Mini } from "@/components/ui/Character";
import type { PlayerLook, QuizSummary, ScoreboardEntry } from "@/lib/api-types";
import { lookForUser } from "@/lib/players";

export const DELETED_PLAYER = "Deleted player";

/** Your place's colour in lists: 1st mint, 2nd pink, 3rd violet, then cyan (History board). */
export function placeTone(rank: number): string {
  return ["mint", "pink", "violet"][rank - 1] ?? "cyan";
}

export function isDeleted(entry: ScoreboardEntry): boolean {
  return entry.displayName === DELETED_PLAYER;
}

/** The character to show for someone in a past quiz: yours, or a steady one for them. */
export function usePastLook(meId: string): (userId: string) => PlayerLook {
  const mine = useFavouriteLook();
  return (userId) => (userId === meId ? mine : lookForUser(userId));
}

/** A dashed circle for players who deleted their account (History detail board). */
export function DeletedAvatar({ size = 32 }: { size?: number }) {
  return <span className="ghostav" style={{ width: size, height: size }} role="img" aria-label={DELETED_PLAYER} />;
}

/** Who else played a quiz: up to four small characters, then "+2" (History board). */
export function QuizAvatars({ quiz, meId, max = 4 }: { quiz: QuizSummary; meId: string; max?: number }) {
  const lookOf = usePastLook(meId);
  const others = quiz.finalScoreboard.filter((e) => e.userId !== meId);
  const names = others.map((e) => e.displayName).join(", ");
  return (
    <span className="who" role="img" aria-label={`Played with ${names || "nobody"}`}>
      {others.slice(0, max).map((e) =>
        isDeleted(e) ? (
          <span key={e.userId} className="mini" style={{ borderStyle: "dashed", borderColor: "var(--line-strong)" }} />
        ) : (
          <Mini key={e.userId} look={lookOf(e.userId)} />
        ),
      )}
      {others.length > max && (
        <span className="small faint" style={{ marginLeft: 6 }}>
          +{others.length - max}
        </span>
      )}
    </span>
  );
}
