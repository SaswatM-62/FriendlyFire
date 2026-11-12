"use client";

import { useSyncExternalStore } from "react";

/**
 * The current time, re-rendering every `everyMs` while mounted. Countdowns read it
 * instead of calling Date.now() while rendering. Returns 0 on the server and before
 * the first tick; callers treat 0 as "not known yet".
 *
 * One timer per interval is shared by every component that asks for it.
 */
const listeners = new Map<number, Set<() => void>>();
const timers = new Map<number, ReturnType<typeof setInterval>>();
// useSyncExternalStore resubscribes whenever `subscribe` changes, so there's exactly
// one subscribe function per interval.
const subscribers = new Map<number, (listener: () => void) => () => void>();
let now = 0;

function tick(everyMs: number) {
  now = Date.now();
  for (const listener of listeners.get(everyMs) ?? []) listener();
}

function subscriberFor(everyMs: number) {
  let subscribe = subscribers.get(everyMs);
  if (subscribe) return subscribe;
  subscribe = (listener: () => void) => {
    let set = listeners.get(everyMs);
    if (!set) {
      set = new Set();
      listeners.set(everyMs, set);
      timers.set(
        everyMs,
        setInterval(() => tick(everyMs), everyMs),
      );
    }
    set.add(listener);
    // A fresh time for the first render, unless one is already recent.
    if (Date.now() - now >= Math.min(everyMs, 250)) queueMicrotask(() => tick(everyMs));
    return () => {
      set.delete(listener);
      if (set.size === 0) {
        clearInterval(timers.get(everyMs));
        timers.delete(everyMs);
        listeners.delete(everyMs);
      }
    };
  };
  subscribers.set(everyMs, subscribe);
  return subscribe;
}

const getNow = () => now;
const getServerNow = () => 0;

export function useNow(everyMs = 1_000): number {
  return useSyncExternalStore(subscriberFor(everyMs), getNow, getServerNow);
}
