"use client";

import { useSyncExternalStore } from "react";
import { DEFAULT_LOOK, favouriteLook, type FavouriteLook } from "@/lib/preferences";

const LOOK_EVENT = "ff-look-change";
let cached: { raw: string; look: FavouriteLook } | null = null;

function snapshot(): FavouriteLook {
  const look = favouriteLook();
  const raw = `${look.icon}:${look.colour}:${look.expression}`;
  if (!cached || cached.raw !== raw) cached = { raw, look };
  return cached.look;
}

function subscribe(listener: () => void) {
  window.addEventListener(LOOK_EVENT, listener);
  return () => window.removeEventListener(LOOK_EVENT, listener);
}

/** Your favourite character, colour and face (Profile "Your look"), from this browser. */
export function useFavouriteLook(): FavouriteLook {
  return useSyncExternalStore(subscribe, snapshot, () => DEFAULT_LOOK);
}

export function announceLookChange() {
  window.dispatchEvent(new Event(LOOK_EVENT));
}
