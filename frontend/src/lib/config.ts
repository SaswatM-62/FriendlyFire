// Where the browser reaches the Gateway. NEXT_PUBLIC_* values come from .env and are
// built into the page by Next.js (see .env.example at the repo root). This is the only
// file that reads them.

/**
 * The fake backend (FF-513): MSW answers every REST call and a scripted room replaces
 * the WebSocket, so every screen works with nothing else running. Only in `next dev`
 * with NEXT_PUBLIC_FAKE_BACKEND=true; `next build` always sets NODE_ENV=production.
 */
const fakeBackend = process.env.NODE_ENV !== "production" && process.env.NEXT_PUBLIC_FAKE_BACKEND === "true";

export const config = {
  gatewayUrl: process.env.NEXT_PUBLIC_GATEWAY_URL ?? "http://localhost:8080",
  wsUrl: process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws",
  fakeBackend,
} as const;

/**
 * The fake backend's browser entry, or null. It's loaded synchronously so pages never
 * wait for it, and the condition is written out here (not as `config.fakeBackend`) so
 * that a production build folds it to `false` and leaves the mocks out of the bundle
 * entirely (`npm run check:no-fake-backend` checks this).
 */
export function loadFakeBackend(): typeof import("@/mocks/browser") | null {
  if (process.env.NODE_ENV !== "production" && process.env.NEXT_PUBLIC_FAKE_BACKEND === "true") {
    // eslint-disable-next-line @typescript-eslint/no-require-imports -- a static import can't be left out of production builds
    return require("@/mocks/browser") as typeof import("@/mocks/browser");
  }
  return null;
}
