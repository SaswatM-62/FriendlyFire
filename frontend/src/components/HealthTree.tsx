import type { HealthNode, HealthStatus } from "@/lib/health";

const TAG: Record<HealthStatus, { cls: string; label: string }> = {
  UP: { cls: "tag--mint", label: "Up" },
  DEGRADED: { cls: "tag--yellow", label: "Degraded" },
  DOWN: { cls: "tag--red", label: "Down" },
};

function rows(node: HealthNode, depth: number, path: string): { node: HealthNode; depth: number; key: string }[] {
  return [
    { node, depth, key: path },
    ...node.dependencies.flatMap((child, i) => rows(child, depth + 1, `${path}/${i}-${child.name}`)),
  ];
}

/** The dependency tree, one row per node, indented under what it depends on (Status board). */
export function HealthTree({ node }: { node: HealthNode }) {
  return (
    <ul className="deps">
      {rows(node, 0, node.name).map(({ node: n, depth, key }) => (
        <li key={key} style={{ paddingLeft: 6 + depth * 22, height: n.error ? "auto" : undefined, minHeight: 44 }}>
          <span>
            <span className="h2" style={{ fontSize: 16 }}>
              {n.name}
            </span>
            {n.error && (
              <span className="small" style={{ display: "block", color: "var(--red-text)" }}>
                {n.error}
              </span>
            )}
          </span>
          <span className="ms">{n.latencyMs} ms</span>
          <span>
            <span className={`tag ${TAG[n.status].cls}`}>{TAG[n.status].label}</span>
          </span>
        </li>
      ))}
    </ul>
  );
}
