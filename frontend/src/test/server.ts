import { setupServer } from "msw/node";

/**
 * Fake Gateway for tests (MSW). It has no handlers by default: each test adds the
 * responses it needs with server.use(...), and any other request fails the test.
 */
export const server = setupServer();
