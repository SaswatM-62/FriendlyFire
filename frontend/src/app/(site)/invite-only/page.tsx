import type { Metadata } from "next";
import { InviteOnly } from "@/components/common/GatePages";

export const metadata: Metadata = { title: "Invite-only for now", robots: { index: false } };

export default function InviteOnlyPage() {
  return (
    <>
      <InviteOnly />
    </>
  );
}
