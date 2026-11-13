"use client";

import { use } from "react";
import { RoomPage } from "@/components/room/RoomPage";

export function RoomRoute({ params }: { params: Promise<{ roomCode: string }> }) {
  const { roomCode } = use(params);
  return <RoomPage rawCode={roomCode} />;
}
