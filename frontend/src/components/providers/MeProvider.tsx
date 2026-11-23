"use client";

import { useRouter } from "next/navigation";
import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useApi } from "@/components/providers/ApiProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { ApiError } from "@/lib/api";
import type { Me } from "@/lib/api-types";
import { ERROR_CODES } from "@/lib/errors";

export interface MeState {
  me: Me | null;
  status: "idle" | "loading" | "ready" | "error";
  error: ApiError | null;
  reload: () => void;
  /** After PATCH /api/me or linking: replace the stored user. */
  setMe: (me: Me) => void;
}

const MeContext = createContext<MeState>({
  me: null,
  status: "idle",
  error: null,
  reload: () => {},
  setMe: () => {},
});

export function useMe(): MeState {
  return useContext(MeContext);
}

/** Where to send a player the Gateway won't serve yet. */
const GATES: Record<string, string> = {
  [ERROR_CODES.emailNotVerified]: "/verify-email",
  [ERROR_CODES.inviteOnly]: "/invite-only",
};

/**
 * After sign-in, GET /api/me (which creates the user on their first visit, FF-102) and
 * keep the user for every page.
 */
export function MeProvider({ children }: { children: ReactNode }) {
  const session = useSession();
  const api = useApi();
  const router = useRouter();
  const [me, setMe] = useState<Me | null>(null);
  const [status, setStatus] = useState<MeState["status"]>("idle");
  const [error, setError] = useState<ApiError | null>(null);
  const [attempt, setAttempt] = useState(0);

  const signedIn = session.status === "signedIn";
  const [loadedFor, setLoadedFor] = useState<string | null>(null);
  const key = signedIn ? `${session.email}:${attempt}` : null;

  // Signing out clears the user (during render, not in an effect).
  if (!signedIn && (me !== null || status !== "idle")) {
    setMe(null);
    setStatus("idle");
    setError(null);
    setLoadedFor(null);
  }
  if (key !== null && key !== loadedFor) {
    setLoadedFor(key);
    setStatus("loading");
  }

  useEffect(() => {
    if (key === null) return;
    let cancelled = false;
    api.getMe({ quiet: true }).then(
      (user) => {
        if (cancelled) return;
        setMe(user);
        setError(null);
        setStatus("ready");
      },
      (e: unknown) => {
        if (cancelled) return;
        const apiError = e instanceof ApiError ? e : null;
        setError(apiError);
        setStatus("error");
        const gate = apiError && GATES[apiError.code];
        if (gate) router.replace(gate);
      },
    );
    return () => {
      cancelled = true;
    };
  }, [key, api, router]);

  const reload = useCallback(() => setAttempt((a) => a + 1), []);
  const value = useMemo<MeState>(() => ({ me, status, error, reload, setMe }), [me, status, error, reload]);
  return <MeContext.Provider value={value}>{children}</MeContext.Provider>;
}
