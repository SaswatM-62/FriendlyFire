import type { HealthNode } from "@/lib/health";
import { StatusBadge } from "@/components/StatusBadge";

/** The dependency tree, one indented row per node. */
export function HealthTree({ node }: { node: HealthNode }) {
  return (
    <li>
      <div className="flex flex-wrap items-center gap-2 py-1">
        <span className="font-medium">{node.name}</span>
        <StatusBadge status={node.status} />
        <span className="text-sm text-gray-500">{node.latencyMs} ms</span>
        {node.error && <span className="text-sm text-red-700 dark:text-red-300">{node.error}</span>}
      </div>
      {node.dependencies.length > 0 && (
        <ul className="ml-6 border-l border-gray-300 pl-4 dark:border-gray-700">
          {node.dependencies.map((child, index) => (
            <HealthTree key={`${child.name}-${index}`} node={child} />
          ))}
        </ul>
      )}
    </li>
  );
}
