import type { ConnectionQuality } from "@/lib/api-types";

/** The network bar pings the Gateway this often (Architecture 2.5). */
export const PING_EVERY_MS = 3_000;

/**
 * GOOD under 150 ms, FAIR 150–300 ms, POOR over 300 ms or after a missed ping
 * (no pong before the next ping is due).
 */
export function qualityFromRtt(rttMs: number | null, missedPings: number): ConnectionQuality {
  if (missedPings > 0 || rttMs === null) return "POOR";
  if (rttMs < 150) return "GOOD";
  if (rttMs <= 300) return "FAIR";
  return "POOR";
}

export const QUALITY_LABEL: Record<ConnectionQuality, string> = {
  GOOD: "Good",
  FAIR: "Fair",
  POOR: "Poor",
};

/** Reconnect delays: 1, 2, 4, 8, 16 seconds, then every 30 (Architecture 3.1). */
export function backoffMs(attempt: number): number {
  return Math.min(30_000, 1_000 * 2 ** Math.max(0, attempt));
}
