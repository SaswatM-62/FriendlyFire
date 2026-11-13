import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { vi } from "vitest";
import { ApiProvider } from "@/components/providers/ApiProvider";
import { MeProvider } from "@/components/providers/MeProvider";
import { SessionContext, type Session } from "@/components/providers/SessionProvider";
import { SoundProvider } from "@/components/providers/SoundProvider";
import { SyncStatusProvider } from "@/components/providers/SyncStatusProvider";
import { ToastProvider } from "@/components/ui/Toasts";
import { FakeGateway } from "@/mocks/gateway";
import { createHandlers } from "@/mocks/handlers";
import { server } from "@/test/server";

export function signedInSession(): Session {
  return { status: "signedIn", email: "alex@example.com", getToken: async () => "test-token", signIn: vi.fn(), signOut: vi.fn() };
}

/**
 * Renders with the app's providers against the fake Gateway (the same MSW handlers as
 * the fake backend). Returns the gateway, so a test can change its state.
 */
export function renderWithApp(ui: ReactElement, options: { session?: Session; gateway?: FakeGateway } = {}) {
  const gateway = options.gateway ?? new FakeGateway();
  server.use(...createHandlers(gateway));
  const session = options.session ?? signedInSession();
  const result = render(
    <ToastProvider>
      <SessionContext.Provider value={session}>
        <ApiProvider>
          <MeProvider>
            <SyncStatusProvider>
              <SoundProvider>{ui}</SoundProvider>
            </SyncStatusProvider>
          </MeProvider>
        </ApiProvider>
      </SessionContext.Provider>
    </ToastProvider>,
  );
  return { ...result, gateway, session };
}
