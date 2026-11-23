import type { Metadata } from "next";
import { Suspense } from "react";
import { RiotCallback } from "@/components/profile/RiotCallback";
import { Spinner } from "@/components/ui/States";

export const metadata: Metadata = { title: "Linking your Riot account", robots: { index: false } };

export default function RiotCallbackPage() {
  return (
    <>
      <Suspense fallback={<Spinner size={56} />}>
        <RiotCallback />
      </Suspense>
    </>
  );
}
