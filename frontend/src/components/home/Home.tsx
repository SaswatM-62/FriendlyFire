"use client";

import Link from "next/link";
import { useRouter } from "next/navigation";
import { useId, useState, useSyncExternalStore, type FormEvent } from "react";
import { useFavouriteLook } from "@/components/chrome/useFavouriteLook";
import { DataBadge } from "@/components/common/DataBadge";
import { useCreateRoom } from "@/components/home/useCreateRoom";
import { useApi } from "@/components/providers/ApiProvider";
import { useSyncStatus } from "@/components/providers/SyncStatusProvider";
import { Av } from "@/components/ui/Character";
import { CodeInput } from "@/components/ui/CodeInput";
import { Icon } from "@/components/ui/Icon";
import { ApiError } from "@/lib/api";
import type { Me } from "@/lib/api-types";
import { ERROR_CODES } from "@/lib/errors";
import { formatEstimate, formatPoints, regionName, relativeTime } from "@/lib/format";
import { clearInvite, pendingInvite } from "@/lib/preferences";
import { cacheRoom } from "@/lib/roomCache";
import { useGatewayData } from "@/lib/useGatewayData";
import { useNow } from "@/lib/useNow";

const noop = () => () => {};

/** What to say when joining fails, by the Gateway's error. */
function joinError(error: ApiError): string {
  if (error.status === 404) return "Room not found. Check the code and try again.";
  if (error.code === ERROR_CODES.roomFull) return "That room is full (8 players).";
  if (error.code === ERROR_CODES.quizAlreadyStarted || error.status === 409) {
    return "That quiz has already started. Ask them to play again after it ends.";
  }
  return error.message;
}

/** "Live now · 24 rooms playing · 103 players online" (planned endpoint; hidden until the Gateway has it). */
function LiveStrip() {
  const api = useApi();
  const { data } = useGatewayData("live-stats", () => api.getLiveStats({ quiet: true }));
  if (!data) return null;
  return (
    <section className="stats" aria-label="Live right now">
      <span className="stats__live">
        <i aria-hidden="true" />
        Live now
      </span>
      <span className="stats__item">
        <span className="num">{formatPoints(data.roomsPlaying)}</span> rooms playing
      </span>
      <span className="stats__sep" aria-hidden="true" />
      <span className="stats__item">
        <span className="num">{formatPoints(data.playersOnline)}</span> players online
      </span>
    </section>
  );
}

/** "Join room K7QM2X?" after a signed-out invite link (FF-503). */
function PendingInvite({ code, onDone }: { code: string; onDone: () => void }) {
  const api = useApi();
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const join = async () => {
    setBusy(true);
    try {
      const snapshot = await api.joinRoom(code, { quiet: true });
      clearInvite();
      cacheRoom(snapshot);
      router.push(`/room/${snapshot.roomCode}`);
    } catch (e) {
      setBusy(false);
      setError(e instanceof ApiError ? joinError(e) : "Couldn't join. Try again.");
    }
  };

  return (
    <section className="card bannerc" role="status" aria-label="Pending invite">
      <span className="mico" style={{ width: 40, height: 40, color: "var(--cyan-text)" }}>
        <Icon name="link" />
      </span>
      <p className="grow">
        <strong>Join room {code}?</strong> <span className="muted">{error ?? "You opened an invite link before signing in."}</span>
      </p>
      <button type="button" className="btn btn--mint btn--sm" onClick={join} disabled={busy}>
        {busy ? "Joining…" : "Join"}
      </button>
      <button
        type="button"
        className="btn btn--ghost btn--sm"
        onClick={() => {
          clearInvite();
          onDone();
        }}
      >
        Dismiss
      </button>
    </section>
  );
}

function LinkPrompt() {
  return (
    <section className="card bannerc" style={{ borderColor: "var(--yellow-text)" }} aria-labelledby="h-link">
      <span className="mico" style={{ color: "var(--yellow-text)" }}>
        <Icon name="link" size={22} />
      </span>
      <div className="grow">
        <p id="h-link" className="h2" style={{ fontSize: 17 }}>
          Link your Riot account to play
        </p>
        <p className="small muted">Questions are built from your squad&apos;s matches. You can open or join a room first.</p>
      </div>
      <Link className="btn btn--primary btn--sm" href="/profile#riot">
        Link account
      </Link>
    </section>
  );
}

