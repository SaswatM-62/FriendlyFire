"use client";

import { useSession } from "@/components/providers/SessionProvider";
import { MessageCard } from "@/components/ui/States";

/** "m***@example.com". */
export function maskEmail(email: string | null): string {
  if (!email || !email.includes("@")) return "your email address";
  const [name, domain] = email.split("@");
  return `${name[0]}***@${domain}`;
}

/** Email/password accounts must verify their email first (FF-103, Verify board). */
export function VerifyEmail() {
  const session = useSession();
  return (
    <MessageCard
      icon="mail"
      tone="cyan"
      title="Check your inbox"
      actions={
        <>
          <button type="button" className="btn btn--primary" onClick={session.signIn}>
            I&apos;ve verified, continue
          </button>
          <button type="button" className="btn btn--ghost" onClick={session.signOut}>
            Sign out
          </button>
        </>
      }
    >
      <p className="muted">
        We sent a link to <strong>{maskEmail(session.email)}</strong>. Click it, then sign in again.
      </p>
      <p className="small faint">Can&apos;t find it? Check your spam folder.</p>
    </MessageCard>
  );
}

/** Before Riot approves us, only invited testers get in (FF-618, InviteOnly board). */
export function InviteOnly() {
  const session = useSession();
  return (
    <MessageCard
      icon="lock"
      tone="violet"
      title="FriendlyFire is invite-only for now"
      actions={
        <button type="button" className="btn btn--line" onClick={session.signOut}>
          Sign out
        </button>
      }
    >
      <p className="muted">We&apos;re waiting on Riot to approve us. We&apos;ll open up soon.</p>
      {session.email && <p className="small faint">Signed in as {session.email}</p>}
    </MessageCard>
  );
}
