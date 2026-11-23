"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import type { ReactNode } from "react";
import { useFavouriteLook } from "@/components/chrome/useFavouriteLook";
import { MuteToggle, ThemeToggle } from "@/components/chrome/Toggles";
import { useMe } from "@/components/providers/MeProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { LogoLink } from "@/components/ui/Brand";
import { Av } from "@/components/ui/Character";

/** Pages for players the Gateway won't serve yet: their card has Sign out, the header nothing more. */
const GATED = ["/verify-email", "/invite-only"];

/**
 * The header on every page outside a room: logo, then theme and sound, and you (your
 * character and name) or How it works and Sign in.
 */
export function TopBar() {
  const session = useSession();
  const { me } = useMe();
  const look = useFavouriteLook();
  const pathname = usePathname();
  const gated = GATED.includes(pathname);
  const linking = pathname.startsWith("/link/");
  const signedOut = session.status === "signedOut";

  let you: ReactNode = null;
  if (signedOut) {
    you = (
      <button type="button" className="btn btn--line btn--sm" onClick={session.signIn}>
        Sign in
      </button>
    );
  } else if (session.status === "signedIn" && !gated && !linking) {
    const name = me?.displayName ?? "Profile";
    you = (
      <Link className="chip" href="/profile" aria-label={`Profile: ${name}`}>
        <Av look={look} size={22} />
        {name}
      </Link>
    );
  }

  return (
    <header className="bar">
      <LogoLink />
      <div className="bar__side">
        {(signedOut || gated) && (
          <Link className="btn btn--ghost btn--sm bar__how" href="/how-it-works">
            How it works
          </Link>
        )}
        <ThemeToggle />
        <MuteToggle />
        {you}
      </div>
    </header>
  );
}

/** The header before the page knows who you are (it reads the address, so it streams in). */
export function StaticTopBar() {
  return (
    <header className="bar">
      <LogoLink />
    </header>
  );
}
