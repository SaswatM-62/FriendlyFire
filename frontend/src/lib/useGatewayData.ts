"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import { ApiError } from "@/lib/api";

export interface GatewayData<T> {
  data: T | null;
  error: ApiError | null;
  loading: boolean;
  reload: () => void;
}

/**
 * Loads something from the Gateway when `key` is set, and again whenever it changes
 * or `reload()` is called. `key` null means "don't load yet" (e.g. still signing in).
 */
export function useGatewayData<T>(key: string | null, load: () => Promise<T>): GatewayData<T> {
  const [attempt, setAttempt] = useState(0);
  const [result, setResult] = useState<{ key: string; data: T | null; error: ApiError | null } | null>(null);
  const loader = useRef(load);
  useEffect(() => {
    loader.current = load;
  });

  const fullKey = key === null ? null : `${key}#${attempt}`;

  useEffect(() => {
    if (fullKey === null) return;
    let cancelled = false;
    loader.current().then(
      (data) => {
        if (!cancelled) setResult({ key: fullKey, data, error: null });
      },
      (e: unknown) => {
        if (cancelled) return;
        const error = e instanceof ApiError ? e : new ApiError(0, "INTERNAL", "Something went wrong.");
        setResult({ key: fullKey, data: null, error });
      },
    );
    return () => {
      cancelled = true;
    };
  }, [fullKey]);

  const reload = useCallback(() => setAttempt((a) => a + 1), []);
  const current = result && result.key === fullKey ? result : null;
  return {
    data: current?.data ?? null,
    error: current?.error ?? null,
    loading: fullKey !== null && current === null,
    reload,
  };
}
