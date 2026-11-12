"use client";

import { createContext, Suspense, useContext, useMemo, useState, type ReactNode } from "react";
import { ApiProvider } from "@/components/providers/ApiProvider";
import { MeProvider } from "@/components/providers/MeProvider";
import { SessionProvider, useSession } from "@/components/providers/SessionProvider";
import { SoundProvider } from "@/components/providers/SoundProvider";
import { SyncStatusProvider } from "@/components/providers/SyncStatusProvider";
import { ToastProvider } from "@/components/ui/Toasts";
import { config, loadFakeBackend } from "@/lib/config";
import { createStompTransport, type RoomTransportFactory } from "@/lib/realtime";
import type { FakeBackend } from "@/mocks/browser";

const RealtimeContext = createContext<RoomTransportFactory | null>(null);

/** Opens a room's live connection: STOMP to the Gateway, or the fake room. */
export function useRoomTransport(): RoomTransportFactory | null {
  return useContext(RealtimeContext);
}

function RealtimeProvider({ fake, children }: { fake: FakeBackend | null; children: ReactNode }) {
  const { getToken } = useSession();
  const factory = useMemo(
    () => fake?.roomTransport ?? createStompTransport({ url: config.wsUrl, getToken }),
    [fake, getToken],
  );
  return <RealtimeContext.Provider value={factory}>{children}</RealtimeContext.Provider>;
}

/** Everything the pages share: toasts, session, the API, the user, sync status, sound, realtime. */
export function AppProviders({ children }: { children: ReactNode }) {
  const [fake] = useState<FakeBackend | null>(() => loadFakeBackend()?.getFakeBackend() ?? null);
  const DevPanel = fake?.DevPanel;
  return (
    <ToastProvider>
      <SessionProvider fake={fake}>
        <ApiProvider fetchImpl={fake?.fetch}>
          <MeProvider>
            <SyncStatusProvider>
              <SoundProvider>
                <RealtimeProvider fake={fake}>
                  {children}
                  {DevPanel && (
                    <Suspense fallback={null}>
                      <DevPanel />
                    </Suspense>
                  )}
                </RealtimeProvider>
              </SoundProvider>
            </SyncStatusProvider>
          </MeProvider>
        </ApiProvider>
      </SessionProvider>
    </ToastProvider>
  );
}
