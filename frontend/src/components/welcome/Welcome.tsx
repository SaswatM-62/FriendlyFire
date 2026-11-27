"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState, useSyncExternalStore, type FormEvent, type ReactNode } from "react";
import { useCreateRoom } from "@/components/home/useCreateRoom";
import { useApi } from "@/components/providers/ApiProvider";
import { useMe } from "@/components/providers/MeProvider";
import { useSyncStatus } from "@/components/providers/SyncStatusProvider";
import { linkErrorMessage } from "@/components/profile/ProfileView";
import { Icon } from "@/components/ui/Icon";
import { InlineError } from "@/components/ui/States";
import { ApiError } from "@/lib/api";
import type { Me } from "@/lib/api-types";
import { formatEstimate } from "@/lib/format";
import { isPlaceholderName } from "@/lib/onboarding";
import { clearInvite, pendingInvite } from "@/lib/preferences";
import { useNavigation } from "@/lib/useNavigation";

type Step = "welcome" | "name" | "riot" | "finish";

/** This browser saw the welcome screen, so a return visit starts at the name step. */
const SEEN_KEY = "ff-welcome-seen";
const noop = () => () => {};

function seenWelcome(): boolean {
  try {
    return window.localStorage.getItem(SEEN_KEY) === "1";
  } catch {
    return false;
  }
}

function markSeen() {
  try {
    window.localStorage.setItem(SEEN_KEY, "1");
  } catch {
    // Not remembered; they'll see the welcome again next time.
  }
}

function firstStep(params: URLSearchParams, replay: boolean): Step {
  if (params.get("linked") === "1") return "finish";
  if (params.get("error")) return "riot";
  if (replay) return "welcome";
  return seenWelcome() ? "name" : "welcome";
}

/**
 * /welcome: the new-player tutorial (welcome, your name, your Riot account, then a finish
 * screen). It shows until the player reaches the finish screen, which marks it done in
 * Account (POST /api/me/onboarding). ?replay=1 (How it works) shows it again and saves
 * nothing; the Riot callback comes back with ?linked=1 or ?error=.
 */
export function Welcome({ me }: { me: Me }) {
  const params = useSearchParams();
  const router = useRouter();
  const replay = params.get("replay") === "1";
  const [step, setStep] = useState<Step>(() => firstStep(params, replay));
  const done = Boolean(me.onboardedAt);

  // Someone who already finished, opening /welcome directly, goes home.
  const leaving = done && !replay && step !== "finish";
  useEffect(() => {
    if (leaving) router.replace("/");
  }, [leaving, router]);
  if (leaving) return null;

  const afterName = () => setStep(me.riotLink && !replay ? "finish" : "riot");

  return (
    <div className="mwrap">
      {step === "welcome" && (
        <WelcomeStep
          onNext={() => {
            markSeen();
            setStep("name");
          }}
        />
      )}
      {step === "name" && <NameStep me={me} onBack={() => setStep("welcome")} onNext={afterName} />}
      {step === "riot" && (
        <RiotStep me={me} error={params.get("error")} replay={replay} onBack={() => setStep("name")} onLater={() => setStep("finish")} />
      )}
      {step === "finish" && <FinishStep me={me} replay={replay} />}
    </div>
  );
}

function StepCard({ n, title, onBack, children }: { n?: number; title: string; onBack?: () => void; children: ReactNode }) {
  return (
    <section className="card mcard" style={{ alignItems: "stretch", textAlign: "left" }} aria-labelledby="h-welcome">
      {n && (
        <div className="spread">
          <span className="small muted">Step {n} of 3</span>
          {onBack && (
            <button type="button" className="btn btn--ghost btn--sm" onClick={onBack}>
              <Icon name="arrowLeft" size={16} />
              Back
            </button>
          )}
        </div>
      )}
      <h1 id="h-welcome" className="title" style={{ textWrap: "balance" }}>
        {title}
      </h1>
      {children}
    </section>
  );
}

