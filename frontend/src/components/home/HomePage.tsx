"use client";

import { SignedIn } from "@/components/common/SignedIn";
import { Home } from "@/components/home/Home";
import { Landing } from "@/components/home/Landing";
import { useSession } from "@/components/providers/SessionProvider";
import { PageSkeleton } from "@/components/ui/States";

/** "/": what FriendlyFire is when signed out; create or join a room when signed in. */
export function HomePage() {
  const session = useSession();
  if (session.status === "loading") return <PageSkeleton />;
  if (session.status === "signedOut") return <Landing />;
  return (
    <SignedIn what="your home page">
      {(me) => <Home me={me} />}
    </SignedIn>
  );
}
