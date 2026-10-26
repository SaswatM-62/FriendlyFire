import { config } from "@/lib/config";

export type HealthStatus = "UP" | "DEGRADED" | "DOWN";

/** One node of GET /api/health/deep. */
export interface HealthNode {
  name: string;
  status: HealthStatus;
  latencyMs: number;
  error?: string;
  dependencies: HealthNode[];
}

/**
 * Every node below the root is one check. A check passes unless that node is DOWN
 * (a DEGRADED service answered; something below it didn't).
 */
export function summarize(root: HealthNode): { passing: number; total: number } {
  let passing = 0;
  let total = 0;
  const visit = (node: HealthNode) => {
    for (const child of node.dependencies) {
      total += 1;
      if (child.status !== "DOWN") passing += 1;
      visit(child);
    }
  };
  visit(root);
  return { passing, total };
}

export async function fetchDeepHealth(signal?: AbortSignal): Promise<HealthNode> {
  const response = await fetch(`${config.gatewayUrl}/api/health/deep`, { cache: "no-store", signal });
  if (!response.ok) {
    throw new Error(`Gateway answered HTTP ${response.status}`);
  }
  return (await response.json()) as HealthNode;
}