function WelcomeStep({ onNext }: { onNext: () => void }) {
  return (
    <StepCard n={1} title="Welcome to FriendlyFire">
      <p className="muted">
        Trivia made from your squad&apos;s VALORANT matches. Who topped the scoreboard last week? Who clutched the most rounds? Answer fast to
        score.
      </p>
      <ul className="rules">
        <li>
          <Icon name="clock" size={18} />
          <span>Questions come from your last 30 days of Competitive and Unrated matches.</span>
        </li>
        <li>
          <Icon name="users" size={18} />
          <span>Up to 8 friends in a room.</span>
        </li>
        <li>
          <Icon name="play" size={18} />
          <span>About 5 minutes a quiz.</span>
        </li>
      </ul>
      <div className="btns" style={{ justifyContent: "flex-start" }}>
        <button type="button" className="btn btn--primary" onClick={onNext}>
          Set up my account
          <Icon name="arrowRight" />
        </button>
      </div>
    </StepCard>
  );
}

function NameStep({ me, onBack, onNext }: { me: Me; onBack: () => void; onNext: () => void }) {
  const api = useApi();
  const { setMe } = useMe();
  const placeholder = isPlaceholderName(me.displayName);
  // An email sign-up's "Player 4821" isn't prefilled: it's the hint, and keeping it is fine.
  const [name, setName] = useState(placeholder ? "" : me.displayName);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  const save = async (e: FormEvent) => {
    e.preventDefault();
    const trimmed = name.trim();
    if (trimmed === "" && placeholder) return onNext();
    if (trimmed === me.displayName) return onNext();
    setBusy(true);
    setError(null);
    try {
      setMe(await api.updateDisplayName(trimmed, { quiet: true }));
      onNext();
    } catch (err) {
      setError(err instanceof ApiError ? err.message : "Couldn't save your name. Try again.");
      setBusy(false);
    }
  };

  return (
    <StepCard n={2} title="What should your friends call you?" onBack={onBack}>
      <form onSubmit={save} style={{ display: "flex", flexDirection: "column", gap: 16 }}>
        <div className="field">
          <label className="label" htmlFor="welcome-name">
            Your name
          </label>
          <input
            id="welcome-name"
            className="input"
            value={name}
            maxLength={24}
            placeholder={placeholder ? me.displayName : undefined}
            aria-invalid={error ? true : undefined}
            aria-describedby="name-help"
            onChange={(e) => setName(e.target.value)}
            autoFocus
          />
          <span id="name-help" className="small faint">
            1 to 24 characters. You can change it later on your profile.
          </span>
        </div>
        {error && <InlineError message={error} />}
        <div className="btns" style={{ justifyContent: "flex-start" }}>
          <button type="submit" className="btn btn--primary" disabled={busy}>
            {busy ? "Saving…" : "Save and continue"}
            <Icon name="arrowRight" />
          </button>
        </div>
      </form>
    </StepCard>
  );
}

function useStartRiotLink(): { start: () => Promise<void>; busy: boolean } {
  const api = useApi();
  const [busy, setBusy] = useState(false);
  const start = async () => {
    setBusy(true);
    try {
      const { authorizeUrl } = await api.startRiotLink();
      window.location.assign(authorizeUrl);
    } catch {
      setBusy(false);
    }
  };
  return { start, busy };
}

