import { describe, expect, it } from "vitest";
import { summarize } from "@/lib/health";
import { healthyTree, treeWithScoringPostgresDown } from "@/test/healthTrees";

describe("summarize", () => {
  it("counts every node below the root as one check", () => {
    expect(summarize(healthyTree())).toEqual({ passing: 15, total: 15 });
  });

  it("only counts DOWN nodes as failing (a DEGRADED service still answered)", () => {
    expect(summarize(treeWithScoringPostgresDown())).toEqual({ passing: 14, total: 15 });
  });
});
