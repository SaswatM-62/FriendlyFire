import { Client, type IFrame, type IMessage } from "@stomp/stompjs";
import type { Pong, RealtimeError, RoomAction, RoomActions, RoomEvent } from "@/lib/api-types";
import { ERROR_CODES } from "@/lib/errors";
import { backoffMs, PING_EVERY_MS } from "@/lib/net";

/**
 * connecting: first connection · open: live · reconnecting: dropped, retrying with
 * backoff · elsewhere: refused because the player is connected in another tab or
 * device (ADR-017), no more retries · closed: we closed it.
 */
export type ConnectionState = "connecting" | "open" | "reconnecting" | "elsewhere" | "closed";

export interface PongSample {
  sentAt: number;
  receivedAt: number;
  serverTime?: number;
}

export interface RoomTransportHandlers {
  onEvent(event: RoomEvent): void;
  onError(error: RealtimeError): void;
  onState(state: ConnectionState): void;
  onPong(sample: PongSample): void;
  /** A ping went unanswered until the next one was due. */
  onMissedPing(): void;
}

/** One room's live connection: the real STOMP one, or the fake backend's scripted room. */
export interface RoomTransport {
  send<A extends RoomAction>(action: A, body: RoomActions[A]): void;
  close(): void;
}

export type RoomTransportFactory = (roomCode: string, handlers: RoomTransportHandlers) => RoomTransport;

function parse<T>(message: IMessage): T | null {
  try {
    return JSON.parse(message.body) as T;
  } catch {
    return null;
  }
}

function isAlreadyConnected(frame: IFrame): boolean {
  return `${frame.headers.message ?? ""} ${frame.body}`.includes(ERROR_CODES.alreadyConnected);
}

/**
 * The STOMP connection to the Gateway (/ws). Sends the access token when connecting
 * (fetching a fresh one before every attempt), subscribes to the room topic, the
 * player's errors and pongs, pings every 3 seconds, and reconnects with backoff.
 */
export function createStompTransport(options: {
  url: string;
  getToken: () => Promise<string | null>;
}): RoomTransportFactory {
  return (roomCode, handlers) => {
    let attempt = 0;
    let stopped = false;
    let pingTimer: ReturnType<typeof setInterval> | undefined;
    let outstanding: number | null = null;

    const client = new Client({
      brokerURL: options.url,
      reconnectDelay: backoffMs(0),
      heartbeatIncoming: 10_000,
      heartbeatOutgoing: 10_000,
      beforeConnect: async () => {
        const token = await options.getToken().catch(() => null);
        client.connectHeaders = token ? { Authorization: `Bearer ${token}` } : {};
      },
    });

    const stopPinging = () => {
      clearInterval(pingTimer);
      outstanding = null;
    };

    client.onConnect = () => {
      attempt = 0;
      client.reconnectDelay = backoffMs(0);
      handlers.onState("open");
      client.subscribe(`/topic/room/${roomCode}`, (message) => {
        const event = parse<RoomEvent>(message);
        if (event) handlers.onEvent(event);
      });
      client.subscribe("/user/queue/errors", (message) => {
        const error = parse<RealtimeError>(message);
        if (error) handlers.onError(error);
      });
      client.subscribe("/user/queue/pong", (message) => {
        const pong = parse<Pong>(message);
        if (!pong) return;
        if (outstanding === pong.t) outstanding = null;
        handlers.onPong({ sentAt: pong.t, receivedAt: Date.now(), serverTime: pong.serverTime });
      });
      const ping = () => {
        if (outstanding !== null) handlers.onMissedPing();
        outstanding = Date.now();
        client.publish({ destination: "/app/ping", body: JSON.stringify({ t: outstanding }) });
      };
      ping();
      pingTimer = setInterval(ping, PING_EVERY_MS);
    };

    client.onStompError = (frame) => {
      if (isAlreadyConnected(frame)) {
        stopped = true;
        stopPinging();
        handlers.onState("elsewhere");
        void client.deactivate();
      }
    };

    client.onWebSocketClose = () => {
      stopPinging();
      if (stopped) return;
      attempt += 1;
      client.reconnectDelay = backoffMs(attempt);
      handlers.onState("reconnecting");
    };

    handlers.onState("connecting");
    client.activate();

    return {
      send(action, body) {
        if (!client.connected) return;
        client.publish({ destination: `/app/room/${roomCode}/${action}`, body: JSON.stringify(body) });
      },
      close() {
        stopped = true;
        stopPinging();
        handlers.onState("closed");
        void client.deactivate();
      },
    };
  };
}
