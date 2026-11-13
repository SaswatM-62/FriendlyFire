"use client";

import { createContext, useContext, useMemo, type ReactNode } from "react";
import { useSession } from "@/components/providers/SessionProvider";
import { useToast } from "@/components/ui/Toasts";
import { createApiClient, type ApiClient } from "@/lib/api";
import { config } from "@/lib/config";

const ApiContext = createContext<ApiClient | null>(null);

/** The Gateway client, with the player's token and errors shown as toasts. */
export function useApi(): ApiClient {
  const api = useContext(ApiContext);
  if (!api) throw new Error("useApi needs an ApiProvider");
  return api;
}

export function ApiProvider({ fetchImpl, children }: { fetchImpl?: typeof fetch; children: ReactNode }) {
  const { getToken } = useSession();
  const toast = useToast();
  const api = useMemo(
    () =>
      createApiClient({
        baseUrl: config.gatewayUrl,
        getToken,
        fetch: fetchImpl,
        onError: (error) => toast(error.message, "red"),
      }),
    [getToken, fetchImpl, toast],
  );
  return <ApiContext.Provider value={api}>{children}</ApiContext.Provider>;
}
