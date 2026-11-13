"use client";

import { useSyncExternalStore } from "react";
import { useSound } from "@/components/providers/SoundProvider";
import { Icon } from "@/components/ui/Icon";
import { currentTheme, saveTheme, type Theme } from "@/lib/preferences";

const THEME_EVENT = "ff-theme-change";

function subscribeTheme(listener: () => void) {
  window.addEventListener(THEME_EVENT, listener);
  return () => window.removeEventListener(THEME_EVENT, listener);
}

/** Dark ↔ light, remembered in this browser. */
export function ThemeToggle() {
  const theme = useSyncExternalStore<Theme>(subscribeTheme, currentTheme, () => "dark");
  const next: Theme = theme === "dark" ? "light" : "dark";
  return (
    <button
      type="button"
      className="btn btn--icon"
      aria-label={`Switch to ${next} theme`}
      onClick={() => {
        saveTheme(next);
        window.dispatchEvent(new Event(THEME_EVENT));
      }}
    >
      <Icon name={theme === "dark" ? "moon" : "sun"} />
    </button>
  );
}

/** Sound on/off (FF-511), remembered in this browser. */
export function MuteToggle() {
  const { muted, setMuted } = useSound();
  return (
    <button
      type="button"
      className="btn btn--icon"
      aria-label={muted ? "Turn sound on" : "Mute sound"}
      aria-pressed={muted}
      onClick={() => setMuted(!muted)}
    >
      <Icon name={muted ? "muted" : "sound"} />
    </button>
  );
}
