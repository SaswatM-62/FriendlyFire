import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiProvider } from "@/components/providers/ApiProvider";
import { SoundProvider } from "@/components/providers/SoundProvider";
import { RoomLive } from "@/components/room/RoomLive";
import { ToastProvider } from "@/components/ui/Toasts";
import type { RoomTransportFactory } from "@/lib/realtime";
import { FAKE_PLAYERS, ME_ID } from "@/mocks/data";
import { FAKE_TIMING, FakeRoom, fakePlayer } from "@/mocks/fakeRoom";

const p = FAKE_PLAYERS;
let transport: RoomTransportFactory | null = null;

vi.mock("@/components/providers/AppProviders", () => ({ useRoomTransport: () => transport }));

function hostedRoom() {
  return new FakeRoom({
    code: "TEST22",
    hostUserId: ME_ID,
    meId: ME_ID,
    players: [fakePlayer(p.alex), fakePlayer(p.maya), fakePlayer(p.sam), fakePlayer(p.jordan)],
    settings: { questionCount: 2, secondsPerQuestion: 10 },
    random: () => 0,
  });
}

describe("RoomLive", () => {
  beforeEach(() => vi.useFakeTimers());
  afterEach(() => {
    vi.useRealTimers();
    transport = null;
  });

  it("plays a whole quiz through to the final standings, with the chat alongside, then goes back to the lobby", async () => {
    const room = hostedRoom();
    transport = (_code, handlers) => room.connect(handlers);
    render(
      <ToastProvider>
        <ApiProvider>
          <SoundProvider>
            <RoomLive code="TEST22" meId={ME_ID} initial={room.snapshot} />
          </SoundProvider>
        </ApiProvider>
      </ToastProvider>,
    );
    await act(async () => vi.advanceTimersByTime(300));
    // The theme can change mid-game: the room bar has the toggle.
    expect(screen.getByRole("banner")).toContainElement(screen.getByRole("button", { name: /Switch to (light|dark) theme/ }));

    await act(async () => room.handle("start", {}, ME_ID));
    // Everyone sees the loading screen, with how a quiz works, before the countdown.
    expect(screen.getByRole("heading", { name: "Building your questions…" })).toBeInTheDocument();
    expect(screen.getByRole("heading", { name: "How a quiz works" })).toBeInTheDocument();
    await act(async () => vi.advanceTimersByTime(FAKE_TIMING.preparingMs));
    expect(room.snapshot.phase).toBe("STARTING");
    for (let i = 0; i < 2; i++) {
      await act(async () => vi.advanceTimersByTime(FAKE_TIMING.startingMs + 100));
      expect(room.snapshot.phase).toBe("QUESTION");
      await act(async () => vi.advanceTimersByTime(12_000));
      expect(room.snapshot.phase).toBe("REVEAL");
      await act(async () => vi.advanceTimersByTime(FAKE_TIMING.revealMs + 100));
    }

    expect(room.snapshot.phase).toBe("FINISHED");
    expect(screen.getByRole("heading", { name: /wins!|win!|tie!/ })).toBeInTheDocument();
    expect(screen.getByRole("region", { name: "Final standings" })).toBeInTheDocument();

    // The chat followed along: what happened, then what you say.
    const chat = screen.getByRole("region", { name: "Room chat" });
    expect(within(chat).getByText("You joined the room")).toBeInTheDocument();
    expect(within(chat).getByText("Question 2 started")).toBeInTheDocument();
    expect(within(chat).getAllByText("Time's up")).toHaveLength(2);
    fireEvent.change(within(chat).getByRole("textbox", { name: "Message" }), { target: { value: "rematch!" } });
    await act(async () => fireEvent.click(within(chat).getByRole("button", { name: "Send" })));
    expect(within(chat).getByText("rematch!")).toBeInTheDocument();

    // Back to lobby shows the lobby again; Start begins the next quiz.
    await act(async () => fireEvent.click(screen.getByRole("button", { name: "Back to lobby" })));
    expect(screen.getByRole("heading", { name: "Room code" })).toBeInTheDocument();
    await act(async () => fireEvent.click(screen.getByRole("button", { name: /Start quiz/ })));
    expect(room.snapshot.phase).toBe("PREPARING");
  });

  it("shows why in the lobby when the quiz couldn't start", async () => {
    const room = hostedRoom();
    transport = (_code, handlers) => room.connect(handlers);
    render(
      <ToastProvider>
        <ApiProvider>
          <SoundProvider>
            <RoomLive code="TEST22" meId={ME_ID} initial={room.snapshot} />
          </SoundProvider>
        </ApiProvider>
      </ToastProvider>,
    );
    await act(async () => vi.advanceTimersByTime(300));

    await act(async () => room.handle("start", {}, ME_ID));
    await act(async () => room.failStart("Not enough matches for 15 questions. Try 10."));

    expect(screen.getByRole("alert")).toHaveTextContent("The quiz couldn't start. Not enough matches for 15 questions. Try 10.");
    await act(async () => fireEvent.click(screen.getByRole("button", { name: "Dismiss" })));
    expect(screen.queryByText(/couldn't start/)).not.toBeInTheDocument();
  });
});