function RiotStep({
  me,
  error,
  replay,
  onBack,
  onLater,
}: {
  me: Me;
  error: string | null;
  replay: boolean;
  onBack: () => void;
  onLater: () => void;
}) {
  const api = useApi();
  const { setMe } = useMe();
  const { start, busy } = useStartRiotLink();
  const { navigating, go } = useNavigation();
  const [finishing, setFinishing] = useState(false);
  const skipping = finishing || navigating;
  const link = me.riotLink;

  // "Skip tutorial" ends it here, the same as Later, but goes straight home.
  const skip = async () => {
    setFinishing(true);
    try {
      if (!replay) setMe(await api.finishOnboarding());
      go("/");
    } catch {
      // The API client has already shown the error as a toast.
    } finally {
      setFinishing(false);
    }
  };

  return (
    <StepCard n={3} title="Link your Riot account" onBack={onBack}>
      {link ? (
        <p className="row" style={{ gap: 8 }}>
          <Icon name="check" size={18} />
          <span>
            Linked as{" "}
            <strong>
              {link.gameName}#{link.tagLine}
            </strong>
          </span>
        </p>
      ) : (
        <>
          <p className="muted">
            We read your recent Competitive and Unrated matches to make the questions. You confirm it on Riot&apos;s own page; we never see your
            password.
          </p>
          <p className="small" style={{ color: "var(--yellow-text)" }}>
            Every player needs a linked and synced account before a quiz can start.
          </p>
        </>
      )}
      {error && (
        <p className="small" role="alert" style={{ color: "var(--red-text)" }}>
          <strong>Couldn&apos;t link your Riot account.</strong> {linkErrorMessage(error)}
        </p>
      )}
      <div className="btns" style={{ justifyContent: "flex-start" }}>
        {link ? (
          <button type="button" className="btn btn--primary" onClick={onLater}>
            Continue
            <Icon name="arrowRight" />
          </button>
        ) : (
          <>
            <button type="button" className="btn btn--primary" onClick={start} disabled={busy}>
              {busy ? "Opening Riot…" : "Link Riot account"}
            </button>
            <button type="button" className="btn btn--line" onClick={onLater}>
              Later
            </button>
          </>
        )}
      </div>
      <button type="button" className="btn btn--ghost btn--sm" style={{ alignSelf: "flex-start" }} onClick={skip} disabled={skipping}>
        Skip tutorial
      </button>
    </StepCard>
  );
}

function FinishStep({ me, replay }: { me: Me; replay: boolean }) {
  const api = useApi();
  const { setMe } = useMe();
  const { sync } = useSyncStatus();
  const { create, busy } = useCreateRoom();
  const { start, busy: linking } = useStartRiotLink();
  const router = useRouter();
  const invite = useSyncExternalStore(noop, pendingInvite, () => null);
  const marked = useRef(false);

  // Reaching this screen finishes the tutorial, so closing the tab here doesn't bring it back.
  useEffect(() => {
    if (replay || me.onboardedAt || marked.current) return;
    marked.current = true;
    api.finishOnboarding({ quiet: true }).then(setMe, () => {
      marked.current = false;
    });
  }, [replay, me.onboardedAt, api, setMe]);

  const linked = Boolean(me.riotLink);
  const join = () => {
    if (!invite) return;
    clearInvite();
    router.push(`/room/${invite}`);
  };

  return (
    <StepCard title={replay ? "That's the tour" : linked ? "You're all set" : "Nearly there"}>
      {replay ? (
        <p className="muted">That&apos;s everything a new player sees. Your account hasn&apos;t changed.</p>
      ) : linked ? (
        <p className="muted" role="status">
          {sync?.status === "SYNCING"
            ? `Your matches are syncing (${formatEstimate(sync.estimatedSeconds)} left).`
            : sync?.status === "SYNCED"
              ? "Your matches are ready."
              : "Your matches will sync in the background."}
        </p>
      ) : (
        <>
          <p className="muted">Link your Riot account before your first quiz: every player needs one.</p>
          <div>
            <button type="button" className="btn btn--line btn--sm" onClick={start} disabled={linking}>
              {linking ? "Opening Riot…" : "Link Riot account now"}
            </button>
          </div>
        </>
      )}
      <div className="btns" style={{ justifyContent: "flex-start" }}>
        {invite ? (
          <button type="button" className="btn btn--primary" onClick={join}>
            Join room {invite}
            <Icon name="arrowRight" />
          </button>
        ) : (
          <button type="button" className="btn btn--primary" onClick={create} disabled={busy}>
            {busy ? "Creating…" : "Create a room"}
            <Icon name="arrowRight" />
          </button>
        )}
        <Link className="btn btn--line" href="/">
          Go to home
        </Link>
      </div>
    </StepCard>
  );
}
