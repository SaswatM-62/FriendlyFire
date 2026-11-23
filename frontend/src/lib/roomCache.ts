import type { RoomSnapshot } from "@/lib/api-types";

/**
 * The snapshot from creating or joining a room, handed to the room page so it can draw
 * straight away instead of loading the room again. Kept in memory for one navigation.
 */
const cache = new Map<string, RoomSnapshot>();

export function cacheRoom(snapshot: RoomSnapshot): void {
  cache.set(snapshot.roomCode, snapshot);
}

export function takeCachedRoom(code: string): RoomSnapshot | null {
  const snapshot = cache.get(code) ?? null;
  cache.delete(code);
  return snapshot;
}
