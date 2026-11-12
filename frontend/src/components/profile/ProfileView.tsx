"use client";

import Link from "next/link";
import { usePathname, useRouter, useSearchParams } from "next/navigation";
import { useEffect, useId, useRef, useState, type FormEvent } from "react";
import { DataBadge } from "@/components/common/DataBadge";
import { quizTitle } from "@/components/history/HistoryList";
import { placeTone, QuizAvatars } from "@/components/history/QuizAvatars";
import { useFavouriteLook } from "@/components/chrome/useFavouriteLook";
import { LookPicker } from "@/components/profile/LookPicker";
import { useApi } from "@/components/providers/ApiProvider";
import { useMe } from "@/components/providers/MeProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { useSyncStatus } from "@/components/providers/SyncStatusProvider";
import { Banner } from "@/components/ui/Banner";
import { Av } from "@/components/ui/Character";
import { ConfirmDialog } from "@/components/ui/Dialog";
import { Icon } from "@/components/ui/Icon";
import { useToast } from "@/components/ui/Toasts";
import { ApiError } from "@/lib/api";
import type { Me } from "@/lib/api-types";
import { ERROR_CODES } from "@/lib/errors";
import { formatDate, formatEstimate, ordinal, regionName, relativeTime } from "@/lib/format";
import { useGatewayData } from "@/lib/useGatewayData";
import { useNow } from "@/lib/useNow";
import { InlineError, Skeleton } from "@/components/ui/States";

/** What a failed link means, from the ?error= the callback sends back (FF-110). */
export function linkErrorMessage(code: string): string {
  switch (code) {
    case "access_denied":
      return "You cancelled on Riot's page, so nothing was linked.";
    case ERROR_CODES.riotAlreadyLinked:
    case "ALREADY_EXISTS":
      return "That Riot account is already linked to another FriendlyFire account.";
    case ERROR_CODES.invalidLinkState:
    case "INVALID_ARGUMENT":
      return "That link request expired or wasn't yours. Start linking again.";
    case "UNAVAILABLE":
      return "Riot Sign-On isn't answering right now. Try again in a few minutes.";
    default:
      return "Linking didn't work. Try again.";
  }
}

