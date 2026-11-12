"use client";

import Link from "next/link";
import type { CSSProperties, ReactNode } from "react";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { Standings } from "@/components/room/Scoreboard";
import { Av, Mini } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import { MessageCard } from "@/components/ui/States";
import type { ScoreEntry } from "@/lib/api-types";
import { formatPoints } from "@/lib/format";

/** The confetti burst from the middle of the podium (Final results board): the same 56 pieces every time. */
const CONFETTI = (() => {
  let seed = 7;
  const rand = () => {
    seed = (seed * 16807) % 2147483647;
    return seed / 2147483647;
  };
  const colours = ["var(--player-1-fill)", "var(--player-2-fill)", "var(--player-3-fill)", "var(--player-5-fill)", "var(--player-6-fill)", "var(--player-8-fill)", "var(--orange)"];
  const shapes: CSSProperties[] = [
    { width: 8, height: 8, borderRadius: "50%" },
    { width: 6, height: 16 },
    { width: 10, height: 10 },
  ];
  return Array.from({ length: 56 }, (_, i) => ({
    "--x": `${Math.round(rand() * 460 - 230)}px`,
    "--y": `${Math.round(-90 - rand() * 210)}px`,
    "--r": `${Math.round(rand() * 1660 - 830)}deg`,
    "--d": `${(rand() * 0.25).toFixed(2)}s`,
    background: colours[Math.floor(rand() * colours.length)],
    ...shapes[i % 3],
  })) as CSSProperties[];
})();

function Confetti() {
  return (
    <div className="confetti" aria-hidden="true">
      {CONFETTI.map((style, i) => (
        <i key={i} style={style} />
      ))}
    </div>
  );
}

/** "Maya wins!", "You win!" or "It's a tie!", and the line under it. */
function useHeadline(board: ScoreEntry[]): { title: string; line: string } {
  const { meId } = useRoom();
  const info = usePlayerInfo();
  const winners = board.filter((e) => e.rank === 1);
  if (winners.length === 0) return { title: "Final results", line: "" };
  if (winners.length > 1) {
    return { title: "It's a tie!", line: `${winners.map((w) => (w.userId === meId ? "You" : info(w.userId).name)).join(" and ")} know this squad best.` };
  }
  if (winners[0].userId === meId) return { title: "You win!", line: "You know this squad best." };
  const name = info(winners[0].userId).name;
  return { title: `${name} wins!`, line: `${name} knows this squad best.` };
}

/**
 * Final results (FF-508, Final results board): the standings, the podium with confetti,
 * and the chat, side by side at 1:2:1. Back to lobby keeps the room for another quiz.
 */
export function PodiumScreen({ chat, onBackToLobby }: { chat: ReactNode; onBackToLobby: () => void }) {
  const { snapshot, meId } = useRoom();
  const info = usePlayerInfo();
  const board = snapshot.scoreboard;
  const { title, line } = useHeadline(board);
  const podium: { entry: ScoreEntry | undefined; place: 1 | 2 | 3 }[] = [
    { entry: board[1], place: 2 },
    { entry: board[0], place: 1 },
    { entry: board[2], place: 3 },
  ];
  const heights = { 1: 170, 2: 120, 3: 86 };
  const fastest = snapshot.fastestAnswer;

  return (
    <div className="podroom">
      <section className="card st nf-phase" aria-labelledby="h-st">
        <div style={{ display: "flex", flexDirection: "column", gap: 12 }}>
          <h2 id="h-st" className="label">
            Final standings
          </h2>
          <Standings entries={board} rounds={snapshot.roundNumber} />
        </div>
        <div className="st__btns">
          <button type="button" className="btn btn--primary btn--block" onClick={onBackToLobby}>
            Back to lobby
          </button>
          <Link className="btn btn--line btn--block" href="/">
            Back to home
          </Link>
        </div>
      </section>
      <section className="card stage nf-phase" aria-labelledby="h-win">
        <div>
          <p className="label">Final results</p>
          <h1 id="h-win" className="title">
            {title}
          </h1>
          <p className="muted">{line}</p>
        </div>
        <div className="steps3">
          {podium.map(({ entry, place }) =>
            entry ? (
              <div key={entry.userId} className="col">
                <Av look={info(entry.userId).look} size={place === 1 ? 80 : 60} face={place === 1 ? "wow" : undefined} />
                <span className="nm">{info(entry.userId).name}</span>
                <span className="num small" style={place === 1 ? { color: "var(--orange-text)" } : { color: "var(--text-muted)" }}>
                  {formatPoints(entry.totalPoints)}
                </span>
                <div
                  className="pblock"
                  style={{ height: heights[place], ...(place === 1 ? { background: "var(--orange)", color: "var(--on-neon)" } : {}) }}
                >
                  <span className="sr">Place </span>
                  {place}
                </div>
              </div>
            ) : (
              <div key={place} className="col" />
            ),
          )}
        </div>
        {fastest ? (
          <p className="row small muted" style={{ gap: 8, justifyContent: "center" }}>
            <Icon name="clock" size={16} />
            Fastest answer: {fastest.userId === meId ? "you" : info(fastest.userId).name}, {fastest.seconds.toFixed(1)} s
          </p>
        ) : (
          <span />
        )}
        <Confetti />
      </section>
      {chat}
    </div>
  );
}

/** The quiz ended before the last question (Room closed board, "ended early"). */
export function EndedEarlyScreen({ onBackToLobby }: { onBackToLobby: () => void }) {
  const { snapshot, meId } = useRoom();
  const info = usePlayerInfo();
  const why = snapshot.endReason === "NOT_ENOUGH_PLAYERS" ? "Too few players were left to carry on." : "Most of you voted to end it.";
  const rounds = snapshot.roundNumber;
  return (
    <MessageCard
      icon="pause"
      tone="yellow"
      title="The quiz ended early"
      wide
      actions={
        <>
          <button type="button" className="btn btn--primary" onClick={onBackToLobby}>
            Back to lobby
          </button>
          <Link className="btn btn--ghost" href="/">
            Back to home
          </Link>
        </>
      }
    >
      <p className="muted">
        {why} Here&apos;s how it stood after {rounds} {rounds === 1 ? "question" : "questions"}.
      </p>
      <ol
        aria-label="Standings so far"
        style={{
          width: "100%",
          textAlign: "left",
          display: "flex",
          flexDirection: "column",
          gap: 2,
          padding: "8px 0",
          borderTop: "1px solid var(--line)",
          borderBottom: "1px solid var(--line)",
        }}
      >
        {snapshot.scoreboard.map((entry) => (
          <li key={entry.userId} className="row" style={{ height: 40, justifyContent: "space-between" }}>
            <span className="row" style={{ gap: 10 }}>
              <span className="num muted" style={{ width: 14 }}>
                {entry.rank}
              </span>
              <Mini look={info(entry.userId).look} />
              <b className="h2" style={{ fontSize: 15 }}>
                {info(entry.userId).name}
              </b>
              {entry.userId === meId && <span className="tag tag--pink">You</span>}
            </span>
            <span className="num">{formatPoints(entry.totalPoints)}</span>
          </li>
        ))}
      </ol>
    </MessageCard>
  );
}
