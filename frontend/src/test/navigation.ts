import { vi } from "vitest";

/** The Next.js router as tests see it (next/navigation is mocked in setup.ts). */
export const router = {
  push: vi.fn(),
  replace: vi.fn(),
  back: vi.fn(),
  forward: vi.fn(),
  refresh: vi.fn(),
  prefetch: vi.fn(),
};

/** The current address; tests set these before rendering. */
export const location = {
  pathname: "/",
  search: new URLSearchParams(),
};
