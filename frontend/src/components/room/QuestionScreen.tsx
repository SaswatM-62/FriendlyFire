"use client";

import { useState } from "react";
import { useSound } from "@/components/providers/SoundProvider";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { StageCard } from "@/components/room/StageCard";
import { Av } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import { msUntil } from "@/lib/clock";
import { activePlayers } from "@/lib/players";
import { useNow } from "@/lib/useNow";

/** The last few seconds turn the timer red and sweep the scope (Last 5 seconds / time's up board). */
export const HURRY_MS = 5_000;

export function TimerBar({ leftMs, limitMs, timeUp }: { leftMs: number; limitMs: number; timeUp?: boolean }) {
  const seconds = Math.ceil(leftMs / 1000);
  const hurry = leftMs <= HURRY_MS;
  const fraction = limitMs > 0 ? Math.min(1, leftMs / limitMs) : 0;
  const colour = timeUp ? "var(--text-faint)" : hurry ? "var(--red-text)" : undefined;
  return (
    <div className="timer" role="timer" aria-label={timeUp ? "Time's up" : `${seconds} seconds left`}>
      <div className="track">
        {!timeUp && <div className="fill" style={{ width: `${fraction * 100}%`, background: hurry ? "var(--red)" : "var(--cyan)" }} />}
      </div>
      <span className="num" style={colour ? { color: colour } : undefined} aria-hidden="true">
        {timeUp ? 0 : seconds}
      </span>
    </div>
  );
}

interface Props {
  /** How many of our answers the server has rejected (time's up, already answered). */
  rejections: number;
}

/**
 * A question (FF-506): the players as big answer buttons, a timer from the server's
 * deadline (corrected for this browser's clock), lock-in that can't be changed, and
 * who has answered so far. Mounted fresh for each question.
 */
export function QuestionScreen({ rejections }: Props) {
  const { snapshot, meId, clockOffsetMs, send } = useRoom();
  const info = usePlayerInfo();
  const { play } = useSound();
  const now = useNow(250);
  const question = snapshot.currentQuestion!;
  const limitMs = snapshot.settings.secondsPerQuestion * 1000;
  const leftMs = now === 0 ? limitMs : msUntil(snapshot.phaseDeadline, now, clockOffsetMs);
  const [locked, setLocked] = useState<{ optionId: string; rejections: number } | null>(null);

  // A rejection after we locked in unlocks the tiles again (Architecture 3.1, Answering).
  const myLock = locked && rejections === locked.rejections ? locked.optionId : null;
  const me = snapshot.players.find((p) => p.userId === meId);
  const confirmed = me?.answeredCurrentQuestion ?? false;
  const answered = myLock !== null || confirmed;
  const canAnswer = me?.status !== "SITTING_OUT";
  const timeUp = now !== 0 && leftMs === 0;
  const hurry = leftMs <= HURRY_MS && !timeUp;

  const players = activePlayers(snapshot.players);
  const lockedCount = players.filter((p) => p.answeredCurrentQuestion).length;

  const choose = (optionId: string) => {
    if (answered || timeUp) return;
    setLocked({ optionId, rejections });
    play("lock");
    send("answer", { questionId: question.questionId, optionId });
  };

  const missed = timeUp && !answered;

  return (
    <StageCard>
      <header style={{ display: "flex", flexDirection: "column", gap: 12, alignItems: "flex-start" }}>
        <span className="tag tag--violet">Pick the player</span>
        <h1 className="q">{question.text}</h1>
      </header>
      <TimerBar leftMs={leftMs} limitMs={limitMs} timeUp={timeUp} />
      {missed && (
        <div className="card timesup" role="status">
          <span className="mico" style={{ color: "var(--red-text)" }}>
            <Icon name="clock" size={26} />
          </span>
          <div>
            <p className="h2" style={{ color: "var(--red-text)" }}>
              Time&apos;s up
            </p>
            <p className="small muted">You didn&apos;t lock in, so no points this time.</p>
          </div>
        </div>
      )}
      <div className="tiles" role="group" aria-label="Answers">
        {question.options.map((option) => {
          const look = info(option.optionId).look;
          const mine = myLock === option.optionId;
          const dim = (answered && !mine) || timeUp;
          return (
            <button
              key={option.optionId}
              type="button"
              className={`tile${mine ? " tile--locked" : dim ? " tile--dim" : ""}`}
              aria-pressed={mine}
              disabled={!canAnswer || (answered && !mine) || timeUp}
              onClick={() => choose(option.optionId)}
            >
              <Av look={look} size={48} />
              <span className="tile__nm">
                <b>{option.label}</b>
                {mine && <small style={{ color: "var(--yellow-text)" }}>Locked in</small>}
              </span>
              <span />
            </button>
          );
        })}
      </div>
      {!missed && (
        <p className="lockstrip" role="status">
          <span className="faces" aria-hidden="true">
            {players.map((p) => (
              <Av
                key={p.userId}
                look={info(p.userId).look}
                size={30}
                away={!p.answeredCurrentQuestion}
                face={p.answeredCurrentQuestion ? undefined : p.status === "DISCONNECTED" ? "away" : "thinking"}
              />
            ))}
          </span>
          <span className="h2" style={{ fontSize: 17, color: hurry && !answered ? "var(--red-text)" : undefined }}>
            {timeUp ? "Time's up" : `${hurry && !answered ? "Hurry: " : ""}${lockedCount} of ${players.length} locked in`}
          </span>
        </p>
      )}
      {me?.status === "SITTING_OUT" && (
        <p className="small muted" style={{ textAlign: "center" }}>
          You&apos;re sitting out after a disconnect, so you can watch but not answer.
        </p>
      )}
    </StageCard>
  );
}
