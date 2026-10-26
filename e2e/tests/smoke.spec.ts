import { expect, test } from "@playwright/test";

test("the status page shows every check passing and a live realtime connection", async ({ page }) => {
  await page.goto("/");

  // The page refreshes every 10 seconds, so give slow-starting services time.
  await expect(page.getByText("17 of 17 checks passing")).toBeVisible({ timeout: 90_000 });
  await expect(page.getByText(/Realtime: connected · \d+ ms/)).toBeVisible({ timeout: 30_000 });
});