function StartCard() {
  const { create, busy } = useCreateRoom();
  return (
    <section className="card" aria-labelledby="h-start">
      <h2 id="h-start" className="h2">
        Start a room
      </h2>
      <p className="muted">Invite up to 7 friends who play together. Every question is about one of you. You&apos;ll be the host.</p>
      <button type="button" className="btn btn--primary btn--block" onClick={create} disabled={busy}>
        {busy ? "Creating…" : "Create room"}
        <Icon name="arrowRight" />
      </button>
    </section>
  );
}

function JoinCard() {
  const api = useApi();
  const router = useRouter();
  const errorId = useId();
  const [code, setCode] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const join = async (event: FormEvent) => {
    event.preventDefault();
    if (code.length < 6) {
      setError("Enter all 6 characters of the room code.");
      return;
    }
    setBusy(true);
    setError(null);
    try {
      const snapshot = await api.joinRoom(code, { quiet: true });
      cacheRoom(snapshot);
      router.push(`/room/${snapshot.roomCode}`);
    } catch (e) {
      setError(e instanceof ApiError ? joinError(e) : "Couldn't join. Try again.");
      setBusy(false);
    }
  };

  return (
    <section className="card" aria-labelledby="h-join">
      <h2 id="h-join" className="h2">
        Join a room
      </h2>
      <p className="muted">Got a code from a friend? Type it in.</p>
      <form className="flex flex-col gap-2" onSubmit={join} noValidate>
        <div className="row">
          <CodeInput
            value={code}
            onChange={(next) => {
              setCode(next);
              setError(null);
            }}
            error={error !== null}
            describedBy={error ? errorId : undefined}
          />
          <button type="submit" className="btn btn--mint join-btn" disabled={busy}>
            {busy ? "Joining…" : "Join"}
          </button>
        </div>
        {error && (
          <p id={errorId} className="row small" role="alert" style={{ gap: 6, color: "var(--red-text)" }}>
            <Icon name="alert" size={16} />
            {error}
          </p>
        )}
      </form>
    </section>
  );
}

/** Your account under the cards: character, Riot ID and sync state. */
function MeCard({ me, compact }: { me: Me; compact: boolean }) {
  const look = useFavouriteLook();
  const { sync } = useSyncStatus();
  const now = useNow(60_000);
  const link = me.riotLink;
  return (
    <section className={`card ${compact ? "bannerc" : "me"}`} aria-label="Your account">
      <Av look={look} size={compact ? 40 : 44} />
      <div className="grow">
        <p className="h2" style={compact ? { fontSize: 17 } : undefined}>
          {me.displayName}{" "}
          {link && (
            <span className="small faint" style={{ fontFamily: "var(--font-body)", fontWeight: 500 }}>
              {link.gameName}#{link.tagLine} · {regionName(link.region)}
            </span>
          )}
        </p>
        <p className="row small muted" style={{ gap: 8 }}>
          {!link ? (
            <>
              <DataBadge status="NOT_LINKED" />
              Riot account not linked
            </>
          ) : (
            sync && (
              <>
                <DataBadge status={sync.status} estimatedSeconds={sync.estimatedSeconds} />
                {sync.status === "SYNCED" && sync.lastSyncedAt && now > 0 && (
                  <span>
                    {relativeTime(sync.lastSyncedAt, now)} · {sync.matchesStored ?? 0} matches from the last 30 days
                  </span>
                )}
                {sync.status === "SYNCING" && <span>about {formatEstimate(sync.estimatedSeconds).slice(1)} to go</span>}
              </>
            )
          )}
        </p>
      </div>
      <Link className="btn btn--line btn--sm" href="/profile">
        Profile and history
      </Link>
    </section>
  );
}

/** The home page when signed in (Home and Home, new player boards). */
export function Home({ me }: { me: Me }) {
  const linked = Boolean(me.riotLink);
  const stored = useSyncExternalStore(noop, pendingInvite, () => null);
  const [dismissed, setDismissed] = useState(false);
  const invite = stored && !dismissed ? stored : null;
  const busy = Boolean(invite) || !linked;
  return (
    <div className={`home${busy ? " home--busy" : ""}`}>
      <header>
        <p className="label">
          {linked ? "Welcome back" : "Welcome"}, {me.displayName}
        </p>
        <h1 className="title">Who knows the squad best?</h1>
      </header>
      {!busy && <LiveStrip />}
      {invite && <PendingInvite code={invite} onDone={() => setDismissed(true)} />}
      {!linked && <LinkPrompt />}
      <div className="home__cards">
        <StartCard />
        <JoinCard />
      </div>
      <MeCard me={me} compact={busy} />
    </div>
  );
}
