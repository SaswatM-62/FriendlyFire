"use client";

import type { ReactNode } from "react";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { StageCard } from "@/components/room/StageCard";
import { Av } from "@/components/ui/Character";
import { Icon, type IconName } from "@/components/ui/Icon";
import { activePlayers } from "@/lib/players";
import { useNow } from "@/lib/useNow";

/** The loading screen lasts at least this long (Room's minimum), so everyone can read it. */
export const PREPARING_MIN_MS = 8_000;
/** After this long, say it's taking longer than usual (Room gives up at 60 s). */
export const PREPARING_SLOW_MS = 20_000;

function Rule({ icon, title, children }: { icon: IconName; title: string; children: ReactNode }) {
  return (
    <li>
      <span style={{ color: "var(--orange-text)" }}>
        <Icon name={icon} size={18} />
      </span>
      <span>
        <b>{title}.</b> <span className="muted">{children}</span>
      </span>
    </li>
  );
}

/**
 * The loading screen (PREPARING): the host pressed Start and the quiz engine is building
 * the questions. Everyone in the room sees how a quiz works while they wait; the bar
 * fills over Room's 8-second minimum, then pulses until the questions are ready.
 */
export function PreparingScreen() {
  const { snapshot, meId, clockOffsetMs } = useRoom();
  const info = usePlayerInfo();
  const now = useNow(250);
  const { questionCount, secondsPerQuestion } = snapshot.settings;

  // phaseDeadline is Start + 8 s, so Start is 8 s before it (msUntil stops at 0, so measure past it here).
  const deadline = snapshot.phaseDeadline ? Date.parse(snapshot.phaseDeadline) : NaN;
  const elapsed = now === 0 || Number.isNaN(deadline) ? 0 : now + clockOffsetMs - (deadline - PREPARING_MIN_MS);
  const filled = elapsed >= PREPARING_MIN_MS;
  const slow = elapsed >= PREPARING_SLOW_MS;
  const fraction = Math.min(1, Math.max(0, elapsed / PREPARING_MIN_MS));

  const playing = activePlayers(snapshot.players);
  const fewMatches = playing.filter((p) => p.noRecentMatches);
  const spectating = snapshot.players.find((p) => p.userId === meId)?.status === "SPECTATING";

  return (
    <StageCard>
      <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        <div role="status">
          <h1 className="title">Building your questions…</h1>
        </div>
        <div className="track" aria-hidden="true" style={{ flex: "none" }}>
          <div className={`fill${filled ? " prep-wait" : ""}`} style={{ width: `${fraction * 100}%`, background: "var(--orange)" }} />
        </div>
        {slow && (
          <p className="small" role="status" style={{ color: "var(--yellow-text)" }}>
            Taking longer than usual…
          </p>
        )}
      </div>

      <section aria-labelledby="h-rules" style={{ display: "flex", flexDirection: "column", gap: 12 }}>
        <h2 id="h-rules" className="label">
          How a quiz works
        </h2>
        <ul className="rules" style={{ gap: 10 }}>
          <Rule icon="users" title="It's about you">
            Every question is about someone in this room, from your last 30 days of Competitive and Unrated matches.
          </Rule>
          <Rule icon="target" title="Pick a player">
            Every player in the room is an option, so yes, sometimes the answer is you.
          </Rule>
          <Rule icon="clock" title="Beat the clock">
            You get {secondsPerQuestion} seconds per question. One answer each: it locks in when you pick.
          </Rule>
          <Rule icon="star" title="Points">
            A right answer scores 500, plus up to 500 more the faster you are. A wrong answer or no answer scores 0.
          </Rule>
          <Rule icon="flag" title="The reveal">
            After each question you see the answer, who picked what, and the updated scoreboard.
          </Rule>
        </ul>
        {fewMatches.map((p) => (
          <p key={p.userId} className="small muted">
            {info(p.userId).name} doesn&apos;t have enough recent matches, so there are no questions about them. They still play.
          </p>
        ))}
        <p className="small faint">If someone drops out, the room votes to wait for them, carry on or end the quiz.</p>
        {spectating && (
          <p className="small" style={{ color: "var(--cyan-text)" }}>
            You joined while this quiz was starting, so you&apos;re watching this one. You&apos;ll play in the next.
          </p>
        )}
      </section>

      <footer className="spread" style={{ flexWrap: "wrap" }}>
        <span className="small muted">
          <span className="num" style={{ color: "var(--text)" }}>
            {questionCount}
          </span>{" "}
          questions ·{" "}
          <span className="num" style={{ color: "var(--text)" }}>
            {secondsPerQuestion}
          </span>{" "}
          seconds each
        </span>
        <span style={{ display: "flex", gap: 4 }} role="img" aria-label={`${playing.length} players`}>
          {playing.map((p) => (
            <Av key={p.userId} look={info(p.userId).look} size={28} />
          ))}
        </span>
      </footer>
    </StageCard>
  );
}
