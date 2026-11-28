import { act, fireEvent, render, screen, within } from "@testing-library/react";
import { HttpResponse, http } from "msw";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import { ApiProvider } from "@/components/providers/ApiProvider";
import { SoundProvider } from "@/components/providers/SoundProvider";
import { REFRESH_GRACE_MS, RoomLive } from "@/components/room/RoomLive";
import { ToastProvider } from "@/components/ui/Toasts";
import type { RoomTransportFactory } from "@/lib/realtime";
import { FAKE_PLAYERS, ME_ID } from "@/mocks/data";
import { FAKE_TIMING, FakeRoom, fakePlayer } from "@/mocks/fakeRoom";
import { server } from "@/test/server";

const p = FAKE_PLAYERS;
let transport: RoomTransportFactory | null = null;
// The room the Gateway answers with when the page loads it again over REST.
let current: FakeRoom | null = null;

vi.mock("@/components/providers/AppProviders", () => ({ useRoomTransport: () => transport }));

function hostedRoom() {
  current = new FakeRoom({
    code: "TEST22",
    hostUserId: ME_ID,
    meId: ME_ID,
    players: [fakePlayer(p.alex), fakePlayer(p.maya), fakePlayer(p.sam), fakePlayer(p.jordan)],
    settings: { questionCount: 2, secondsPerQuestion: 10 },
    random: () => 0,
  });
  return current;
}

function renderLive(room: FakeRoom, initial = room.snapshot) {
  return render(
    <ToastProvider>
      <ApiProvider>
        <SoundProvider>
          <RoomLive code="TEST22" meId={ME_ID} initial={initial} />
        </SoundProvider>
      </ApiProvider>
    </ToastProvider>,
  );
}

