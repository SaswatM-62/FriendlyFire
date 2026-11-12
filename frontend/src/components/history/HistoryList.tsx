"use client";

import Link from "next/link";
import { useState } from "react";
import { useCreateRoom } from "@/components/home/useCreateRoom";
import { placeTone, QuizAvatars } from "@/components/history/QuizAvatars";
import { useApi } from "@/components/providers/ApiProvider";
import { Icon } from "@/components/ui/Icon";
import { InlineError, MessageCard, PageSkeleton } from "@/components/ui/States";
import { ApiError } from "@/lib/api";
import type { Me, QuizSummary } from "@/lib/api-types";
import { ordinal, relativeTime, roomTitle } from "@/lib/format";
import { useGatewayData } from "@/lib/useGatewayData";
import { useNow } from "@/lib/useNow";

export const HISTORY_PAGE_SIZE = 10;

/** "Your room" or "Maya's room". */
export function quizTitle(quiz: QuizSummary, me: Me): string {
  return quiz.hostDisplayName === me.displayName ? "Your room" : roomTitle(quiz.hostDisplayName);
}

function HistoryRow({ quiz, me, now }: { quiz: QuizSummary; me: Me; now: number }) {
  const mine = quiz.finalScoreboard.find((s) => s.userId === me.userId);
  const winner = quiz.finalScoreboard.find((s) => s.rank === 1);
  const rank = mine?.rank ?? 0;
  const early = quiz.endReason !== undefined && quiz.endReason !== "COMPLETED";
  return (
    <li>
      <Link href={`/history/${quiz.quizId}`}>
        <span className="place" style={{ color: `var(--${placeTone(rank)}-text)` }}>
          {rank ? ordinal(rank) : "–"}
        </span>
        <span>
          <b className="h2" style={{ fontSize: 17, display: "block" }}>
            {quizTitle(quiz, me)}
          </b>
          <span className="small faint">
            {now > 0 ? relativeTime(quiz.finishedAt, now) : " "} · {quiz.roundsPlayed} questions{early && " · ended early"}
          </span>
        </span>
        <QuizAvatars quiz={quiz} meId={me.userId} />
        <span className="small muted">
          {winner && (
            <>
              Winner <strong style={{ color: "var(--text)" }}>{winner.userId === me.userId ? "You" : winner.displayName}</strong>
            </>
          )}
        </span>
        <span className="faint">
          <Icon name="chevronRight" size={16} />
        </span>
      </Link>
    </li>
  );
}

function EmptyHistory() {
  const { create, busy } = useCreateRoom();
  return (
    <section className="card" style={{ alignItems: "center", textAlign: "center", padding: 48 }}>
      <span className="mico" style={{ color: "var(--text-muted)" }}>
        <Icon name="clock" size={26} />
      </span>
      <h2 className="title" style={{ fontSize: 24 }}>
        No quizzes yet
      </h2>
      <p className="muted">Create a room and invite your squad.</p>
      <button type="button" className="btn btn--primary" onClick={create} disabled={busy}>
        {busy ? "Creating…" : "Create room"}
      </button>
    </section>
  );
}

/** Your past quizzes, newest first, with Load more (FF-510). */
export function HistoryList({ me }: { me: Me }) {
  const api = useApi();
  const now = useNow(60_000);
  const first = useGatewayData(`history:${me.userId}`, () => api.getHistory({ pageSize: HISTORY_PAGE_SIZE }, { quiet: true }));
  const [more, setMore] = useState<{ quizzes: QuizSummary[]; nextPageToken?: string; forKey: QuizSummary[] | null }>({
    quizzes: [],
    forKey: null,
  });
  const [loadingMore, setLoadingMore] = useState(false);
  const [moreError, setMoreError] = useState<string | null>(null);

  if (first.loading) return <PageSkeleton label="Loading your quizzes…" />;
  if (first.error) {
    return (
      <MessageCard
        icon="alert"
        tone="red"
        role="alert"
        title="Couldn't load your history"
        actions={
          <button type="button" className="btn btn--primary" onClick={first.reload}>
            Try again
          </button>
        }
      >
        <p className="muted">{first.error.message}</p>
      </MessageCard>
    );
  }
  if (!first.data) return null;

  // Pages loaded with "Load more" belong to this first page.
  const extra = more.forKey === first.data.quizzes ? more : { quizzes: [], nextPageToken: undefined };
  const quizzes = [...first.data.quizzes, ...extra.quizzes];
  const nextToken = extra.quizzes.length > 0 ? extra.nextPageToken : first.data.nextPageToken;

  const loadMore = async () => {
    if (!nextToken || !first.data) return;
    const owner = first.data.quizzes;
    setLoadingMore(true);
    setMoreError(null);
    try {
      const page = await api.getHistory({ pageSize: HISTORY_PAGE_SIZE, pageToken: nextToken }, { quiet: true });
      setMore((m) => ({
        quizzes: [...(m.forKey === owner ? m.quizzes : []), ...page.quizzes],
        nextPageToken: page.nextPageToken,
        forKey: owner,
      }));
    } catch (e) {
      setMoreError(e instanceof ApiError ? e.message : "Couldn't load more.");
    } finally {
      setLoadingMore(false);
    }
  };

  if (quizzes.length === 0) return <EmptyHistory />;

  return (
    <section className="card" style={{ gap: 8 }}>
      <ul className="hl">
        {quizzes.map((quiz) => (
          <HistoryRow key={quiz.quizId} quiz={quiz} me={me} now={now} />
        ))}
      </ul>
      {moreError && <InlineError message={moreError} onRetry={loadMore} />}
      {nextToken && (
        <button type="button" className="btn btn--line btn--sm" style={{ alignSelf: "center" }} onClick={loadMore} disabled={loadingMore}>
          {loadingMore ? "Loading…" : "Load more"}
        </button>
      )}
    </section>
  );
}
