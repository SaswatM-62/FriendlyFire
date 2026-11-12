import { render } from "@testing-library/react";
import type { ReactElement } from "react";
import { vi } from "vitest";
import { SoundProvider } from "@/components/providers/SoundProvider";
import { RoomContext, type RoomView } from "@/components/room/RoomContext";
import { ToastProvider } from "@/components/ui/Toasts";
import type { Player, RoomSnapshot } from "@/lib/api-types";
import { displayNames, playerLooks } from "@/lib/players";
import { FAKE_PLAYERS, ME_ID } from "@/mocks/data";
import { fakePlayer } from "@/mocks/fakeRoom";

const p = FAKE_PLAYERS;

/** A lobby with you (Alex), Maya, Sam and Jordan, everyone ready. */
export function lobbySnapshot(overrides: Partial<RoomSnapshot> = {}, players?: Player[]): RoomSnapshot {
  return {
    roomCode: "K7QM2X",
    hostUserId: ME_ID,
    phase: "LOBBY",
    settings: { questionCount: 10, secondsPerQuestion: 20 },
    players: players ?? [fakePlayer(p.alex), fakePlayer(p.maya), fakePlayer(p.sam), fakePlayer(p.jordan)],
    roundNumber: 0,
    scoreboard: [],
    ...overrides,
  };
}

/** Renders a room screen for `snapshot`; `send` records the actions it sends. */
export function renderRoom(ui: ReactElement, snapshot: RoomSnapshot, meId = ME_ID) {
  const send = vi.fn();
  const askLeave = vi.fn();
  const view: RoomView = {
    snapshot,
    meId,
    isHost: snapshot.hostUserId === meId,
    looks: playerLooks(snapshot.players),
    names: displayNames(snapshot.players),
    clockOffsetMs: 0,
    send,
    askLeave,
  };
  const result = render(
    <ToastProvider>
      <SoundProvider>
        <RoomContext.Provider value={view}>{ui}</RoomContext.Provider>
      </SoundProvider>
    </ToastProvider>,
  );
  const rerenderWith = (next: RoomSnapshot, extra: ReactElement = ui) =>
    result.rerender(
      <ToastProvider>
        <SoundProvider>
          <RoomContext.Provider value={{ ...view, snapshot: next, looks: playerLooks(next.players), names: displayNames(next.players) }}>
            {extra}
          </RoomContext.Provider>
        </SoundProvider>
      </ToastProvider>,
    );
  return { ...result, send, askLeave, rerenderWith };
}
