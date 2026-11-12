import type { Metadata } from "next";
import { Suspense } from "react";
import { ProfileRoute } from "@/components/profile/ProfileRoute";
import { PageSkeleton } from "@/components/ui/States";

export const metadata: Metadata = { title: "Profile" };

export default function ProfilePage() {
  return (
    <>
      <Suspense fallback={<PageSkeleton />}>
        <ProfileRoute />
      </Suspense>
    </>
  );
}
