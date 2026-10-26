// Where the browser reaches the Gateway. NEXT_PUBLIC_* values come from .env and are
// built into the page by Next.js (see .env.example at the repo root).
export const config = {
  gatewayUrl: process.env.NEXT_PUBLIC_GATEWAY_URL ?? "http://localhost:8080",
  wsUrl: process.env.NEXT_PUBLIC_WS_URL ?? "ws://localhost:8080/ws",
} as const;
