"use client";

import { getAccessToken, useUser } from "@auth0/nextjs-auth0";
import { createContext, useCallback, useContext, useMemo, useSyncExternalStore, type ReactNode } from "react";
import type { FakeBackend } from "@/mocks/browser";

export interface Session {
  status: "loading" | "signedOut" | "signedIn";
  email: string | null;
  /** The Gateway access token, fetched fresh when needed. Never stored in the browser. */
  getToken: () => Promise<string | null>;
  /** Auth0's sign-in page. Sign-in always comes back to the home page (FF-102). */
  signIn: () => void;
  signOut: () => void;
}

const signedOut: Session = {
  status: "signedOut",
  email: null,
  getToken: async () => null,
  signIn: () => {},
  signOut: () => {},
};

/** Exported for tests, which provide a session directly. */
export const SessionContext = createContext<Session>(signedOut);

export function useSession(): Session {
  return useContext(SessionContext);
}

// Stable across renders, so the API client isn't rebuilt every time the session updates.
const auth0Token = () => getAccessToken();
// /auth/* are the Auth0 SDK's routes in src/proxy.ts, not Next.js pages: they need a full navigation.
// eslint-disable-next-line @next/next/no-location-assign-relative-destination
const auth0SignIn = () => window.location.assign("/auth/login");
// eslint-disable-next-line @next/next/no-location-assign-relative-destination
const auth0SignOut = () => window.location.assign("/auth/logout");

/** Sign-in through Auth0 (FF-102): the SDK keeps the session in an HttpOnly cookie. */
function Auth0Session({ children }: { children: ReactNode }) {
  const { user, isLoading } = useUser();
  const session = useMemo<Session>(
    () => ({
      status: isLoading ? "loading" : user ? "signedIn" : "signedOut",
      email: user?.email ?? null,
      getToken: auth0Token,
      signIn: auth0SignIn,
      signOut: auth0SignOut,
    }),
    [user, isLoading],
  );
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>;
}

/** The fake backend's pretend sign-in (FF-513): no Auth0 needed. */
function FakeSession({ fake, children }: { fake: FakeBackend; children: ReactNode }) {
  // The server doesn't know the fake sign-in (it lives in this tab), so it renders "loading".
  const state = useSyncExternalStore(fake.auth.subscribe, fake.auth.getState, () => null);
  const getToken = useCallback(async () => (fake.auth.getState().signedIn ? "fake-access-token" : null), [fake]);
  const session = useMemo<Session>(
    () => ({
      status: state === null ? "loading" : state.signedIn ? "signedIn" : "signedOut",
      email: state?.signedIn ? state.email : null,
      getToken,
      signIn: fake.auth.signIn,
      signOut: fake.auth.signOut,
    }),
    [state, fake, getToken],
  );
  return <SessionContext.Provider value={session}>{children}</SessionContext.Provider>;
}

export function SessionProvider({ fake, children }: { fake: FakeBackend | null; children: ReactNode }) {
  return fake ? <FakeSession fake={fake}>{children}</FakeSession> : <Auth0Session>{children}</Auth0Session>;
}
