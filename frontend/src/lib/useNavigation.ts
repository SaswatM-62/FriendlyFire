"use client";

import { useRouter } from "next/navigation";
import { useCallback, useTransition } from "react";

/**
 * Goes to another page; `navigating` is true until that page has opened.
 *
 * With cacheComponents, Next.js keeps a page you leave alive and shows it again, state
 * and all, when you come back. So a button must never stay "busy" until its page goes
 * away: it would still say "Creating…" when you return. Reset your own state in
 * `finally` and show `navigating` for the wait instead.
 */
export function useNavigation(): {
  navigating: boolean;
  go: (href: string, options?: { replace?: boolean }) => void;
} {
  const router = useRouter();
  const [navigating, startTransition] = useTransition();
  const go = useCallback(
    (href: string, options: { replace?: boolean } = {}) => {
      startTransition(() => {
        if (options.replace) router.replace(href);
        else router.push(href);
      });
    },
    [router],
  );
  return { navigating, go };
}