function DisplayName({ me }: { me: Me }) {
  const api = useApi();
  const { setMe } = useMe();
  const toast = useToast();
  const inputId = useId();
  const hintId = useId();
  const [name, setName] = useState(me.displayName);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const trimmed = name.trim();

  const save = async (event: FormEvent) => {
    event.preventDefault();
    if (trimmed.length < 1 || trimmed.length > 24) {
      setError("Your name needs 1 to 24 characters.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const updated = await api.updateDisplayName(trimmed, { quiet: true });
      setMe(updated);
      setName(updated.displayName);
      toast("Name saved.", "mint");
    } catch (e) {
      setError(e instanceof ApiError ? e.message : "Couldn't save your name. Try again.");
    } finally {
      setBusy(false);
    }
  };

  return (
    <form className="field" onSubmit={save} noValidate>
      <label className="label" htmlFor={inputId}>
        Display name
      </label>
      <span className="row">
        <input
          id={inputId}
          className={`input${error ? " err" : ""}`}
          style={{ flex: 1 }}
          value={name}
          maxLength={24}
          autoComplete="nickname"
          aria-invalid={error ? true : undefined}
          aria-describedby={hintId}
          onChange={(e) => {
            setName(e.target.value);
            setError(null);
          }}
        />
        <button type="submit" className="btn btn--line btn--sm" disabled={busy || trimmed === me.displayName}>
          {busy ? "Saving…" : "Save"}
        </button>
      </span>
      <span id={hintId} className="small" role={error ? "alert" : undefined} style={{ color: error ? "var(--red-text)" : "var(--text-faint)" }}>
        {error ?? "1 to 24 characters. Your squad sees this."}
      </span>
    </form>
  );
}

/** The match-data line: a tag, then when and how many (or what's happening). */
function MatchData({ linked }: { linked: boolean }) {
  const { sync, refresh } = useSyncStatus();
  const now = useNow(60_000);
  if (!linked) return <DataBadge status="NOT_LINKED" />;
  if (!sync) return <span className="small muted">Checking…</span>;
  let text: string;
  if (sync.status === "SYNCING") text = `about ${formatEstimate(sync.estimatedSeconds).slice(1)} left`;
  else if (sync.status === "NOT_SYNCED") text = "over a day old, refreshing";
  else if (sync.status === "FAILED") text = sync.errorMessage ?? "the last sync failed";
  else text = `${sync.lastSyncedAt && now > 0 ? `${relativeTime(sync.lastSyncedAt, now)} · ` : ""}${sync.matchesStored ?? 0} matches`;
  return (
    <span className="row" style={{ gap: 8, flexWrap: "wrap" }} role="status">
      <DataBadge status={sync.status} estimatedSeconds={sync.estimatedSeconds} />
      <span className="small muted">{text}</span>
      {sync.status === "FAILED" && (
        <button type="button" className="btn btn--line btn--sm" onClick={refresh}>
          Retry
        </button>
      )}
    </span>
  );
}

function RiotAccount({ me }: { me: Me }) {
  const api = useApi();
  const [busy, setBusy] = useState(false);
  const link = me.riotLink;

  const start = async () => {
    setBusy(true);
    try {
      const { authorizeUrl } = await api.startRiotLink();
      window.location.assign(authorizeUrl);
    } catch {
      setBusy(false);
    }
  };

  return (
    <div id="riot" className="scroll-mt-6" style={{ display: "flex", flexDirection: "column", gap: 8 }}>
      <p className="label">Riot account</p>
      {link ? (
        <dl className="kv">
          <dt>Riot ID</dt>
          <dd>
            {link.gameName}#{link.tagLine}
          </dd>
          <dt>Linked</dt>
          <dd>{formatDate(link.linkedAt)}</dd>
          <dt>Match data</dt>
          <dd>
            <MatchData linked />
          </dd>
        </dl>
      ) : (
        <>
          <p className="row small muted" style={{ gap: 8 }}>
            <DataBadge status="NOT_LINKED" />
            Link it to play: questions come from your matches.
          </p>
          <button type="button" className="btn btn--primary btn--sm" style={{ alignSelf: "flex-start" }} onClick={start} disabled={busy}>
            <Icon name="link" size={16} />
            {busy ? "Opening Riot…" : "Link Riot account"}
          </button>
        </>
      )}
    </div>
  );
}

function DangerZone({ me }: { me: Me }) {
  const api = useApi();
  const session = useSession();
  const { reload } = useMe();
  const { refresh } = useSyncStatus();
  const toast = useToast();
  const router = useRouter();
  const [dialog, setDialog] = useState<"unlink" | "delete" | null>(null);
  const [busy, setBusy] = useState(false);

  const unlink = async () => {
    setBusy(true);
    try {
      await api.unlinkRiot();
      setDialog(null);
      toast("Riot account unlinked. We deleted your synced matches.", "mint");
      reload();
      refresh();
    } finally {
      setBusy(false);
    }
  };

  const remove = async () => {
    setBusy(true);
    try {
      await api.deleteAccount();
      setDialog(null);
      router.replace("/");
      session.signOut();
    } catch {
      setBusy(false);
    }
  };

  return (
    <div className="danger">
      {me.riotLink && (
        <button type="button" className="btn btn--danger btn--sm" onClick={() => setDialog("unlink")}>
          Unlink Riot account
        </button>
      )}
      <button type="button" className="btn btn--danger btn--sm" onClick={() => setDialog("delete")}>
        Delete account
      </button>
      <ConfirmDialog
        open={dialog === "unlink"}
        title="Unlink your Riot account?"
        danger
        confirmLabel="Unlink"
        busy={busy}
        onConfirm={unlink}
        onCancel={() => setDialog(null)}
      >
        We delete your synced matches. You can&apos;t play until you link again. Your history stays.
      </ConfirmDialog>
      <ConfirmDialog
        open={dialog === "delete"}
        title="Delete your account?"
        danger
        confirmLabel="Delete account"
        typeToConfirm="DELETE"
        busy={busy}
        onConfirm={remove}
        onCancel={() => setDialog(null)}
      >
        This deletes your profile, linked Riot account and match data. In past quizzes you&apos;ll show as &quot;Deleted player&quot;. It
        can&apos;t be undone.
      </ConfirmDialog>
    </div>
  );
}

/** The left card: who you are, your name, your Riot account, and the way out. */
function AccountCard({ me }: { me: Me }) {
  const look = useFavouriteLook();
  const link = me.riotLink;
  return (
    <section className="card" aria-labelledby="h-acc">
      <div className="row">
        <Av look={look} size={56} />
        <div>
          <h2 id="h-acc" className="h2">
            {me.displayName}
          </h2>
          <p className="small faint">{link ? `${link.gameName}#${link.tagLine} · ${regionName(link.region)}` : "Riot account not linked"}</p>
        </div>
      </div>
      <DisplayName key={me.displayName} me={me} />
      <RiotAccount me={me} />
      <p className="small faint">
        We use your Competitive and Unrated matches from the last 30 days. Only people in your room see questions about you. More in the{" "}
        <Link href="/privacy" style={{ color: "var(--cyan-text)" }}>
          Privacy Policy
        </Link>
        .
      </p>
      <DangerZone me={me} />
    </section>
  );
}

/** The right card: your last five quizzes, each linking to its results. */
function RecentQuizzes({ me }: { me: Me }) {
  const api = useApi();
  const now = useNow(60_000);
  const { data, error, loading, reload } = useGatewayData(`recent:${me.userId}`, () => api.getHistory({ pageSize: 5 }, { quiet: true }));
  return (
    <section className="card rq" aria-labelledby="h-rq">
      <div className="spread">
        <h2 id="h-rq" className="label">
          Recent quizzes
        </h2>
        <Link className="small" href="/history" style={{ color: "var(--cyan-text)" }}>
          See all history
        </Link>
      </div>
      {loading && (
        <div className="flex flex-col gap-3" aria-busy="true">
          <Skeleton width="100%" height={48} />
          <Skeleton width="100%" height={48} />
          <Skeleton width="100%" height={48} />
        </div>
      )}
      {error && <InlineError message="Couldn't load your quizzes." onRetry={reload} />}
      {data && data.quizzes.length === 0 && <p className="small muted">No quizzes yet. Start a room and play one!</p>}
      {data && data.quizzes.length > 0 && (
        <ul style={{ display: "flex", flexDirection: "column", gap: 2 }}>
          {data.quizzes.map((quiz) => {
            const rank = quiz.finalScoreboard.find((s) => s.userId === me.userId)?.rank ?? 0;
            return (
              <li key={quiz.quizId}>
                <Link href={`/history/${quiz.quizId}`}>
                  <span className="place" style={{ color: `var(--${placeTone(rank)}-text)` }}>
                    {rank ? ordinal(rank) : "–"}
                  </span>
                  <span style={{ minWidth: 0 }}>
                    <span className="h2" style={{ fontSize: 16, display: "block" }}>
                      {quizTitle(quiz, me)}
                    </span>
                    <span className="small faint" style={{ whiteSpace: "nowrap" }}>
                      {now > 0 ? relativeTime(quiz.finishedAt, now) : " "}
                    </span>
                  </span>
                  <QuizAvatars quiz={quiz} meId={me.userId} />
                  <span className="faint">
                    <Icon name="chevronRight" size={16} />
                  </span>
                </Link>
              </li>
            );
          })}
        </ul>
      )}
    </section>
  );
}

/** Shows the result of linking (?linked=1 or ?error=...), then tidies the address. */
function LinkResult() {
  const params = useSearchParams();
  const router = useRouter();
  const pathname = usePathname();
  const toast = useToast();
  const { me } = useMe();
  const shown = useRef(false);
  const errorCode = params.get("error");
  const linked = params.get("linked") === "1";
  const [error] = useState(errorCode);

  useEffect(() => {
    if (shown.current || (!linked && !errorCode)) return;
    // After linking, wait for the reloaded account so the toast can name the Riot ID.
    if (linked && !me?.riotLink) return;
    shown.current = true;
    if (linked && me?.riotLink) toast(`Linked as ${me.riotLink.gameName}#${me.riotLink.tagLine}. Syncing your matches now.`, "mint");
    router.replace(pathname);
  }, [linked, errorCode, me, toast, router, pathname]);

  if (!error) return null;
  return (
    <Banner tone="red" icon="alert" role="alert">
      <strong>Couldn&apos;t link your Riot account.</strong> {linkErrorMessage(error)}
    </Banner>
  );
}

/** /profile (FF-504, Profile board): account, look, recent quizzes. */
export function ProfileView({ me }: { me: Me }) {
  const session = useSession();
  return (
    <>
      <header className="spread">
        <div>
          <h1 className="title">Profile</h1>
          <p className="muted">Your name, your look, your quizzes and the data we use.</p>
        </div>
        <button type="button" className="btn btn--ghost btn--sm" onClick={session.signOut}>
          <Icon name="signOut" size={16} />
          Sign out
        </button>
      </header>
      <LinkResult />
      <div className="prof">
        <AccountCard me={me} />
        <LookPicker />
        <RecentQuizzes me={me} />
      </div>
    </>
  );
}
