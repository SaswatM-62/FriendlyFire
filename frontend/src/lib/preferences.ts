import type { PlayerLook } from "@/lib/api-types";
import { isCharacterId, isColourId, isPickableExpression } from "@/lib/characters";
import { readJson, readStorage, writeStorage } from "@/lib/storage";

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

const LOOK_KEY = "ff-look";
export const DEFAULT_LOOK: Required<Pick<PlayerLook, "icon" | "colour" | "expression">> = {
  icon: "blob",
  colour: "pink",
  expression: "happy",
};

export type FavouriteLook = typeof DEFAULT_LOOK;

/**
 * TODO(ff): save the favourite look on the server so rooms can use it
 *   - needs a story and a contract change: a look on the Account user and on Room's
 *     Player; Room hands out icons and colours first come, first served, random
 *     otherwise; expressions are never changed
 *   - until then it's kept in this browser and only shown on your own screens
 */
export function favouriteLook(): FavouriteLook {
  const saved = readJson(LOOK_KEY, (v): v is Partial<FavouriteLook> => typeof v === "object" && v !== null);
  if (!saved || !isCharacterId(saved.icon) || !isColourId(saved.colour)) return DEFAULT_LOOK;
  return { icon: saved.icon, colour: saved.colour, expression: isPickableExpression(saved.expression) ? saved.expression : "happy" };
}

export function saveFavouriteLook(look: FavouriteLook): void {
  writeStorage(LOOK_KEY, JSON.stringify(look));
}
