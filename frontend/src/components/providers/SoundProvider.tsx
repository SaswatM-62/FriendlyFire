"use client";

import { createContext, useCallback, useContext, useEffect, useMemo, useState, useSyncExternalStore, type ReactNode } from "react";
import { SoundBoard, type SoundName } from "@/lib/sound";

interface SoundState {
  muted: boolean;
  setMuted: (muted: boolean) => void;
  play: (name: SoundName) => void;
}

const SoundContext = createContext<SoundState>({ muted: false, setMuted: () => {}, play: () => {} });

export function useSound(): SoundState {
  return useContext(SoundContext);
}

/** Sound effects (FF-511): on by default, mute remembered, unlocked by the first tap. */
export function SoundProvider({ children }: { children: ReactNode }) {
  const [board] = useState(() => new SoundBoard());
  const subscribe = useCallback((listener: () => void) => board.subscribe(listener), [board]);
  const muted = useSyncExternalStore(subscribe, () => board.muted, () => false);

  useEffect(() => {
    const unlock = () => {
      board.unlock();
      window.removeEventListener("pointerdown", unlock);
      window.removeEventListener("keydown", unlock);
    };
    window.addEventListener("pointerdown", unlock);
    window.addEventListener("keydown", unlock);
    return () => {
      window.removeEventListener("pointerdown", unlock);
      window.removeEventListener("keydown", unlock);
    };
  }, [board]);

  const value = useMemo<SoundState>(
    () => ({ muted, setMuted: (m) => board.setMuted(m), play: (name) => board.play(name) }),
    [board, muted],
  );
  return <SoundContext.Provider value={value}>{children}</SoundContext.Provider>;
}
