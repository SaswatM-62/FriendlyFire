/**
 * The difference between the server's clock and this browser's, so timer bars match
 * the server's phase deadlines (FF-506: within half a second).
 *
 * Each pong that carries the server's time gives one estimate: the server read its
 * clock about halfway through the round trip. The estimate from the fastest recent
 * round trip is the most accurate, so that's the one we keep.
 */
export class ClockOffset {
  private samples: { rttMs: number; offsetMs: number }[] = [];

  constructor(private readonly keep = 8) {}

  /** `sentAt` and `receivedAt` are this browser's Date.now(); `serverTime` is the server's. */
  addSample(sentAt: number, receivedAt: number, serverTime: number): void {
    const rttMs = receivedAt - sentAt;
    if (rttMs < 0) return;
    const offsetMs = serverTime + rttMs / 2 - receivedAt;
    this.samples.push({ rttMs, offsetMs });
    if (this.samples.length > this.keep) this.samples.shift();
  }

  /** Add this to Date.now() to get the server's time. 0 until the first sample. */
  get offsetMs(): number {
    if (this.samples.length === 0) return 0;
    return this.samples.reduce((best, s) => (s.rttMs < best.rttMs ? s : best)).offsetMs;
  }
}

/** Milliseconds until an ISO deadline, by the server's clock. Never negative. */
export function msUntil(deadline: string | undefined, now: number, offsetMs: number): number {
  if (!deadline) return 0;
  return Math.max(0, new Date(deadline).getTime() - (now + offsetMs));
}
