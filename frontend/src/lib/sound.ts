import { readStorage, writeStorage } from "@/lib/storage";

export type SoundName = "tick" | "go" | "lock" | "correct" | "wrong" | "reveal" | "podium" | "join";

const MUTE_KEY = "ff-muted";

/** One note: frequency (Hz), start offset and length (s), waveform and volume. */
interface Note {
  f: number;
  at: number;
  len: number;
  type?: OscillatorType;
  gain?: number;
  /** Glide to this frequency over the note. */
  to?: number;
}

/**
 * The sounds (FF-511), synthesised with the Web Audio API: no audio files to license
 * or download, and nothing to credit.
 */
const SOUNDS: Record<SoundName, Note[]> = {
  tick: [{ f: 880, at: 0, len: 0.06, type: "square", gain: 0.05 }],
  go: [{ f: 1320, at: 0, len: 0.18, type: "square", gain: 0.06 }],
  lock: [
    { f: 660, at: 0, len: 0.07, type: "triangle", gain: 0.12 },
    { f: 990, at: 0.07, len: 0.1, type: "triangle", gain: 0.12 },
  ],
  correct: [
    { f: 523, at: 0, len: 0.1, type: "triangle", gain: 0.14 },
    { f: 659, at: 0.09, len: 0.1, type: "triangle", gain: 0.14 },
    { f: 784, at: 0.18, len: 0.22, type: "triangle", gain: 0.14 },
  ],
  wrong: [{ f: 320, to: 180, at: 0, len: 0.35, type: "sawtooth", gain: 0.06 }],
  reveal: [{ f: 440, to: 880, at: 0, len: 0.25, type: "sine", gain: 0.1 }],
  podium: [
    { f: 523, at: 0, len: 0.14, type: "square", gain: 0.06 },
    { f: 659, at: 0.14, len: 0.14, type: "square", gain: 0.06 },
    { f: 784, at: 0.28, len: 0.14, type: "square", gain: 0.06 },
    { f: 1047, at: 0.42, len: 0.5, type: "square", gain: 0.07 },
  ],
  join: [{ f: 740, at: 0, len: 0.08, type: "sine", gain: 0.1 }],
};

/**
 * Plays the sounds. On by default; mute is remembered in this browser. Browsers block
 * audio until the first click, tap or key press, so the AudioContext is only created
 * then (no autoplay warnings); sounds before that are skipped.
 */
export class SoundBoard {
  private ctx: AudioContext | null = null;
  private listeners = new Set<() => void>();
  muted: boolean;

  constructor() {
    this.muted = readStorage(MUTE_KEY) === "1";
  }

  /** Call from a user gesture. */
  unlock(): void {
    if (this.ctx || typeof window === "undefined" || !("AudioContext" in window)) return;
    try {
      this.ctx = new AudioContext();
    } catch {
      this.ctx = null;
    }
  }

  setMuted(muted: boolean): void {
    this.muted = muted;
    writeStorage(MUTE_KEY, muted ? "1" : null);
    for (const listener of this.listeners) listener();
  }

  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  play(name: SoundName): void {
    const ctx = this.ctx;
    if (this.muted || !ctx) return;
    if (ctx.state === "suspended") void ctx.resume();
    const start = ctx.currentTime + 0.01;
    for (const note of SOUNDS[name]) {
      const osc = ctx.createOscillator();
      const gain = ctx.createGain();
      osc.type = note.type ?? "sine";
      osc.frequency.setValueAtTime(note.f, start + note.at);
      if (note.to) osc.frequency.exponentialRampToValueAtTime(note.to, start + note.at + note.len);
      gain.gain.setValueAtTime(note.gain ?? 0.1, start + note.at);
      gain.gain.exponentialRampToValueAtTime(0.0001, start + note.at + note.len);
      osc.connect(gain).connect(ctx.destination);
      osc.start(start + note.at);
      osc.stop(start + note.at + note.len + 0.02);
    }
  }
}
