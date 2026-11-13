"use client";

import { useState } from "react";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { StageCard } from "@/components/room/StageCard";
import { Av } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import type { VoteOption } from "@/lib/api-types";
import { msUntil } from "@/lib/clock";
import { formatClock } from "@/lib/format";
import { useNow } from "@/lib/useNow";

const OPTIONS: { option: VoteOption; label: (name: string) => string }[] = [
  { option: "PAUSE", label: () => "Wait 1 min" },
  { option: "CONTINUE_WITHOUT_PLAYER", label: (name) => `Continue without ${name}` },
  { option: "END_QUIZ", label: () => "End quiz" },
];

/** The pause budget for a whole quiz (ADR-026). */
const PAUSE_BUDGET_S = 180;

/** The vote after someone drops (FF-509, Vote board): wait, carry on without them, or end. */
export function VoteScreen() {
  const { snapshot, meId, clockOffsetMs, send } = useRoom();
  const info = usePlayerInfo();
  const now = useNow(250);
  const vote = snapshot.activeVote!;
  const about = info(vote.aboutUserId);
  const [myVote, setMyVote] = useState<VoteOption | null>(null);
  const counts: Record<VoteOption, number> = {
    PAUSE: vote.pauseVotes,
    CONTINUE_WITHOUT_PLAYER: vote.continueVotes,
    END_QUIZ: vote.endVotes,
  };
  const voted = myVote !== null || vote.votedUserIds.includes(meId);
  const voters = snapshot.players.filter((p) => p.status === "CONNECTED" && p.userId !== vote.aboutUserId);
  const left = now === 0 ? null : msUntil(vote.deadline, now, clockOffsetMs) / 1000;

  const cast = (option: VoteOption) => {
    if (voted) return;
    setMyVote(option);
    send("vote", { option });
  };

  return (
    <StageCard center vote>
      <p className="row muted" style={{ gap: 10 }}>
        <Av look={about.look} size={30} face="away" away />
        {about.name} disconnected.
      </p>
      <div>
        <h1 className="title">What should we do?</h1>
        <p className="muted" aria-live="polite">
          Vote closes in{" "}
          <span className="num" style={{ color: "var(--text)" }}>
            {left === null ? "–" : formatClock(left)}
          </span>
        </p>
      </div>
      <div className="opts" role="group" aria-label="Vote">
        {OPTIONS.map(({ option, label }) => {
          const mine = myVote === option;
          const unavailable = option === "PAUSE" && vote.pauseAllowed === false;
          return (
            <button
              key={option}
              type="button"
              className={`opt${mine ? " opt--mine" : ""}`}
              aria-pressed={mine}
              disabled={unavailable || (voted && !mine)}
              style={unavailable || (voted && !mine) ? { opacity: 0.55, cursor: "default" } : undefined}
              onClick={() => cast(option)}
            >
              <b>{label(about.name)}</b>
              <span className="num" style={mine ? { color: "var(--cyan-text)" } : undefined}>
                {counts[option]}
              </span>
              <span className={mine ? "small" : "small faint"} style={mine ? { color: "var(--cyan-text)" } : undefined}>
                {unavailable ? "No pause time left" : mine ? "Your vote" : voted ? " " : "Tap to vote"}
              </span>
            </button>
          );
        })}
      </div>
      <p className="small muted">
        {vote.votedUserIds.length} of {voters.length} players voted. The option with the most votes wins when the vote closes; a tie waits if
        there&apos;s pause time left.
      </p>
    </StageCard>
  );
}

/** Waiting up to a minute for someone to come back (Paused board, ADR-026). */
export function PausedScreen() {
  const { snapshot, clockOffsetMs } = useRoom();
  const info = usePlayerInfo();
  const now = useNow(250);
  const who = info(snapshot.waitingForUserId ?? "");
  const leftMs = now === 0 ? 60_000 : msUntil(snapshot.phaseDeadline, now, clockOffsetMs);
  const budget = snapshot.pauseSecondsLeft;

  return (
    <StageCard center vote>
      <span className="tag tag--yellow">
        <Icon name="pause" size={12} />
        Paused
      </span>
      <p className="num" style={{ fontSize: 88, lineHeight: 1 }} role="timer" aria-label={`${Math.ceil(leftMs / 1000)} seconds`}>
        {formatClock(leftMs / 1000)}
      </p>
      <div className="row" style={{ justifyContent: "center", gap: 12 }}>
        <Av look={who.look} size={44} face="away" away />
        <h1 className="title" style={{ fontSize: 26 }}>
          Waiting for {who.name}
        </h1>
      </div>
      {budget !== undefined && (
        <div style={{ width: "min(420px, 100%)", display: "flex", flexDirection: "column", gap: 8 }}>
          <div className="track">
            <div className="fill" style={{ width: `${Math.min(100, (budget / PAUSE_BUDGET_S) * 100)}%`, background: "var(--yellow)" }} />
          </div>
          <p className="small muted">Pause time left: {formatClock(budget)} of 3:00</p>
        </div>
      )}
      <p className="muted" style={{ maxWidth: 460 }}>
        If {who.name} comes back, we carry on with a short countdown. If not, we&apos;ll vote again.
      </p>
    </StageCard>
  );
}
