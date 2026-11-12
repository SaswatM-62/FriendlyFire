"use client";

import { useEffect, useRef } from "react";
import { useSound } from "@/components/providers/SoundProvider";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { usePhaseLength } from "@/components/room/usePhaseLength";
import { StageCard } from "@/components/room/StageCard";
import { Av, Mini } from "@/components/ui/Character";
import { msUntil } from "@/lib/clock";
import { activePlayers, yourStanding } from "@/lib/players";
import { useNow } from "@/lib/useNow";

/** The whole seconds left in the STARTING phase, with a tick sound on each. */
function useCountdown() {
  const { snapshot, clockOffsetMs } = useRoom();
  const { play } = useSound();
  const now = useNow(100);
  const leftMs = now === 0 ? 3_000 : msUntil(snapshot.phaseDeadline, now, clockOffsetMs);
  const number = Math.max(1, Math.ceil(leftMs / 1000));

  const lastTick = useRef<number | null>(null);
  useEffect(() => {
    if (now === 0 || lastTick.current === number) return;
    lastTick.current = number;
    play("tick");
  }, [number, now, play]);

  return { leftMs, number, started: now !== 0 };
}

const RING_R = 100;
const RING_LENGTH = 2 * Math.PI * RING_R;

/** The 220 px countdown ring: it empties as the seconds run out (Countdown and Get ready boards). */
function Ring({ fraction, colour, number, label }: { fraction: number; colour: string; number: number; label: string }) {
  return (
    <div className="cring" role="timer" aria-label={label}>
      <svg viewBox="0 0 220 220" width="220" height="220" aria-hidden="true">
        <circle cx="110" cy="110" r={RING_R} fill="none" stroke="var(--surface-2)" strokeWidth="10" />
        <circle
          cx="110"
          cy="110"
          r={RING_R}
          fill="none"
          stroke={colour}
          strokeWidth="10"
          strokeLinecap="round"
          strokeDasharray={RING_LENGTH}
          strokeDashoffset={RING_LENGTH * (1 - fraction)}
          style={{ transition: "stroke-dashoffset 100ms linear" }}
        />
      </svg>
      <span key={number} className="num countnum" aria-live="assertive">
        {number}
      </span>
    </div>
  );
}

/** 3-2-1 before the first question (Countdown board). */
export function Countdown() {
  const { snapshot } = useRoom();
  const info = usePlayerInfo();
  const { leftMs, number, started } = useCountdown();
  const totalMs = usePhaseLength(leftMs, started);
  const fraction = totalMs ? Math.min(1, Math.max(0, leftMs / totalMs)) : 1;
  const players = activePlayers(snapshot.players);

  return (
    <StageCard center>
      <h1 className="label">Get ready</h1>
      <Ring fraction={fraction} colour="var(--orange)" number={number} label={`Starting in ${number}`} />
      <p className="row muted" style={{ gap: 8, justifyContent: "center" }}>
        <span style={{ display: "flex", gap: 4 }}>
          {players.map((p) => (
            <Mini key={p.userId} look={info(p.userId).look} />
          ))}
        </span>
        {players.length} players · good luck
      </p>
    </StageCard>
  );
}

/**
 * Get ready (Get ready board): the short countdown before every later question, and after
 * a pause. The ring empties, then the question appears. Shows where you stand.
 */
export function GetReady() {
  const { snapshot, meId, names } = useRoom();
  const info = usePlayerInfo();
  const { leftMs, number, started } = useCountdown();
  const totalMs = usePhaseLength(leftMs, started);
  const fraction = totalMs ? Math.min(1, Math.max(0, leftMs / totalMs)) : 1;
  const standing = yourStanding(snapshot.scoreboard, meId, names);

  return (
    <StageCard center>
      <div>
        <p className="label">Get ready</p>
        <h1 className="title">
          Question {snapshot.roundNumber + 1} of {snapshot.settings.questionCount}
        </h1>
      </div>
      <Ring fraction={fraction} colour="var(--cyan)" number={number} label={`Next question in ${number}`} />
      <p className="muted" style={{ maxWidth: 420 }}>
        Eyes up. The question appears when the ring empties, and the timer starts a moment later.
      </p>
      {standing && (
        <div className="card standing">
          <Av look={info(meId).look} size={40} />
          <div style={{ textAlign: "left" }}>
            <p className="h2" style={{ fontSize: 17 }}>
              {standing.title}
            </p>
            <p className="small muted">{standing.line}</p>
          </div>
        </div>
      )}
    </StageCard>
  );
}
