"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from "react";
import { useApi } from "@/components/providers/ApiProvider";
import { useMe } from "@/components/providers/MeProvider";
import type { SyncStatusResponse } from "@/lib/api-types";

interface SyncState {
  sync: SyncStatusResponse | null;
  refresh: () => void;
}

const SyncContext = createContext<SyncState>({ sync: null, refresh: () => {} });

export function useSyncStatus(): SyncState {
  return useContext(SyncContext);
}

const POLL_MS = 10_000;

/**
 * The player's match-data status (Architecture 2.3): asked when the app loads and
 * every 10 seconds while a sync is running. Drives the sync banner on every page,
 * the profile and the home page.
 */
export function SyncStatusProvider({ children }: { children: ReactNode }) {
  const api = useApi();
  const { me } = useMe();
  const [sync, setSync] = useState<SyncStatusResponse | null>(null);
  const [attempt, setAttempt] = useState(0);
  const linked = Boolean(me?.riotLink);
  const userId = me?.userId ?? null;

  const [trackedUser, setTrackedUser] = useState(userId);
  if (trackedUser !== userId) {
    setTrackedUser(userId);
    setSync(null);
  }

  useEffect(() => {
    if (!userId) return;
    let cancelled = false;
    let timer: ReturnType<typeof setTimeout> | undefined;
    const load = () => {
      api.getSyncStatus({ quiet: true }).then(
        (next) => {
          if (cancelled) return;
          setSync(next);
          const busy = next.status === "SYNCING" || next.status === "NOT_SYNCED";
          if (busy) timer = setTimeout(load, POLL_MS);
        },
        () => {
          // The banner just stays hidden; the profile shows its own error.
        },
      );
    };
    load();
    return () => {
      cancelled = true;
      clearTimeout(timer);
    };
  }, [api, userId, linked, attempt]);

  const refresh = useCallback(() => setAttempt((a) => a + 1), []);
  const value = useMemo(() => ({ sync, refresh }), [sync, refresh]);
  return <SyncContext.Provider value={value}>{children}</SyncContext.Provider>;
}
