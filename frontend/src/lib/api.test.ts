import { http, HttpResponse } from "msw";
import { describe, expect, it, vi } from "vitest";
import { ApiError, createApiClient, isRoomCode, normalizeRoomCode } from "@/lib/api";
import { server } from "@/test/server";

const baseUrl = "http://gateway.test";

function client(overrides: Partial<Parameters<typeof createApiClient>[0]> = {}) {
  const onError = vi.fn();
  const api = createApiClient({ baseUrl, getToken: async () => "token-123", onError, ...overrides });
  return { api, onError };
}

async function failure(promise: Promise<unknown>): Promise<ApiError> {
  try {
    await promise;
  } catch (e) {
    if (e instanceof ApiError) return e;
    throw e;
  }
  throw new Error("expected the call to fail");
}

describe("createApiClient", () => {
  it("sends the access token on every call", async () => {
    let auth: string | null = null;
    server.use(
      http.get(`${baseUrl}/api/me`, ({ request }) => {
        auth = request.headers.get("Authorization");
        return HttpResponse.json({ userId: "u1", displayName: "Alex", email: "a@example.com", createdAt: "2026-11-01T00:00:00Z" });
      }),
    );
    const { api } = client();

    const me = await api.getMe();

    expect(auth).toBe("Bearer token-123");
    expect(me.displayName).toBe("Alex");
  });

  it("turns the standard error body into an ApiError and reports it", async () => {
    server.use(
      http.post(`${baseUrl}/api/rooms/PACKED/join`, () =>
        HttpResponse.json({ error: { code: "ROOM_FULL", message: "Room PACKED is full." } }, { status: 409 }),
      ),
    );
    const { api, onError } = client();

    const error = await failure(api.joinRoom("PACKED"));

    expect(error).toMatchObject({ status: 409, code: "ROOM_FULL", message: "That room is full (8 players)." });
    expect(onError).toHaveBeenCalledWith(error);
  });

  it("shows the Gateway's own message for codes it doesn't know", async () => {
    server.use(
      http.patch(`${baseUrl}/api/me`, () =>
        HttpResponse.json({ error: { code: "INVALID_ARGUMENT", message: "Names can't be longer than 24 characters." } }, { status: 400 }),
      ),
    );
    const { api } = client();

    const error = await failure(api.updateDisplayName("x".repeat(30)));

    expect(error.message).toBe("Names can't be longer than 24 characters.");
  });

  it("doesn't report quiet calls, so the page can show the error itself", async () => {
    server.use(http.post(`${baseUrl}/api/rooms/ZZZZ22/join`, () => HttpResponse.json({ error: { code: "ROOM_NOT_FOUND", message: "x" } }, { status: 404 })));
    const { api, onError } = client();

    const error = await failure(api.joinRoom("ZZZZ22", { quiet: true }));

    expect(error.status).toBe(404);
    expect(onError).not.toHaveBeenCalled();
  });

  it("understands today's 501 stubs, whose body isn't the standard format", async () => {
    server.use(http.get(`${baseUrl}/api/history`, () => HttpResponse.json({ error: "not implemented yet" }, { status: 501 })));
    const { api } = client();

    const error = await failure(api.getHistory());

    expect(error).toMatchObject({ status: 501, code: "NOT_IMPLEMENTED", message: "That part of FriendlyFire isn't ready yet." });
  });

  it("handles an error page that isn't JSON at all", async () => {
    server.use(http.get(`${baseUrl}/api/me`, () => new HttpResponse("<html>Bad gateway</html>", { status: 502 })));
    const { api } = client();

    const error = await failure(api.getMe());

    expect(error).toMatchObject({ status: 502, code: "INTERNAL" });
  });

  it("reports a network failure as NETWORK", async () => {
    server.use(http.get(`${baseUrl}/api/me`, () => HttpResponse.error()));
    const { api, onError } = client();

    const error = await failure(api.getMe());

    expect(error).toMatchObject({ status: 0, code: "NETWORK" });
    expect(onError).toHaveBeenCalledOnce();
  });

  it("reports a missing or expired sign-in as UNAUTHENTICATED without calling the Gateway", async () => {
    const { api } = client({
      getToken: async () => {
        throw new Error("no session");
      },
    });

    const error = await failure(api.getMe());

    expect(error).toMatchObject({ status: 401, code: "UNAUTHENTICATED" });
  });

  it("returns nothing for 204 and passes paging parameters", async () => {
    let query = "";
    server.use(
      http.delete(`${baseUrl}/api/link/riot`, () => new HttpResponse(null, { status: 204 })),
      http.get(`${baseUrl}/api/history`, ({ request }) => {
        query = new URL(request.url).search;
        return HttpResponse.json({ quizzes: [], nextPageToken: "" });
      }),
    );
    const { api } = client();

    await expect(api.unlinkRiot()).resolves.toBeUndefined();
    await api.getHistory({ pageSize: 10, pageToken: "abc" });

    expect(query).toBe("?pageSize=10&pageToken=abc");
  });
});

describe("room codes", () => {
  it("upper-cases and drops anything that can't be in a code", () => {
    expect(normalizeRoomCode(" k7qm-2x ")).toBe("K7QM2X");
    expect(normalizeRoomCode("K7QM2XEXTRA")).toBe("K7QM2X");
    // No look-alikes: 0/O and 1/I/L never appear in codes.
    expect(normalizeRoomCode("O0I1L")).toBe("");
  });

  it("knows a valid code", () => {
    expect(isRoomCode("K7QM2X")).toBe(true);
    expect(isRoomCode("K7QM2")).toBe(false);
    expect(isRoomCode("k7qm2x")).toBe(false);
  });
});
