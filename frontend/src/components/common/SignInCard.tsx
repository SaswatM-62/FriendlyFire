"use client";

import Link from "next/link";
import { useRouter, useSearchParams } from "next/navigation";
import { useEffect } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import { LogoLink } from "@/components/ui/Brand";
import { Icon } from "@/components/ui/Icon";

/**
 * /login (FF-102, Sign in board). Auth0's hosted page does the actual sign-in with
 * Google or email; this is the way in, and where a failed sign-in comes back to.
 */
export function SignInCard({ available }: { available: boolean }) {
  const session = useSession();
  const router = useRouter();
  const error = useSearchParams().get("error");

  useEffect(() => {
    if (session.status === "signedIn") router.replace("/");
  }, [session.status, router]);

  return (
    <section className="card acard nf-phase" aria-labelledby="h-auth">
      <div className="row" style={{ justifyContent: "center" }}>
        <LogoLink />
      </div>
      <div style={{ textAlign: "center" }}>
        <h1 id="h-auth" className="title">
          Welcome back
        </h1>
        <p className="muted">Log in to FriendlyFire with Google or your email.</p>
      </div>
      {error && (
        <p className="row small" role="alert" style={{ justifyContent: "center", gap: 6, color: "var(--red-text)" }}>
          <Icon name="alert" size={16} />
          Sign-in didn&apos;t finish. Try again.
        </p>
      )}
      {available ? (
        <>
          <button type="button" className="btn btn--primary btn--block" onClick={session.signIn}>
            Continue
            <Icon name="arrowRight" />
          </button>
          <p className="alinks" style={{ justifyContent: "center" }}>
            <span className="muted">
              New here? <a href="/auth/login?screen_hint=signup">Sign up</a>
            </span>
          </p>
        </>
      ) : (
        <p className="small muted" role="status" style={{ textAlign: "center" }}>
          Sign-in isn&apos;t set up on this server yet. Developers: fill in the AUTH0_* settings in .env, or run with
          NEXT_PUBLIC_FAKE_BACKEND=true.
        </p>
      )}
      <p className="afoot">
        Hosted by Auth0 · <Link href="/terms">Terms</Link> · <Link href="/privacy">Privacy</Link>
      </p>
    </section>
  );
}
