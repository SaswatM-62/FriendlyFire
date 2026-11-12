"use client";

import { useEffect, useRef, useState, type CSSProperties } from "react";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { Av } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import type { ScoreEntry } from "@/lib/api-types";
import { formatPoints } from "@/lib/format";

/** Counts a number up from `from` to `to` (skipped for prefers-reduced-motion). */
export function CountUp({ from, to, ms = 900 }: { from: number; to: number; ms?: number }) {
  const [value, setValue] = useState(from);
  const frame = useRef(0);
  useEffect(() => {
    const reduce = window.matchMedia?.("(prefers-reduced-motion: reduce)")?.matches ?? false;
    const start = performance.now();
    const step = (t: number) => {
      const progress = reduce ? 1 : Math.min(1, (t - start) / ms);
      const eased = 1 - (1 - progress) ** 3;
      setValue(Math.round(from + (to - from) * eased));
      if (progress < 1) frame.current = requestAnimationFrame(step);
    };
    frame.current = requestAnimationFrame(step);
    return () => cancelAnimationFrame(frame.current);
  }, [from, to, ms]);
  return <>{formatPoints(value)}</>;
}

/**
 * Where each player stood before this round: ranks from the totals minus this round's
 * points, with equal points sharing a rank, as the server ranks them.
 */
export function previousRanks(board: ScoreEntry[]): Map<string, number> {
  const before = board.map((e) => ({ userId: e.userId, points: e.totalPoints - e.pointsLastRound }));
  const sorted = [...before].sort((a, b) => b.points - a.points);
  return new Map(before.map((e) => [e.userId, sorted.findIndex((s) => s.points === e.points) + 1]));
}

/** The scoreboard during the reveal (Reveal board): this round's points, the total counting up, and who moved. */
export function Scoreboard({ entries, className = "", style }: { entries: ScoreEntry[]; className?: string; style?: CSSProperties }) {
  const { meId } = useRoom();
  const info = usePlayerInfo();
  const before = previousRanks(entries);

  return (
    <section className={`card sb ${className}`.trim()} aria-labelledby="h-sb" style={style}>
      <h2 id="h-sb" className="label">
        Scoreboard
      </h2>
      <ol style={{ display: "flex", flexDirection: "column", gap: 4 }}>
        {entries.map((entry) => {
          const you = entry.userId === meId;
          const { name, look } = info(entry.userId);
          const was = before.get(entry.userId) ?? entry.rank;
          const moved = was === entry.rank ? "same" : was > entry.rank ? "up" : "down";
          return (
            <li key={entry.userId} className={you ? "you" : ""}>
              <span className="rank">{entry.rank}</span>
              <Av look={look} size={30} />
              <span className="nm">
                {name}
                {you && <span className="tag tag--pink">You</span>}
              </span>
              <span className="d" style={{ color: entry.pointsLastRound > 0 ? "var(--mint-text)" : "var(--text-faint)" }}>
                +{formatPoints(entry.pointsLastRound)}
              </span>
              <span className="tot">
                <CountUp from={entry.totalPoints - entry.pointsLastRound} to={entry.totalPoints} />
              </span>
              {moved === "same" ? (
                <span />
              ) : (
                <span style={{ color: moved === "up" ? "var(--mint-text)" : "var(--red-text)" }}>
                  <Icon name={moved === "up" ? "arrowUp" : "arrowDown"} size={16} label={moved === "up" ? "Up a place" : "Down a place"} />
                </span>
              )}
            </li>
          );
        })}
      </ol>
    </section>
  );
}

/** Final standings (Final results and Room closed boards): rank, character, name, correct answers, points. */
export function Standings({ entries, rounds }: { entries: ScoreEntry[]; rounds: number }) {
  const { meId } = useRoom();
  const info = usePlayerInfo();
  return (
    <ol style={{ display: "flex", flexDirection: "column", gap: 2 }}>
      {entries.map((entry) => {
        const you = entry.userId === meId;
        const { name, look } = info(entry.userId);
        return (
          <li key={entry.userId} className={you ? "you" : ""}>
            <span className="num muted">{entry.rank}</span>
            <Av look={look} size={32} />
            <span className="nm">
              <b>
                {name}
                {you && <span className="tag tag--pink">You</span>}
              </b>
              {entry.correctAnswers !== undefined && (
                <span className="small faint">
                  {entry.correctAnswers} of {rounds} correct
                </span>
              )}
            </span>
            <span className="num" style={{ fontSize: 17 }}>
              {formatPoints(entry.totalPoints)}
            </span>
          </li>
        );
      })}
    </ol>
  );
}
