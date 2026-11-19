"use client";

import { usePathname, useRouter } from "next/navigation";
import { useEffect } from "react";
import { useMe } from "@/components/providers/MeProvider";
import { needsWelcome, roomCodeIn } from "@/lib/onboarding";
import { rememberInvite } from "@/lib/preferences";

/**
 * Sends a signed-in player who hasn't finished the new-player tutorial to /welcome from
 * any page that needs sign-in. A room link they opened is kept as a pending invite, so
 * the tutorial's last step offers to join it.
 */
export function OnboardingGate() {
  const { me, status } = useMe();
  const pathname = usePathname();
  const router = useRouter();

  useEffect(() => {
    if (status !== "ready" || !needsWelcome(pathname, me)) return;
    const code = roomCodeIn(pathname);
    if (code) rememberInvite(code);
    router.replace("/welcome");
  }, [status, me, pathname, router]);

  return null;
}
