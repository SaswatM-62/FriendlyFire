"use client";

import type { CSSProperties } from "react";
import { usePlayerInfo, useRoom } from "@/components/room/RoomContext";
import { Scoreboard } from "@/components/room/Scoreboard";
import { usePhaseLength } from "@/components/room/usePhaseLength";
import { Scope } from "@/components/ui/Brand";
import { Av, Mini } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import type { PlayerResult } from "@/lib/api-types";
import { msUntil } from "@/lib/clock";
import { formatPoints } from "@/lib/format";
import { useNow } from "@/lib/useNow";

/** The reveal comes in steps (data-motion="reveal"): answers, your result, the scores, then the countdown. */
const step = (delay: string) => ({ "--d": delay }) as CSSProperties;

/** At most three pickers show on a tile, then "+2" (the boards). */
const MAX_PICKS = 3;

/**
 * The fastest correct answer earned the most points (points fall with time taken), so
 * it's the correct result with the highest points.
 */
export function fastestCorrect(results: PlayerResult[]): string | null {
  const correct = results.filter((r) => r.correct);
  if (correct.length === 0) return null;
  return correct.reduce((best, r) => (r.points > best.points ? r : best)).userId;
}

function YourResult({ result, correctName, explanation }: { result: PlayerResult | undefined; correctName: string; explanation: string }) {
  const { snapshot, meId } = useRoom();
  const info = usePlayerInfo();
  const fastest = fastestCorrect(snapshot.currentQuestion?.reveal?.results ?? []) === meId;
  const look = info(meId).look;
  const picked = result?.chosenOptionId ? snapshot.currentQuestion?.options.find((o) => o.optionId === result.chosenOptionId)?.label : null;

  let tone = "mint";
  let title = "Nice!";
  let line = "You got it right.";
  if (!result?.chosenOptionId) {
    tone = "orange";
    title = "Too slow";
    line = "You didn't lock in an answer this time.";
  } else if (!result.correct) {
    tone = "red";
    title = "So close!";
    line = `It was ${correctName}.${picked ? ` You picked ${picked}.` : ""}`;
  } else if (fastest) {
    title = "Called it!";
    line = "You were the fastest to get it right.";
  }

  return (
    <div className="card result nf-step" style={step(".9s")} role="status">
      <Av look={look} size={52} face={result?.correct ? "wow" : undefined} />
      <div className="result__head">
        <p className="title" style={{ fontSize: 22, lineHeight: "28px", color: `var(--${tone}-text)` }}>
          {title}
        </p>
        <p className="small muted">{line}</p>
      </div>
      <div className="result__rc">
        <p className="label">The receipts</p>
        <p className="small">{explanation}</p>
      </div>
      <span className={result?.correct ? "num" : "num faint"} style={result?.correct ? { color: "var(--orange-text)" } : undefined}>
        +{formatPoints(result?.points ?? 0)}
      </span>
    </div>
  );
}

/** The reveal (FF-507, Reveal board): the answer and everyone's picks, your result with the receipts, the scoreboard. */
export function RevealScreen() {
  const { snapshot, meId, clockOffsetMs } = useRoom();
  const info = usePlayerInfo();
  const now = useNow(250);
  const question = snapshot.currentQuestion!;
  const reveal = question.reveal!;
  const pickers = (optionId: string) => reveal.results.filter((r) => r.chosenOptionId === optionId);
  const mine = reveal.results.find((r) => r.userId === meId);
  const correctName = question.options.find((o) => o.optionId === reveal.correctOptionId)?.label ?? info(reveal.correctOptionId).name;
  const leftMs = now === 0 ? 10_000 : msUntil(snapshot.phaseDeadline, now, clockOffsetMs);
  const left = Math.max(0, Math.ceil(leftMs / 1000));
  const totalMs = usePhaseLength(leftMs, now !== 0);
  const last = snapshot.roundNumber >= snapshot.settings.questionCount;
  const playing = snapshot.players.some((p) => p.userId === meId && p.status !== "SITTING_OUT");
  const what = last ? "Final results in" : "Next question in";

  return (
    <>
      <div className="card qcard scoped nf-phase">
        <Scope />
        <h1 className="h2" style={{ fontSize: 22, lineHeight: "30px" }}>
          {question.text}
        </h1>
        <div className="tiles nf-step" style={step("0s")}>
          {question.options.map((option) => {
            const correct = option.optionId === reveal.correctOptionId;
            const wrongPick = !correct && mine?.chosenOptionId === option.optionId;
            const look = info(option.optionId).look;
            const picks = pickers(option.optionId);
            const names = picks.map((p) => (p.userId === meId ? "you" : info(p.userId).name));
            return (
              <div key={option.optionId} className={`tile ${correct ? "tile--ok" : wrongPick ? "tile--bad" : "tile--dim"}`}>
                <Av look={look} size={48} face={correct ? "wow" : undefined} />
                <span className="tile__nm">
                  <b>{option.label}</b>
                  {correct && (
                    <small className="stt" style={{ color: "var(--mint-text)" }}>
                      <Icon name="check" size={14} />
                      Correct
                    </small>
                  )}
                  {wrongPick && (
                    <small className="stt" style={{ color: "var(--red-text)" }}>
                      <Icon name="close" size={14} />
                      Wrong pick
                    </small>
                  )}
                </span>
                <span className="picks" role="img" aria-label={`Picked by ${names.length > 0 ? names.join(", ") : "nobody"}`}>
                  {picks.slice(0, MAX_PICKS).map((p) => (
                    <Mini key={p.userId} look={info(p.userId).look} />
                  ))}
                  {picks.length > MAX_PICKS && <span className="more">+{picks.length - MAX_PICKS}</span>}
                </span>
              </div>
            );
          })}
        </div>
        <div className="next nf-step" role="timer" aria-label={`${what} ${left} seconds`} style={step("2.5s")}>
          <span className="small muted" style={{ whiteSpace: "nowrap" }}>
            {what}{" "}
            <span className="num" style={{ color: "var(--text)" }}>
              {left}
            </span>
            s
          </span>
          <div className="track">
            <div className="fill nf-drain" style={{ width: `${totalMs ? Math.min(100, (leftMs / totalMs) * 100) : 100}%`, background: "var(--violet)" }} />
          </div>
        </div>
      </div>
      {playing ? (
        <YourResult result={mine} correctName={correctName} explanation={reveal.explanation} />
      ) : (
        <div className="card result nf-step" style={step(".9s")}>
          <div className="result__rc" style={{ borderLeft: 0, paddingLeft: 0 }}>
            <p className="label">The receipts</p>
            <p className="small">{reveal.explanation}</p>
          </div>
        </div>
      )}
      <Scoreboard entries={snapshot.scoreboard} className="nf-step" style={step("1.7s")} />
    </>
  );
}
