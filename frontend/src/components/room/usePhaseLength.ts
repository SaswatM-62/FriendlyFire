import { useState } from "react";

/**
 * How long the current phase lasts, for rings and bars that drain: the time left when this
 * screen first saw it, rounded up to whole seconds. Null until the clock has started.
 * (Someone who arrives partway through sees the rest drain from full.)
 */
export function usePhaseLength(leftMs: number, started: boolean): number | null {
  const [totalMs, setTotalMs] = useState<number | null>(null);
  if (totalMs === null && started) setTotalMs(Math.max(1, Math.ceil(leftMs / 1000)) * 1000);
  return totalMs;
}
