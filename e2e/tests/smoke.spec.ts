import { expect, test } from "@playwright/test";

test("the status page shows every check passing and a live realtime connection", async ({ page }) => {
  await page.goto("/");

  // "N of N": every check passes, however many there are. The page refreshes every
  // 10 seconds, so give slow-starting services time.
  await expect(page.getByText(/^(\d+) of \1 checks passing$/)).toBeVisible({ timeout: 90_000 });
  await expect(page.getByText(/Realtime: connected · \d+ ms/)).toBeVisible({ timeout: 30_000 });
});
