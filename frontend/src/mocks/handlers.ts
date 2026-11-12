import { http, HttpResponse, type RequestHandler } from "msw";
import type { ApiErrorBody, HistoryPage, LiveStats } from "@/lib/api-types";
import { config } from "@/lib/config";
import { ME_ID } from "@/mocks/data";
import { FULL_ROOM, STARTED_ROOM, type FakeGateway } from "@/mocks/gateway";

function fail(status: number, code: string, message: string): Response {
  return HttpResponse.json<ApiErrorBody>({ error: { code, message } }, { status });
}

/**
 * MSW handlers for every Gateway REST endpoint (FF-513), answering like the real
 * Gateway will: the same paths, bodies, status codes and error format. The browser's
 * fake backend and the Vitest tests both use them.
 */
export function createHandlers(gateway: FakeGateway, baseUrl: string = config.gatewayUrl): RequestHandler[] {
  const url = (path: string) => `${baseUrl}${path}`;
  const signedOut = () => fail(401, "UNAUTHENTICATED", "Sign in first.");

  return [
    // ---------- account ----------
    http.get(url("/api/me"), () => (gateway.signedIn ? HttpResponse.json(gateway.me()) : signedOut())),

    http.patch(url("/api/me"), async ({ request }) => {
      if (!gateway.signedIn) return signedOut();
      const { displayName } = (await request.json()) as { displayName?: string };
      const name = (displayName ?? "").trim();
      if (name.length < 1 || name.length > 24) {
        return fail(400, "INVALID_ARGUMENT", "Your name needs 1 to 24 characters.");
      }
      gateway.rename(name);
      return HttpResponse.json(gateway.me());
    }),

    http.delete(url("/api/me"), () => {
      if (!gateway.signedIn) return signedOut();
      gateway.deleteAccount();
      return new HttpResponse(null, { status: 204 });
    }),

    // ---------- Riot link ----------
    http.post(url("/api/link/riot/start"), () => {
      if (!gateway.signedIn) return signedOut();
      // The real one points at Riot Sign-On (or the mock RSO); the fake skips straight back.
      const origin = typeof window === "undefined" ? "http://localhost:3000" : window.location.origin;
      return HttpResponse.json({ authorizeUrl: `${origin}/link/riot/callback?code=fake-code&state=fake-state` });
    }),

    http.post(url("/api/link/riot/complete"), async ({ request }) => {
      if (!gateway.signedIn) return signedOut();
      const { code, state } = (await request.json()) as { code?: string; state?: string };
      if (state !== "fake-state") return fail(400, "INVALID_LINK_STATE", "That link request expired.");
      if (code === "taken") return fail(409, "RIOT_ALREADY_LINKED", "That Riot account is linked to someone else.");
      return HttpResponse.json(gateway.link());
    }),

    http.delete(url("/api/link/riot"), () => {
      if (!gateway.signedIn) return signedOut();
      gateway.unlink();
      return new HttpResponse(null, { status: 204 });
    }),

    http.get(url("/api/link/riot/status"), () => (gateway.signedIn ? HttpResponse.json(gateway.syncStatus()) : signedOut())),

    // ---------- rooms ----------
    http.post(url("/api/rooms"), () => {
      if (!gateway.signedIn) return signedOut();
      return HttpResponse.json(gateway.createRoom().snapshot, { status: 201 });
    }),

    http.post(url("/api/rooms/:code/join"), ({ params }) => {
      if (!gateway.signedIn) return signedOut();
      const code = String(params.code);
      if (code === FULL_ROOM) return fail(409, "ROOM_FULL", "That room is full.");
      if (code === STARTED_ROOM) return fail(409, "QUIZ_ALREADY_STARTED", "That quiz has started.");
      const room = gateway.room(code);
      if (!room) return fail(404, "ROOM_NOT_FOUND", "Room not found.");
      return HttpResponse.json(gateway.join(room));
    }),

    http.get(url("/api/rooms/:code"), ({ params }) => {
      if (!gateway.signedIn) return signedOut();
      const room = gateway.room(String(params.code));
      if (!room) return fail(404, "ROOM_NOT_FOUND", "Room not found.");
      if (!room.snapshot.players.some((p) => p.userId === ME_ID)) {
        return fail(403, "NOT_IN_ROOM", "You're not in this room.");
      }
      return HttpResponse.json(room.snapshot);
    }),

    // ---------- history ----------
    http.get(url("/api/history"), ({ request }) => {
      if (!gateway.signedIn) return signedOut();
      const search = new URL(request.url).searchParams;
      const size = Math.min(50, Number(search.get("pageSize")) || 20);
      const start = Number(search.get("pageToken") ?? "0") || 0;
      const quizzes = gateway.history.slice(start, start + size);
      const next = start + size < gateway.history.length ? String(start + size) : "";
      return HttpResponse.json({ quizzes, nextPageToken: next } satisfies HistoryPage);
    }),

    http.get(url("/api/quizzes/:quizId"), ({ params }) => {
      if (!gateway.signedIn) return signedOut();
      const quiz = gateway.history.find((q) => q.quizId === params.quizId);
      return quiz ? HttpResponse.json(quiz) : fail(404, "NOT_FOUND", "Quiz not found.");
    }),

    // ---------- planned: live numbers for Home ----------
    http.get(url("/api/stats/live"), () =>
      gateway.signedIn ? HttpResponse.json(gateway.liveStats() satisfies LiveStats) : signedOut(),
    ),
  ];
}
