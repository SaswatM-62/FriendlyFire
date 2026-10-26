import type { HealthStatus } from "@/lib/health";

const COLORS: Record<HealthStatus, string> = {
  UP: "bg-green-100 text-green-800 dark:bg-green-900 dark:text-green-100",
  DEGRADED: "bg-amber-100 text-amber-800 dark:bg-amber-900 dark:text-amber-100",
  DOWN: "bg-red-100 text-red-800 dark:bg-red-900 dark:text-red-100",
};

export function StatusBadge({ status }: { status: HealthStatus }) {
  return (
    <span className={`rounded px-2 py-0.5 font-mono text-xs font-semibold ${COLORS[status]}`}>
      {status}
    </span>
  );
}
