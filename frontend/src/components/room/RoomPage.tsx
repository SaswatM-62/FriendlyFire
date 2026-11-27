"use client";

import { useEffect, useRef, useState } from "react";
import { useApi } from "@/components/providers/ApiProvider";
import { useMe } from "@/components/providers/MeProvider";
import { useSession } from "@/components/providers/SessionProvider";
import { JoiningSkeleton, RoomMessage } from "@/components/room/RoomMessages";
import { RoomLive } from "@/components/room/RoomLive";
import { Icon } from "@/components/ui/Icon";
import { ApiError, isRoomCode } from "@/lib/api";
import type { RoomSnapshot } from "@/lib/api-types";
import { ERROR_CODES } from "@/lib/errors";
import { rememberInvite } from "@/lib/preferences";
import { takeCachedRoom } from "@/lib/roomCache";

type Load =
  | { state: "loading" }
  | { state: "ready"; snapshot: RoomSnapshot }
  | { state: "error"; error: ApiError };

/**
 * Loads the room, straight from the create/join call that brought you here, or by joining:
 * POST /api/rooms/{code}/join adds someone new (an invite link) and marks someone already
 * in it as back (a refresh), so the others don't keep seeing you as disconnected.
 *
 * Next.js keeps this page alive when you leave it (cacheComponents) and runs this again
 * when you come back. Each load has its own `visit`, so the room under it starts fresh
 * instead of showing what it saw before you left.
 */
function useRoomLoad(code: string, enabled: boolean, userId: string | null): { load: Load; visit: number } {
  const api = useApi();
  const [result, setResult] = useState<{ key: string; visit: number; load: Load } | null>(null);
  const visits = useRef(0);
  const key = enabled && userId ? `${code}:${userId}` : null;

  useEffect(() => {
    if (key === null) return;
    let cancelled = false;
    const visit = ++visits.current;
    const done = (load: Load) => {
      if (!cancelled) setResult({ key, visit, load });
    };
    const cached = takeCachedRoom(code);
    if (cached) {
      done({ state: "ready", snapshot: cached });
      return;
    }
    done({ state: "loading" });
    api.joinRoom(code, { quiet: true }).then(
      (snapshot) => done({ state: "ready", snapshot }),
      (e: unknown) => done({ state: "error", error: e instanceof ApiError ? e : new ApiError(0, "INTERNAL", "Something went wrong.") }),
    );
    return () => {
      cancelled = true;
    };
  }, [api, code, key]);

  return result && result.key === key ? { load: result.load, visit: result.visit } : { load: { state: "loading" }, visit: 0 };
}

/** /room/{code}: the whole room experience (FF-505 to FF-509). */
export function RoomPage({ rawCode }: { rawCode: string }) {
  const code = rawCode.toUpperCase();
  const valid = isRoomCode(code);
  const session = useSession();
  const { me, status: meStatus, error: meError, reload } = useMe();
  // A new player finishes the tutorial first (OnboardingGate keeps this room as their invite), so don't join yet.
  const onboarding = me !== null && !me.onboardedAt;
  const { load, visit } = useRoomLoad(code, valid && session.status === "signedIn" && !onboarding, me?.userId ?? null);

  // A signed-out player with an invite link: remember the room for after sign-in (FF-503).
  useEffect(() => {
    if (valid && session.status === "signedOut") rememberInvite(code);
  }, [valid, session.status, code]);

  if (!valid) {
    return (
      <RoomMessage icon="search" tone="yellow" title="Room not found">
        <p className="muted">Check the code and try again. Codes are 6 letters and numbers, like K7QM2X.</p>
      </RoomMessage>
    );
  }
  if (session.status === "signedOut") {
    return (
      <RoomMessage
        icon="link"
        tone="cyan"
        title={`Sign in to join room ${code}`}
        actions={
          <button type="button" className="btn btn--primary" onClick={session.signIn}>
            Sign in
            <Icon name="arrowRight" />
          </button>
        }
      >
        <p className="muted">You&apos;ve been invited to a FriendlyFire quiz. After you sign in, the home page offers to join this room.</p>
      </RoomMessage>
    );
  }
  if (meStatus === "error") {
    return (
      <RoomMessage
        icon="alert"
        tone="red"
        title="Couldn't load your account"
        actions={
          <button type="button" className="btn btn--primary" onClick={reload}>
            Try again
          </button>
        }
      >
        <p className="muted">{meError?.message ?? "Something went wrong."}</p>
      </RoomMessage>
    );
  }
  if (!me || load.state === "loading") return <JoiningSkeleton code={code} />;

  if (load.state === "error") {
    const e = load.error;
    if (e.status === 404) {
      return (
        <RoomMessage icon="search" tone="yellow" title="Room not found">
          <p className="muted">It may have closed, or the code has a typo. Codes are 6 letters and numbers.</p>
        </RoomMessage>
      );
    }
    if (e.code === ERROR_CODES.roomFull) {
      return (
        <RoomMessage icon="users" tone="orange" title="This room is full">
          <p className="muted">It already has 8 players. Ask the host to open a new room.</p>
        </RoomMessage>
      );
    }
    if (e.status === 409) {
      return (
        <RoomMessage icon="clock" tone="cyan" title="This quiz has already started">
          <p className="muted">You can join the next one when the host plays again.</p>
        </RoomMessage>
      );
    }
    return (
      <RoomMessage
        icon="alert"
        tone="red"
        title="Couldn't open the room"
        actions={
          <button type="button" className="btn btn--primary" onClick={() => window.location.reload()}>
            Try again
          </button>
        }
      >
        <p className="muted">{e.message}</p>
      </RoomMessage>
    );
  }

  return <RoomLive key={visit} code={code} meId={me.userId} initial={load.snapshot} />;
}
