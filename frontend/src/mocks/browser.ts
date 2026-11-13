import { getResponse } from "msw";
import type { ComponentType } from "react";
import type { RoomTransportFactory } from "@/lib/realtime";
import { FAKE_BACKEND_MARKER, FAKE_ME } from "@/mocks/data";
import { DevPanel } from "@/mocks/DevPanel";
import { FakeGateway } from "@/mocks/gateway";
import { createHandlers } from "@/mocks/handlers";

export interface FakeAuthState {
  signedIn: boolean;
  email: string;
}

export interface FakeBackend {
  marker: string;
  gateway: FakeGateway;
  /** Answers Gateway calls with the MSW handlers, in the page (no service worker). */
  fetch: typeof fetch;
  /** Replaces the STOMP connection with the scripted room. */
  roomTransport: RoomTransportFactory;
  auth: {
    subscribe: (listener: () => void) => () => void;
    getState: () => FakeAuthState;
    signIn: () => void;
    signOut: () => void;
  };
  DevPanel: ComponentType;
}

/** A bit of latency, so loading states show up. */
const LATENCY_MS = 180;

function createFakeBackend(): FakeBackend {
  const gateway = new FakeGateway({ persist: true });
  const handlers = createHandlers(gateway);

  const fakeFetch: typeof fetch = async (input, init) => {
    const request = new Request(input, init);
    await new Promise((resolve) => setTimeout(resolve, LATENCY_MS));
    const response = await getResponse(handlers, request);
    return (
      response ??
      Response.json({ error: { code: "NOT_FOUND", message: `The fake backend has no ${request.method} ${request.url}` } }, { status: 404 })
    );
  };

  let authState: FakeAuthState = { signedIn: gateway.signedIn, email: FAKE_ME.email };
  const authListeners = new Set<() => void>();
  gateway.subscribe(() => {
    if (authState.signedIn !== gateway.signedIn) {
      authState = { signedIn: gateway.signedIn, email: FAKE_ME.email };
      for (const listener of authListeners) listener();
    }
  });

  return {
    marker: FAKE_BACKEND_MARKER,
    gateway,
    fetch: fakeFetch,
    roomTransport: (code, transportHandlers) => {
      const room = gateway.room(code);
      if (!room) {
        transportHandlers.onState("connecting");
        return { send: () => {}, close: () => {} };
      }
      return room.connect(transportHandlers);
    },
    auth: {
      subscribe: (listener) => {
        authListeners.add(listener);
        return () => authListeners.delete(listener);
      },
      getState: () => authState,
      signIn: () => gateway.signIn(),
      signOut: () => gateway.signOut(),
    },
    DevPanel,
  };
}

let instance: FakeBackend | null = null;

/** One fake backend per page, shared by every provider and the dev panel. */
export function getFakeBackend(): FakeBackend {
  instance ??= createFakeBackend();
  return instance;
}
