"use client";

import Link from "next/link";
import { quizTitle } from "@/components/history/HistoryList";
import { DeletedAvatar, isDeleted, usePastLook } from "@/components/history/QuizAvatars";
import { useApi } from "@/components/providers/ApiProvider";
import { Av } from "@/components/ui/Character";
import { Icon } from "@/components/ui/Icon";
import { MessageCard, PageSkeleton } from "@/components/ui/States";
import type { Me } from "@/lib/api-types";
import { formatPoints, ordinal, relativeTime } from "@/lib/format";
import { useGatewayData } from "@/lib/useGatewayData";
import { useNow } from "@/lib/useNow";

const END_TEXT = {
  COMPLETED: null,
  VOTED_TO_END: "Ended early by a vote.",
  NOT_ENOUGH_PLAYERS: "Ended early: not enough players were left.",
};

/** One finished quiz and its final scoreboard (HistoryDetail board). */
export function QuizDetail({ quizId, me }: { quizId: string; me: Me }) {
  const api = useApi();
  const now = useNow(60_000);
  const lookOf = usePastLook(me.userId);
  const { data: quiz, error, loading, reload } = useGatewayData(`quiz:${quizId}`, () => api.getQuiz(quizId, { quiet: true }));

  const back = (
    <Link className="row small muted" href="/history" style={{ textDecoration: "none", gap: 6 }}>
      <Icon name="arrowLeft" size={16} />
      History
    </Link>
  );

  if (loading) return <PageSkeleton label="Loading the quiz…" />;
  if (error || !quiz) {
    const missing = error?.status === 404 || error?.status === 403;
    return (
      <MessageCard
        icon={missing ? "search" : "alert"}
        tone={missing ? "yellow" : "red"}
        role="alert"
        title={missing ? "Quiz not found" : "Couldn't load this quiz"}
        actions={
          missing ? (
            <Link className="btn btn--primary" href="/history">
              Back to history
            </Link>
          ) : (
            <button type="button" className="btn btn--primary" onClick={reload}>
              Try again
            </button>
          )
        }
      >
        <p className="muted">{missing ? "You can only open quizzes you played in." : error?.message}</p>
      </MessageCard>
    );
  }

  const mine = quiz.finalScoreboard.find((s) => s.userId === me.userId);
  const facts = [
    now > 0 ? relativeTime(quiz.finishedAt, now) : null,
    `${quiz.roundsPlayed}${quiz.roundsPlayed < quiz.totalRounds ? ` of ${quiz.totalRounds}` : ""} questions`,
    quiz.secondsPerQuestion ? `${quiz.secondsPerQuestion} seconds each` : null,
  ].filter(Boolean);
  const ended = quiz.endReason ? END_TEXT[quiz.endReason] : null;

  return (
    <div className="hd">
      <header>
        {back}
        <h1 className="title">{quizTitle(quiz, me)}</h1>
        <p className="muted">
          {facts.join(" · ")}
          {ended && ` · ${ended}`}
        </p>
      </header>
      <div className="hd__grid">
        {mine && (
          <section className="card res" aria-labelledby="h-res">
            <h2 id="h-res" className="label">
              Your result
            </h2>
            <Av look={lookOf(me.userId)} size={72} />
            <p className="title" style={{ fontSize: 26 }}>
              You came {ordinal(mine.rank)}
            </p>
            <p className="num" style={{ fontSize: 30, color: "var(--orange-text)" }}>
              {formatPoints(mine.totalPoints)}
            </p>
            <p className="small muted">
              {mine.correctAnswers} of {quiz.roundsPlayed} correct
            </p>
          </section>
        )}
        <section className="card st" aria-labelledby="h-fs" style={mine ? undefined : { gridColumn: "1 / -1" }}>
          <h2 id="h-fs" className="label">
            Final scoreboard
          </h2>
          <ol style={{ display: "flex", flexDirection: "column", gap: 2 }}>
            {quiz.finalScoreboard.map((entry) => {
              const you = entry.userId === me.userId;
              const deleted = isDeleted(entry);
              return (
                <li key={entry.userId} className={you ? "you" : ""}>
                  <span className="num muted">{entry.rank}</span>
                  {deleted ? <DeletedAvatar /> : <Av look={lookOf(entry.userId)} size={32} />}
                  <span>
                    <b
                      className="h2"
                      style={deleted ? { fontSize: 16, color: "var(--text-faint)", fontStyle: "italic" } : { fontSize: 16 }}
                    >
                      {entry.displayName}
                    </b>
                    {you && (
                      <>
                        {" "}
                        <span className="tag tag--pink">You</span>
                      </>
                    )}
                    <span className="small faint" style={{ display: "block" }}>
                      {entry.correctAnswers} of {quiz.roundsPlayed} correct
                    </span>
                  </span>
                  <span className="num" style={{ fontSize: 17 }}>
                    {formatPoints(entry.totalPoints)}
                  </span>
                </li>
              );
            })}
          </ol>
          {quiz.finalScoreboard.some(isDeleted) && (
            <p className="small faint">Players who deleted their account show as &quot;Deleted player&quot;.</p>
          )}
        </section>
      </div>
    </div>
  );
}
