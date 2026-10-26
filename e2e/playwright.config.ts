import { defineConfig, devices } from "@playwright/test";

// End-to-end tests against the running stack (`make up`, then `make e2e`).
export default defineConfig({
  testDir: "./tests",
  timeout: 120_000,
  retries: process.env.CI ? 1 : 0,
  reporter: [["list"], ["html", { open: "never" }]],
  // Inside Docker (make e2e), forward localhost:3000/8080 to the stack first.
  globalSetup: process.env.E2E_FORWARD ? "./forward-ports.ts" : undefined,
  use: {
    baseURL: process.env.E2E_BASE_URL ?? "http://localhost:3000",
    trace: "retain-on-failure",
    screenshot: "only-on-failure",
  },
  projects: [{ name: "chromium", use: { ...devices["Desktop Chrome"] } }],
});
