"use client";

import { useCallback, useEffect, useState } from "react";
import { HealthTree } from "@/components/HealthTree";
import { fetchDeepHealth, summarize, type HealthNode } from "@/lib/health";

const REFRESH_EVERY_MS = 10_000;

/** The whole system's health, from GET /api/health/deep on the Gateway. */
export function SystemStatus() {
  const [tree, setTree] = useState<HealthNode | null>(null);
  const [error, setError] = useState<string | null>(null);

  const refresh = useCallback(() => {
    fetchDeepHealth().then(
      (next) => {
        setTree(next);
        setError(null);
      },
      (e: unknown) => setError(e instanceof Error ? e.message : String(e)),
    );
  }, []);

  useEffect(() => {
    refresh();
    const timer = setInterval(refresh, REFRESH_EVERY_MS);
    return () => clearInterval(timer);
  }, [refresh]);

  const summary = tree && summarize(tree);

  return (
    <section className="space-y-4">
      <div className="flex items-center gap-4">
        <p className="text-lg" role="status">
          {summary ? `${summary.passing} of ${summary.total} checks passing` : "Checking..."}
        </p>
        <button
          type="button"
          onClick={refresh}
          className="rounded border border-gray-400 px-3 py-1 text-sm hover:bg-gray-100 dark:hover:bg-gray-800"
        >
          Refresh
        </button>
      </div>
      {error && <p className="text-red-700 dark:text-red-300">Can&apos;t reach the Gateway: {error}</p>}
      {tree && (
        <ul>
          <HealthTree node={tree} />
        </ul>
      )}
    </section>
  );
}
