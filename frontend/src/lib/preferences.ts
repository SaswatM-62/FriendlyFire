import type { PlayerLook } from "@/lib/api-types";
import { readStorage, writeStorage } from "@/lib/storage";

// ---------- Theme ----------

export type Theme = "dark" | "light";
const THEME_KEY = "ff-theme";

/**
 * Runs before the page paints (inline in <head>), so a light-theme player never sees a
 * flash of dark. Dark is the default.
 */
export const THEME_SCRIPT = `(function(){try{var t=localStorage.getItem("${THEME_KEY}");if(t==="light"||t==="dark")document.documentElement.setAttribute("data-theme",t)}catch(e){}})()`;

export function currentTheme(): Theme {
  return document.documentElement.getAttribute("data-theme") === "light" ? "light" : "dark";
}

export function saveTheme(theme: Theme): void {
  document.documentElement.setAttribute("data-theme", theme);
  writeStorage(THEME_KEY, theme);
}

// ---------- Pending invite (FF-503) ----------

const INVITE_KEY = "ff-pending-invite";

/** A signed-out player opened /room/{code}: remember it, offer it after sign-in. */
export function rememberInvite(code: string): void {
  writeStorage(INVITE_KEY, code);
}

export function pendingInvite(): string | null {
  const code = readStorage(INVITE_KEY);
  return code && /^[A-Z0-9]{6}$/.test(code) ? code : null;
}

export function clearInvite(): void {
  writeStorage(INVITE_KEY, null);
}

// ---------- Favourite look (Profile "Your look") ----------

/** The look of someone who hasn't picked one: the pink Blob, happy. The account keeps the real one (FF-519). */
export const DEFAULT_LOOK: Required<Pick<PlayerLook, "icon" | "colour" | "expression">> = {
  icon: "blob",
  colour: "pink",
  expression: "happy",
};

export type FavouriteLook = typeof DEFAULT_LOOK;
