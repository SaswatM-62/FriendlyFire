"use client";

import { useCallback, useEffect, useRef, useState } from "react";
import type { ConnectionQuality, RealtimeError, RoomAction, RoomActions, RoomEvent, RoomSnapshot } from "@/lib/api-types";
import { ClockOffset } from "@/lib/clock";
import { qualityFromRtt } from "@/lib/net";
import type { ConnectionState, RoomTransport, RoomTransportFactory } from "@/lib/realtime";

export interface RoomSocket {
  /** The latest snapshot: every event replaces it, nothing is merged. */
  snapshot: RoomSnapshot | null;
  connection: ConnectionState;
  quality: ConnectionQuality | null;
  rttMs: number | null;
  /** Add to Date.now() for the server's time. */
  clockOffsetMs: number;
  send: <A extends RoomAction>(action: A, body: RoomActions[A]) => void;
}

interface Options {
  /** Open the connection only once the player is in the room. */
  enabled: boolean;
  transport: RoomTransportFactory | null;
  /** The snapshot from the REST join or load, shown until the first event arrives. */
  initialSnapshot: RoomSnapshot | null;
  /** For effects only (toasts, sounds), after the snapshot is replaced. */
  onEvent?: (event: RoomEvent, previous: RoomSnapshot | null) => void;
  onError?: (error: RealtimeError) => void;
  /** After a dropped connection comes back: rejoin the room over REST (FF-309). */
  onReconnected?: () => void;
}

/**
 * The live room (FF-502): connects, subscribes, sends actions, measures the round trip
 * for the network bar and the clock offset for timers, reports quality changes to the
 * room (FF-310), and rejoins after a reconnect.
 */
export function useRoomSocket(roomCode: string, options: Options): RoomSocket {
  const { enabled, transport, initialSnapshot } = options;
  const [snapshot, setSnapshot] = useState<RoomSnapshot | null>(initialSnapshot);
  const [seenInitial, setSeenInitial] = useState<RoomSnapshot | null>(initialSnapshot);
  const [connection, setConnection] = useState<ConnectionState>("connecting");
  const [rttMs, setRttMs] = useState<number | null>(null);
  const [missed, setMissed] = useState(0);
  const [clockOffsetMs, setClockOffsetMs] = useState(0);

  // A new snapshot from REST (first load, rejoin) replaces the state too.
  if (initialSnapshot !== seenInitial) {
    setSeenInitial(initialSnapshot);
    if (initialSnapshot) setSnapshot(initialSnapshot);
  }

  const transportRef = useRef<RoomTransport | null>(null);
  const callbacks = useRef(options);
  useEffect(() => {
    callbacks.current = options;
  });

  useEffect(() => {
    if (!enabled || !transport) return;
    const clock = new ClockOffset();
    let wasOpen = false;
    let current: RoomSnapshot | null = null;
    const t = transport(roomCode, {
      onEvent(event) {
        const previous = current;
        current = event.snapshot;
        setSnapshot(event.snapshot);
        callbacks.current.onEvent?.(event, previous);
      },
      onError(error) {
        callbacks.current.onError?.(error);
      },
      onState(state) {
        setConnection(state);
        if (state === "open") {
          if (wasOpen) callbacks.current.onReconnected?.();
          wasOpen = true;
        }
        if (state !== "open") setRttMs(null);
      },
      onPong(sample) {
        setMissed(0);
        setRttMs(sample.receivedAt - sample.sentAt);
        if (sample.serverTime !== undefined) {
          clock.addSample(sample.sentAt, sample.receivedAt, sample.serverTime);
          setClockOffsetMs(clock.offsetMs);
        }
      },
      onMissedPing() {
        setMissed((m) => m + 1);
      },
    });
    transportRef.current = t;
    return () => {
      transportRef.current = null;
      t.close();
    };
  }, [enabled, transport, roomCode]);

  // Unknown until the first pong (or the first missed ping).
  const quality = connection === "open" && (rttMs !== null || missed > 0) ? qualityFromRtt(rttMs, missed) : null;

  // Tell the room when our quality level changes (not on every ping).
  const reported = useRef<ConnectionQuality | null>(null);
  useEffect(() => {
    if (quality && quality !== reported.current && transportRef.current) {
      reported.current = quality;
      transportRef.current.send("connection-quality", { quality });
    }
  }, [quality]);

  const send = useCallback(<A extends RoomAction>(action: A, body: RoomActions[A]) => {
    transportRef.current?.send(action, body);
  }, []);

  return { snapshot, connection, quality, rttMs, clockOffsetMs, send };
}
