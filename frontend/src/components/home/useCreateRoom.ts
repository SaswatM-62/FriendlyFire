"use client";

import { useRouter } from "next/navigation";
import { useState } from "react";
import { useApi } from "@/components/providers/ApiProvider";
import { cacheRoom } from "@/lib/roomCache";

/** Creates a room and goes to it; `busy` while the Gateway answers. Errors show as a toast. */
export function useCreateRoom(): { create: () => Promise<void>; busy: boolean } {
  const api = useApi();
  const router = useRouter();
  const [busy, setBusy] = useState(false);
  const create = async () => {
    setBusy(true);
    try {
      const snapshot = await api.createRoom();
      cacheRoom(snapshot);
      router.push(`/room/${snapshot.roomCode}`);
    } catch {
      setBusy(false);
    }
  };
  return { create, busy };
}
