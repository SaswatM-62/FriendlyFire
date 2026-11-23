import "@testing-library/jest-dom/vitest";
import { cleanup } from "@testing-library/react";
import { afterAll, afterEach, beforeAll, vi } from "vitest";
import { location, router } from "@/test/navigation";
import { server } from "@/test/server";

vi.mock("next/navigation", async () => {
  const nav = await import("@/test/navigation");
  return {
    useRouter: () => nav.router,
    usePathname: () => nav.location.pathname,
    useSearchParams: () => nav.location.search,
  };
});

beforeAll(() => server.listen({ onUnhandledFrame: "error" }));
afterEach(() => {
  cleanup();
  server.resetHandlers();
  Object.values(router).forEach((fn) => fn.mockReset());
  location.pathname = "/";
  location.search = new URLSearchParams();
  window.localStorage.clear();
});
afterAll(() => server.close());
