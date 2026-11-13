import { expect, test } from "@playwright/test";

test("the status page shows every check passing and a live realtime connection", async ({ page }) => {
  await page.goto("/status");

  // The page refreshes every 10 seconds, so give slow-starting services time.
  await expect(page.getByText(/17 of 17 checks passing/)).toBeVisible({ timeout: 90_000 });
  const ping = page.getByRole("region", { name: "Realtime ping" });
  await expect(ping).toContainText(/\d+\s*ms/, { timeout: 30_000 });
  await expect(ping).not.toContainText("Disconnected");
});

test("the home page explains FriendlyFire to signed-out visitors", async ({ page }) => {
  await page.goto("/");

  await expect(page.getByRole("heading", { name: "How well do you know your squad?" })).toBeVisible();
  await expect(page.getByRole("button", { name: /Sign in to play/ })).toBeVisible();
  await expect(page.getByText(/isn't endorsed by Riot Games/)).toBeVisible();
});

test("an invite link asks a signed-out visitor to sign in first", async ({ page }) => {
  await page.goto("/room/K7QM2X");

  await expect(page.getByRole("heading", { name: "Sign in to join room K7QM2X" })).toBeVisible();
});