describe("RoomLive", () => {
  beforeEach(() => {
    vi.useFakeTimers();
    // No chat yet when the room opens.
    server.use(
      http.get("*/api/rooms/:code/chat", () => HttpResponse.json({ messages: [] })),
      http.get("*/api/rooms/:code", () => HttpResponse.json(current?.snapshot)),
    );
  });
  afterEach(() => {
    vi.useRealTimers();
    transport = null;
    current = null;
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

  it("loads the room's chat so far when it opens", async () => {
    const room = hostedRoom();
    transport = (_code, handlers) => room.connect(handlers);
    const history = {
      messages: [{ messageId: "m-old", userId: "gone-user", displayName: "Riley", text: "brb, grabbing food", sentAt: "2026-11-19T18:00:00Z" }],
    };
    const fetchImpl = vi.fn(async (input: RequestInfo | URL) =>
      String(input).endsWith("/api/rooms/TEST22/chat")
        ? new Response(JSON.stringify(history), { status: 200, headers: { "Content-Type": "application/json" } })
        : new Response(null, { status: 404 }),
    );
    render(
      <ToastProvider>
        <ApiProvider fetchImpl={fetchImpl as unknown as typeof fetch}>
          <SoundProvider>
            <RoomLive code="TEST22" meId={ME_ID} initial={room.snapshot} />
          </SoundProvider>
        </ApiProvider>
      </ToastProvider>,
    );
    await act(async () => vi.advanceTimersByTime(300));

    const chat = screen.getAllByRole("list", { name: "Chat messages" })[0];
    // Riley has left the room, so the name comes from the message.
    expect(within(chat).getByText("brb, grabbing food")).toBeInTheDocument();
    expect(within(chat).getByText("Riley")).toBeInTheDocument();
  });

  it("rejoins when it's connected but the room says it dropped (a refresh)", async () => {
    // The old tab's drop was reported after this page loaded: the room still has us DISCONNECTED.
    const room = hostedRoom();
    room.snapshot.players[0].status = "DISCONNECTED";
    transport = (_code, handlers) => room.connect(handlers);
    const back = { ...room.snapshot, players: room.snapshot.players.map((pl) => ({ ...pl, status: "CONNECTED" as const })) };
    const joins = vi.fn(() => HttpResponse.json(back));
    server.use(http.post("*/api/rooms/:code/join", joins));
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

    expect(joins).toHaveBeenCalledTimes(1);
  });

  it("doesn't rejoin when the room already has us connected", async () => {
    const room = hostedRoom();
    transport = (_code, handlers) => room.connect(handlers);
    const joins = vi.fn(() => HttpResponse.json(room.snapshot));
    server.use(http.post("*/api/rooms/:code/join", joins));
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

    expect(joins).not.toHaveBeenCalled();
  });

  it("shows who joined between the page's load and the live connection, once", async () => {
    const room = hostedRoom();
    const loaded = structuredClone(room.snapshot);
    // Riley joins before this page is subscribed, so their join event never arrives here.
    room.addPlayer(fakePlayer(p.riley));
    transport = (_code, handlers) => room.connect(handlers);
    render(
      <ToastProvider>
        <ApiProvider>
          <SoundProvider>
            <RoomLive code="TEST22" meId={ME_ID} initial={loaded} />
          </SoundProvider>
        </ApiProvider>
      </ToastProvider>,
    );
    await act(async () => vi.advanceTimersByTime(300));

    expect(screen.getByRole("region", { name: "Players" })).toHaveTextContent("Riley");
    const chat = screen.getByRole("region", { name: "Room chat" });
    expect(within(chat).getAllByText("Riley joined the room")).toHaveLength(1);
    // A join after the connection opened still gets its own line.
    await act(async () => room.addPlayer(fakePlayer(p.casey)));
    expect(within(chat).getAllByText("Casey joined the room")).toHaveLength(1);
    expect(within(chat).getAllByText("Riley joined the room")).toHaveLength(1);
  });

  it("doesn't announce a lobby refresh, but does announce a real drop", async () => {
    const room = hostedRoom();
    transport = (_code, handlers) => room.connect(handlers);
    renderLive(room);
    await act(async () => vi.advanceTimersByTime(300));
    const chat = screen.getByRole("region", { name: "Room chat" });

    // Maya refreshes: gone for 2 seconds, then back.
    await act(async () => room.dropPlayer(p.maya.userId));
    await act(async () => vi.advanceTimersByTime(2_000));
    await act(async () => room.addPlayer(fakePlayer(p.maya)));
    await act(async () => vi.advanceTimersByTime(REFRESH_GRACE_MS));
    expect(within(chat).queryByText("Maya disconnected")).not.toBeInTheDocument();
    expect(within(chat).queryByText("Maya is back")).not.toBeInTheDocument();

    // Sam really drops: the line shows once the grace is over, and so does the return.
    await act(async () => room.dropPlayer(p.sam.userId));
    expect(within(chat).queryByText("Sam disconnected")).not.toBeInTheDocument();
    await act(async () => vi.advanceTimersByTime(REFRESH_GRACE_MS + 100));
    expect(within(chat).getByText("Sam disconnected")).toBeInTheDocument();
    await act(async () => room.addPlayer(fakePlayer(p.sam)));
    expect(within(chat).getByText("Sam is back")).toBeInTheDocument();
  });

  it("welcomes you back when you return to a quiz you're playing in", async () => {
    const room = hostedRoom();
    await act(async () => room.handle("start", {}, ME_ID));
    await act(async () => vi.advanceTimersByTime(FAKE_TIMING.preparingMs + FAKE_TIMING.startingMs + 100));
    expect(room.snapshot.phase).toBe("QUESTION");
    transport = (_code, handlers) => room.connect(handlers);
    renderLive(room, structuredClone(room.snapshot));
    await act(async () => vi.advanceTimersByTime(300));

    const chat = screen.getByRole("region", { name: "Room chat" });
    expect(within(chat).getByText("You're back in the room")).toBeInTheDocument();
    expect(within(chat).queryByText("You joined the room")).not.toBeInTheDocument();
  });
});
