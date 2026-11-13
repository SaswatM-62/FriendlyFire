import type { Metadata } from "next";
import { RealtimeStatus } from "@/components/RealtimeStatus";
import { SystemStatus } from "@/components/SystemStatus";

export const metadata: Metadata = { title: "Status", robots: { index: false } };

/** The dependency health tree and the realtime ping (admin-only in production, FF-602). */
export default function StatusPage() {
  return <SystemStatus ping={<RealtimeStatus />} />;
}
