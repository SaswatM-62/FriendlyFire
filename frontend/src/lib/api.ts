import type {
  ApiErrorBody,
  HistoryPage,
  LiveStats,
  Me,
  QuizSummary,
  RoomSnapshot,
  StartRiotLinkResponse,
  SyncStatusResponse,
} from "@/lib/api-types";
import { codeForStatus, ERROR_CODES, messageFor } from "@/lib/errors";

/** A failed Gateway call: the HTTP status (0 = no answer), a code and a message to show. */
export class ApiError extends Error {
  constructor(
    readonly status: number,
    readonly code: string,
    message: string,
  ) {
    super(message);
    this.name = "ApiError";
  }
}

export interface ApiClientOptions {
  baseUrl: string;
  /** The Auth0 access token for the Gateway, or null when signed out. */
  getToken: () => Promise<string | null>;
  /** Called for every failed call that isn't `quiet` (the app shows a toast). */
  onError?: (error: ApiError) => void;
  /** Defaults to the browser's fetch; the fake backend passes its own. */
  fetch?: typeof fetch;
}

interface RequestOptions {
  body?: unknown;
  query?: Record<string, string | number | undefined>;
  /** Don't report the error: the caller shows it in place (e.g. "Room not found"). */
  quiet?: boolean;
}

function isErrorBody(value: unknown): value is ApiErrorBody {
  if (typeof value !== "object" || value === null || !("error" in value)) return false;
  const error = (value as { error: unknown }).error;
  return typeof error === "object" && error !== null && typeof (error as { code: unknown }).code === "string";
}

async function toApiError(response: Response): Promise<ApiError> {
  let body: unknown = null;
  try {
    body = await response.json();
  } catch {
    // Not JSON (a proxy's HTML error page, an empty body...): use the status alone.
  }
  if (isErrorBody(body)) {
    return new ApiError(response.status, body.error.code, messageFor(body.error.code, body.error.message));
  }
  const code = codeForStatus(response.status);
  return new ApiError(response.status, code, messageFor(code));
}

/**
 * The typed Gateway client (FF-502). Adds the access token to every call and turns
 * every failure into an ApiError, which it also reports through `onError` unless the
 * call is `quiet`.
 */
export function createApiClient(options: ApiClientOptions) {
  const doFetch = options.fetch ?? ((input: RequestInfo | URL, init?: RequestInit) => fetch(input, init));

  async function request<T>(method: string, path: string, opts: RequestOptions = {}): Promise<T> {
    try {
      const url = new URL(path, options.baseUrl);
      for (const [key, value] of Object.entries(opts.query ?? {})) {
        if (value !== undefined && value !== "") url.searchParams.set(key, String(value));
      }

      let token: string | null;
      try {
        token = await options.getToken();
      } catch {
        throw new ApiError(401, ERROR_CODES.unauthenticated, messageFor(ERROR_CODES.unauthenticated));
      }

      const headers: Record<string, string> = { Accept: "application/json" };
      if (token) headers.Authorization = `Bearer ${token}`;
      if (opts.body !== undefined) headers["Content-Type"] = "application/json";

      let response: Response;
      try {
        response = await doFetch(url, {
          method,
          headers,
          body: opts.body === undefined ? undefined : JSON.stringify(opts.body),
          cache: "no-store",
        });
      } catch {
        throw new ApiError(0, ERROR_CODES.network, messageFor(ERROR_CODES.network));
      }

      if (!response.ok) throw await toApiError(response);
      if (response.status === 204) return undefined as T;
      return (await response.json()) as T;
    } catch (e) {
      const error = e instanceof ApiError ? e : new ApiError(0, "INTERNAL", messageFor("INTERNAL"));
      if (!opts.quiet) options.onError?.(error);
      throw error;
    }
  }

  return {
    /** The signed-in user; creates them on their first visit. */
    getMe: (opts?: RequestOptions) => request<Me>("GET", "/api/me", opts),
    updateDisplayName: (displayName: string, opts?: RequestOptions) =>
      request<Me>("PATCH", "/api/me", { ...opts, body: { displayName } }),
    deleteAccount: (opts?: RequestOptions) => request<void>("DELETE", "/api/me", opts),

    startRiotLink: (opts?: RequestOptions) => request<StartRiotLinkResponse>("POST", "/api/link/riot/start", opts),
    completeRiotLink: (code: string, state: string, opts?: RequestOptions) =>
      request<Me["riotLink"]>("POST", "/api/link/riot/complete", { ...opts, body: { code, state } }),
    unlinkRiot: (opts?: RequestOptions) => request<void>("DELETE", "/api/link/riot", opts),
    getSyncStatus: (opts?: RequestOptions) => request<SyncStatusResponse>("GET", "/api/link/riot/status", opts),

    createRoom: (opts?: RequestOptions) => request<RoomSnapshot>("POST", "/api/rooms", opts),
    joinRoom: (code: string, opts?: RequestOptions) =>
      request<RoomSnapshot>("POST", `/api/rooms/${encodeURIComponent(code)}/join`, opts),
    getRoom: (code: string, opts?: RequestOptions) =>
      request<RoomSnapshot>("GET", `/api/rooms/${encodeURIComponent(code)}`, opts),

    getHistory: (page: { pageSize?: number; pageToken?: string } = {}, opts?: RequestOptions) =>
      request<HistoryPage>("GET", "/api/history", { ...opts, query: page }),
    getQuiz: (quizId: string, opts?: RequestOptions) =>
      request<QuizSummary>("GET", `/api/quizzes/${encodeURIComponent(quizId)}`, opts),

    /** Planned: rooms playing and players online right now (Home's "Live now"). */
    getLiveStats: (opts?: RequestOptions) => request<LiveStats>("GET", "/api/stats/live", opts),
  };
}

export type ApiClient = ReturnType<typeof createApiClient>;

/** Room codes: 6 characters, no look-alikes (0/O, 1/I/L), upper case. */
export const ROOM_CODE_ALPHABET = "ABCDEFGHJKMNPQRSTUVWXYZ23456789";

/** Upper-cases and keeps only room-code characters: " k7qm-2x " -> "K7QM2X". */
export function normalizeRoomCode(raw: string): string {
  return [...raw.toUpperCase()].filter((c) => ROOM_CODE_ALPHABET.includes(c)).join("").slice(0, 6);
}

export function isRoomCode(value: string): boolean {
  return value.length === 6 && normalizeRoomCode(value) === value;
}
