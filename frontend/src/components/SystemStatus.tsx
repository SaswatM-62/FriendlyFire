"use client";

import { useCallback, useEffect, useState, type ReactNode } from "react";
import { HealthTree } from "@/components/HealthTree";
import { Icon } from "@/components/ui/Icon";
import { fetchDeepHealth, summarize, type HealthNode } from "@/lib/health";

const REFRESH_EVERY_MS = 10_000;

/** Every node that isn't UP, worst first, for the banner. */
function problems(node: HealthNode): HealthNode[] {
  const own = node.status === "UP" ? [] : [node];
  return [...own, ...node.dependencies.flatMap(problems)];
}

/**
 * The whole system's health, from GET /api/health/deep on the Gateway, next to the
 * realtime ping (Status board).
 */
export function SystemStatus({ ping }: { ping: ReactNode }) {
  const [tree, setTree] = useState<HealthNode | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [checkedAt, setCheckedAt] = useState<string | null>(null);

  const refresh = useCallback(() => {
    fetchDeepHealth().then(
      (next) => {
        setTree(next);
        setError(null);
        setCheckedAt(new Date().toLocaleTimeString("en-GB"));
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
  const allUp = summary !== null && summary.passing === summary.total && tree?.status === "UP";
  const bad = tree ? problems(tree).filter((n) => n.dependencies.every((d) => d.status === "UP")) : [];
  const down = bad.some((n) => n.status === "DOWN");

  return (
    <div className="stat">
      <header className="spread">
        <div>
          <h1 className="title">Status</h1>
          <p className="muted">Admins only in production · checked {checkedAt ?? "–"} · auto-refresh every 10 s</p>
        </div>
        <button type="button" className="btn btn--line btn--sm" onClick={refresh}>
          <Icon name="refresh" size={16} />
          Refresh
        </button>
      </header>
      {error && (
        <p className="banner" role="alert" style={{ borderColor: "var(--red-text)" }}>
          <span style={{ color: "var(--red-text)", display: "flex" }}>
            <Icon name="alert" />
          </span>
          <span>
            <strong>Can&apos;t reach the Gateway.</strong> {error}
          </span>
        </p>
      )}
      {summary && (
        <p className="banner" role="status" style={allUp ? { borderColor: "var(--mint-text)" } : down ? { borderColor: "var(--red-text)" } : undefined}>
          <span style={{ color: allUp ? "var(--mint-text)" : down ? "var(--red-text)" : "var(--yellow-text)", display: "flex" }}>
            <Icon name={allUp ? "check" : "alert"} />
          </span>
          <span>
            <strong>{allUp ? "All systems up." : down ? "Down." : "Degraded."}</strong>{" "}
            {allUp
              ? `${summary.passing} of ${summary.total} checks passing.`
              : `${bad.map((n) => n.name).join(", ")} ${bad.length === 1 ? "is" : "are"} ${down ? "not answering" : "slow"}. ${summary.passing} of ${summary.total} checks passing.`}
          </span>
        </p>
      )}
      <div className="stat__grid">
        <section className="card" aria-labelledby="h-dep">
          <h2 id="h-dep" className="label">
            Dependencies · /api/health/deep
          </h2>
          {tree ? <HealthTree node={tree} /> : !error && <p className="small muted">Checking…</p>}
        </section>
        {ping}
      </div>
    </div>
  );
}
