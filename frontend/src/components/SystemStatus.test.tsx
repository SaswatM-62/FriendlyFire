import { render, screen } from "@testing-library/react";
import userEvent from "@testing-library/user-event";
import { http, HttpResponse } from "msw";
import { describe, expect, it } from "vitest";
import { SystemStatus } from "@/components/SystemStatus";
import { config } from "@/lib/config";
import { healthyTree, treeWithScoringPostgresDown } from "@/test/healthTrees";
import { server } from "@/test/server";

const deepHealth = `${config.gatewayUrl}/api/health/deep`;

describe("SystemStatus", () => {
  it("shows how many checks pass and the error of the one that's down", async () => {
    server.use(http.get(deepHealth, () => HttpResponse.json(treeWithScoringPostgresDown())));

    render(<SystemStatus />);

    expect(await screen.findByText("16 of 17 checks passing")).toBeInTheDocument();
    expect(screen.getByText("connection refused")).toBeInTheDocument();
    expect(screen.getAllByText("DOWN")).toHaveLength(1);
  });

  it("fetches again when Refresh is clicked", async () => {
    server.use(http.get(deepHealth, () => HttpResponse.json(treeWithScoringPostgresDown()), { once: true }));
    render(<SystemStatus />);
    expect(await screen.findByText("16 of 17 checks passing")).toBeInTheDocument();

    server.use(http.get(deepHealth, () => HttpResponse.json(healthyTree())));
    await userEvent.click(screen.getByRole("button", { name: "Refresh" }));

    expect(await screen.findByText("17 of 17 checks passing")).toBeInTheDocument();
  });

  it("says so when the Gateway can't be reached", async () => {
    server.use(http.get(deepHealth, () => HttpResponse.error()));

    render(<SystemStatus />);

    expect(await screen.findByText(/Can't reach the Gateway/)).toBeInTheDocument();
  });
});
