"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useState, useSyncExternalStore } from "react";
import { useMe } from "@/components/providers/MeProvider";
import { useSyncStatus } from "@/components/providers/SyncStatusProvider";
import { rememberInvite } from "@/lib/preferences";
import { getFakeBackend } from "@/mocks/browser";
import type { FakeScene } from "@/mocks/fakeRoom";
import { FULL_ROOM, GUEST_ROOM, STARTED_ROOM, type FakeSyncMode } from "@/mocks/gateway";

const SCENES: [FakeScene, string][] = [
  ["lobby", "Lobby"],
  ["countdown", "Countdown"],
  ["get-ready", "Get ready"],
  ["question", "Question"],
  ["hurry", "Last 5 s"],
  ["reveal", "Reveal"],
  ["vote", "Vote"],
  ["paused", "Paused"],
  ["podium", "Podium"],
  ["ended", "Ended early"],
  ["drop", "Drop my connection"],
  ["elsewhere", "Other tab"],
  ["closed", "Close room"],
  ["chat", "Bot chat"],
];

const SYNC: [FakeSyncMode, string][] = [
  ["synced", "Synced"],
  ["syncing", "Syncing"],
  ["stale", "Over 24 h old"],
  ["failed", "Failed"],
];

/**
 * The fake backend's control panel (FF-513, development only): sign in and out, link
 * or unlink Riot, change the sync, and jump the current room to any screen.
 */
export function DevPanel() {
  const fake = getFakeBackend();
  const gateway = fake.gateway;
  useSyncExternalStore(gateway.subscribe, () => gateway.version, () => 0);
  const [open, setOpen] = useState(false);
  const pathname = usePathname();
  const { reload } = useMe();
  const { refresh } = useSyncStatus();
  const roomCode = pathname.startsWith("/room/") ? pathname.split("/")[2] : null;
  const room = roomCode ? gateway.room(roomCode) : undefined;

  const after = (change: () => void) => {
    change();
    reload();
    refresh();
  };

  return (
    <div className="fixed right-4 bottom-4 z-[60] flex max-w-[calc(100vw-32px)] flex-col items-end gap-2">
      {open && (
        <div className="card flex max-h-[70vh] w-[340px] max-w-full flex-col gap-4 overflow-auto" style={{ padding: 16, boxShadow: "0 18px 48px rgb(0 0 0 / .35)" }}>
          <p className="label" style={{ color: "var(--orange-text)" }}>Fake backend</p>
          <section className="flex flex-col gap-2">
            <h2 className="label">Account</h2>
            <div className="row" style={{ gap: 8 }}>
              <button type="button" className="btn btn--sm" onClick={() => after(() => (gateway.signedIn ? gateway.signOut() : gateway.signIn()))}>
                {gateway.signedIn ? "Sign out" : "Sign in"}
              </button>
              <button type="button" className="btn btn--sm" onClick={() => after(() => gateway.setLinked(!gateway.linked))}>
                {gateway.linked ? "Unlink Riot" : "Link Riot"}
              </button>
            </div>
            <div className="row" style={{ gap: 8 }}>
              {SYNC.map(([mode, label]) => (
                <button
                  key={mode}
                  type="button"
                  className={`btn btn--sm${gateway.syncMode === mode ? " btn--mint" : ""}`}
                  aria-pressed={gateway.syncMode === mode}
                  onClick={() => after(() => gateway.setSync(mode))}
                >
                  {label}
                </button>
              ))}
            </div>
            <button
              type="button"
              className="btn btn--sm self-start"
              onClick={() =>
                after(() => {
                  rememberInvite(GUEST_ROOM);
                  gateway.signOut();
                })
              }
            >
              Open an invite while signed out
            </button>
          </section>
          <section className="flex flex-col gap-2">
            <h2 className="label">Rooms</h2>
            <p className="small faint">
              <Link href={`/room/${GUEST_ROOM}`} style={{ color: "var(--cyan-text)" }}>
                {GUEST_ROOM}
              </Link>{" "}
              is Maya&apos;s room. {FULL_ROOM} is full, {STARTED_ROOM} has started, any other code is not found.
            </p>
            {room ? (
              <div className="row" style={{ gap: 8 }}>
                {SCENES.map(([scene, label]) => (
                  <button key={scene} type="button" className="btn btn--sm" onClick={() => room.jumpTo(scene)}>
                    {label}
                  </button>
                ))}
              </div>
            ) : (
              <p className="small faint">Open a room to jump between its screens.</p>
            )}
          </section>
        </div>
      )}
      <button
        type="button"
        className="btn btn--sm"
        style={{ borderColor: "var(--orange)", color: "var(--orange-text)", background: "var(--surface)" }}
        aria-expanded={open}
        onClick={() => setOpen((o) => !o)}
      >
        {open ? "Close" : "Fake backend"}
      </button>
    </div>
  );
}
