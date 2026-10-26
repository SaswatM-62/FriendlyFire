import { RealtimeStatus } from "@/components/RealtimeStatus";
import { SystemStatus } from "@/components/SystemStatus";

export default function StatusPage() {
  return (
    <main className="mx-auto w-full max-w-3xl space-y-6 p-8">
      <h1 className="text-3xl font-bold">System status</h1>
      <RealtimeStatus />
      <SystemStatus />
    </main>
  );
}
