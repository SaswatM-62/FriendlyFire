"use client";

import { Client } from "@stomp/stompjs";
import { useEffect, useState } from "react";
import { NetBars } from "@/components/ui/NetBars";
import { config } from "@/lib/config";
import { PING_EVERY_MS, QUALITY_LABEL, qualityFromRtt } from "@/lib/net";

/**
 * Pings the Gateway over STOMP (/app/ping -> /user/queue/pong) every 3 seconds and
 * shows the round trip and its quality, like the in-game network bar (Status board).
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

  const quality = connected && roundTripMs !== null ? qualityFromRtt(roundTripMs, 0) : null;

  return (
    <section className="card ping" aria-labelledby="h-ping">
      <h2 id="h-ping" className="label">
        Realtime ping
      </h2>
      {connected ? (
        <>
          <p className="num" style={{ fontSize: 64, lineHeight: 1 }} role="status">
            {roundTripMs ?? "–"}
            <span className="muted" style={{ fontSize: 24 }}>
              {" "}
              ms
            </span>
          </p>
          <span className="chip">
            <NetBars quality={quality} />
            {quality ? QUALITY_LABEL[quality] : "Measuring"}
          </span>
        </>
      ) : (
        <p className="h2" role="status" style={{ color: "var(--red-text)" }}>
          Disconnected
        </p>
      )}
      <p className="small faint">Round trip over the room WebSocket.</p>
    </section>
  );
}
