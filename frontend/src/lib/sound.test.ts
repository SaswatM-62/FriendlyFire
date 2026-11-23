import { describe, expect, it, vi } from "vitest";
import { SoundBoard } from "@/lib/sound";

describe("SoundBoard", () => {
  it("is on by default, and remembers mute after a reload", () => {
    expect(new SoundBoard().muted).toBe(false);

    new SoundBoard().setMuted(true);

    expect(new SoundBoard().muted).toBe(true);
  });

  it("makes no sound before the first click or tap (no autoplay)", () => {
    const AudioContext = vi.fn();
    vi.stubGlobal("AudioContext", AudioContext);
    const board = new SoundBoard();

    board.play("correct");

    expect(AudioContext).not.toHaveBeenCalled();
    vi.unstubAllGlobals();
  });
});
