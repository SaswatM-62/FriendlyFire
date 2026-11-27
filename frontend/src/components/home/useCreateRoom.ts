"use client";

import { useState } from "react";
import { useApi } from "@/components/providers/ApiProvider";
import { cacheRoom } from "@/lib/roomCache";
import { useNavigation } from "@/lib/useNavigation";

/**
 * Creates a room and goes to it; `busy` while the Gateway answers and the room opens.
 * Errors show as a toast. Coming back to this page later finds the button ready again.
 */
export function useCreateRoom(): { create: () => Promise<void>; busy: boolean } {
  const api = useApi();
  const { navigating, go } = useNavigation();
  const [creating, setCreating] = useState(false);
  const create = async () => {
    setCreating(true);
    try {
      const snapshot = await api.createRoom();
      cacheRoom(snapshot);
      go(`/room/${snapshot.roomCode}`);
    } catch {
      // The API client has already shown the error as a toast.
    } finally {
      setCreating(false);
    }
  };
  return { create, busy: creating || navigating };
}
