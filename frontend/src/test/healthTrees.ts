import type { HealthNode, HealthStatus } from "@/lib/health";

function node(name: string, dependencies: HealthNode[] = [], status: HealthStatus = "UP", error?: string): HealthNode {
  return { name, status, latencyMs: 3, error, dependencies };
}

const postgres = () => node("postgres");
const matchData = () => node("match-data-service", [postgres()]);

/** The tree the Gateway returns when all 17 checks pass. */
export function healthyTree(): HealthNode {
  return node("gateway-service", [
    node("account-service", [postgres(), node("mock-rso-service"), matchData()]),
    node("room-service", [node("quiz-engine-service", [matchData()]), node("scoring-service", [postgres()]), matchData()]),
    node("scoring-service", [postgres()]),
    matchData(),
  ]);
}

/** The same tree with scoring-service's Postgres DOWN: 16 of 17 checks pass. */
export function treeWithScoringPostgresDown(): HealthNode {
  const tree = healthyTree();
  tree.status = "DEGRADED";
  const scoring = tree.dependencies[2];
  scoring.status = "DEGRADED";
  scoring.dependencies[0] = node("postgres", [], "DOWN", "connection refused");
  return tree;
}
