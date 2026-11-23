"use client";

import { createContext, useContext } from "react";
import type { Player, PlayerLook, RoomActions, RoomAction, RoomSnapshot } from "@/lib/api-types";

export interface RoomView {
  snapshot: RoomSnapshot;
  meId: string;
  isHost: boolean;
  /** Everyone's character, by userId. */
  looks: Map<string, PlayerLook>;
  /** Everyone's name, with Riot tags where two share a name. */
  names: Map<string, string>;
  clockOffsetMs: number;
  send: <A extends RoomAction>(action: A, body: RoomActions[A]) => void;
  /** Ask before leaving (the Leave buttons). */
  askLeave: () => void;
}

export const RoomContext = createContext<RoomView | null>(null);

export function useRoom(): RoomView {
  const room = useContext(RoomContext);
  if (!room) throw new Error("useRoom needs a RoomContext");
  return room;
}

/** A player's name and look, with fallbacks for someone who has just left. */
export function usePlayerInfo() {
  const { looks, names, snapshot } = useRoom();
  return (userId: string): { name: string; look: PlayerLook; player: Player | undefined } => ({
    name: names.get(userId) ?? snapshot.scoreboard.find((s) => s.userId === userId)?.displayName ?? "Someone",
    look: looks.get(userId) ?? { icon: "drop", colour: "silver" },
    player: snapshot.players.find((p) => p.userId === userId),
  });
}
