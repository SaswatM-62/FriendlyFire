import type { Metadata } from "next";
import { Suspense } from "react";
import { RoomRoute } from "@/components/room/RoomRoute";

// Invite links preview as an invitation without saying who's in the room (FF-514).
export const metadata: Metadata = {
  title: "Join my FriendlyFire room",
  description: "You're invited to a FriendlyFire quiz: trivia about your squad's VALORANT matches.",
  openGraph: {
    title: "Join my FriendlyFire room",
    description: "You're invited to a FriendlyFire quiz: trivia about your squad's VALORANT matches.",
  },
  robots: { index: false },
};

export default function RoomPage({ params }: PageProps<"/room/[roomCode]">) {
  return (
    <Suspense fallback={null}>
      <RoomRoute params={params} />
    </Suspense>
  );
}
