"use client";

import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { config } from "@/lib/config";

const PING_EVERY_MS = 3_000;

/**
 * Pings the Gateway over STOMP (/app/ping -> /user/queue/pong) every 3 seconds and
 * shows the round trip. The same pings will drive the in-game network bar.
 */
export function RealtimeStatus() {
  const [connected, setConnected] = useState(false);
  const [roundTripMs, setRoundTripMs] = useState<number | null>(null);

  useEffect(() => {
    let timer: ReturnType<typeof setInterval> | undefined;
    const client = new Client({ brokerURL: config.wsUrl, reconnectDelay: PING_EVERY_MS });

    client.onConnect = () => {
      setConnected(true);
      client.subscribe("/user/queue/pong", (message) => {
        const { t } = JSON.parse(message.body) as { t: number };
        setRoundTripMs(Date.now() - t);
      });
      const ping = () => client.publish({ destination: "/app/ping", body: JSON.stringify({ t: Date.now() }) });
      ping();
      timer = setInterval(ping, PING_EVERY_MS);
    };
    client.onWebSocketClose = () => {
      clearInterval(timer);
      setConnected(false);
      setRoundTripMs(null);
    };

    client.activate();
    return () => {
      clearInterval(timer);
      void client.deactivate();
    };
  }, []);

  return (
    <p className="text-lg">
      Realtime:{" "}
      {connected ? (
        <span className="text-green-700 dark:text-green-300">
          connected{roundTripMs !== null && ` · ${roundTripMs} ms`}
        </span>
      ) : (
        <span className="text-red-700 dark:text-red-300">disconnected</span>
      )}
    </p>
  );
}
