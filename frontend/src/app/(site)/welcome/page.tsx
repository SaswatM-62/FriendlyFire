import type { Metadata } from "next";
import { Suspense } from "react";
import { WelcomeRoute } from "@/components/welcome/WelcomeRoute";
import { PageSkeleton } from "@/components/ui/States";

export const metadata: Metadata = { title: "Welcome", robots: { index: false } };

export default function WelcomePage() {
  return (
    <>
      <Suspense fallback={<PageSkeleton />}>
        <WelcomeRoute />
      </Suspense>
    </>
  );
}
