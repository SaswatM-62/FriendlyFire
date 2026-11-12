"use client";

import Link from "next/link";
import type { ReactNode } from "react";
import { useMe } from "@/components/providers/MeProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { Icon } from "@/components/ui/Icon";
import { MessageCard, PageSkeleton } from "@/components/ui/States";
import type { Me } from "@/lib/api-types";
import { ERROR_CODES } from "@/lib/errors";

/**
 * A page that needs sign-in (FF-102): a skeleton while signing in and loading your
 * account, a Sign in prompt when signed out, an error with a retry when the Gateway
 * can't load your account. The data itself is protected by the Gateway, not by this.
 */
export function SignedIn({
  what,
  children,
}: {
  what: string;
  children: (me: Me) => ReactNode;
}) {
  const session = useSession();
  const { me, status, error, reload } = useMe();

  if (session.status === "loading" || status === "loading" || (session.status === "signedIn" && status === "idle")) {
    return <PageSkeleton />;
  }
  if (session.status === "signedOut") {
    return (
      <MessageCard
        icon="user"
        title={`Sign in to see ${what}`}
        actions={
          <button type="button" className="btn btn--primary" onClick={session.signIn}>
            Sign in
            <Icon name="arrowRight" />
          </button>
        }
      >
        <p className="muted">FriendlyFire uses your sign-in to find your rooms, quizzes and Riot link.</p>
      </MessageCard>
    );
  }
  if (!me && (error?.code === ERROR_CODES.emailNotVerified || error?.code === ERROR_CODES.inviteOnly)) {
    const verify = error.code === ERROR_CODES.emailNotVerified;
    return (
      <MessageCard
        icon={verify ? "mail" : "lock"}
        tone={verify ? "cyan" : "violet"}
        title={verify ? "Verify your email first" : "FriendlyFire is invite-only for now"}
        actions={
          <Link className="btn btn--primary" href={verify ? "/verify-email" : "/invite-only"}>
            {verify ? "What to do" : "More"}
          </Link>
        }
      >
        <p className="muted">{error.message}</p>
      </MessageCard>
    );
  }
  if (!me) {
    return (
      <MessageCard
        icon="alert"
        tone="red"
        role="alert"
        title="Couldn't load your account"
        actions={
          <button type="button" className="btn btn--primary" onClick={reload}>
            Try again
          </button>
        }
      >
        <p className="muted">{error?.message ?? "Something went wrong."}</p>
      </MessageCard>
    );
  }
  return <>{children(me)}</>;
}
