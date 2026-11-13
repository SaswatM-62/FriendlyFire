"use client";

import { useRouter, useSearchParams } from "next/navigation";
import { useEffect, useRef, useState } from "react";
import Link from "next/link";
import { useApi } from "@/components/providers/ApiProvider";
import { useMe } from "@/components/providers/MeProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { Icon } from "@/components/ui/Icon";
import { MessageCard } from "@/components/ui/States";
import { ApiError } from "@/lib/api";

/**
 * /link/riot/callback (ADR-007): Riot Sign-On (or the mock) sends the browser here with
 * ?code=&state=. A browser redirect carries no sign-in token, so this page sends them
 * to the Gateway itself, then goes to Profile. If the player pressed Deny, Riot sends
 * ?error=access_denied and nothing is sent to the Gateway.
 */
export function RiotCallback() {
  const params = useSearchParams();
  const router = useRouter();
  const api = useApi();
  const session = useSession();
  const { reload } = useMe();
  const started = useRef(false);
  const [slow, setSlow] = useState(false);

  // Riot usually answers in a few seconds; after 8 say so, and offer a way back.
  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), 8_000);
    return () => clearTimeout(timer);
  }, []);

  const code = params.get("code");
  const state = params.get("state");
  const riotError = params.get("error");

  useEffect(() => {
    if (started.current || session.status !== "signedIn") return;
    started.current = true;
    if (riotError) {
      router.replace(`/profile?error=${encodeURIComponent(riotError)}`);
      return;
    }
    if (!code || !state) {
      router.replace("/profile?error=INVALID_ARGUMENT");
      return;
    }
    api.completeRiotLink(code, state, { quiet: true }).then(
      () => {
        reload();
        router.replace("/profile?linked=1");
      },
      (e: unknown) => {
        const errorCode = e instanceof ApiError ? e.code : "UNKNOWN";
        router.replace(`/profile?error=${encodeURIComponent(errorCode)}`);
      },
    );
  }, [session.status, riotError, code, state, api, reload, router]);

  if (session.status === "signedOut") {
    return (
      <MessageCard
        icon="user"
        title="Sign in to finish linking"
        actions={
          <button type="button" className="btn btn--primary" onClick={session.signIn}>
            Sign in
            <Icon name="arrowRight" />
          </button>
        }
      >
        <p className="muted">Your sign-in ended while you were on Riot&apos;s page. Sign in, then link your account again from your profile.</p>
      </MessageCard>
    );
  }

  return (
    <MessageCard
      icon="spinner"
      role="status"
      title="Linking your Riot account…"
      actions={
        slow ? (
          <Link className="btn btn--line" href="/profile">
            Back to profile
          </Link>
        ) : undefined
      }
    >
      <p className="muted">Riot is confirming your account. This usually takes a few seconds.</p>
      {slow && (
        <p className="small" style={{ color: "var(--yellow-text)" }}>
          This is taking longer than usual.
        </p>
      )}
    </MessageCard>
  );
}
