import { ROOM_CODE_ALPHABET } from "@/lib/api";
import type { Me, QuizSummary, RiotLink, SyncStatusResponse, LiveStats } from "@/lib/api-types";
import { FAKE_ME, FAKE_PLAYERS, fakeHistory, ME_ID } from "@/mocks/data";
import { FakeRoom, fakePlayer } from "@/mocks/fakeRoom";

export type FakeSyncMode = "synced" | "syncing" | "stale" | "failed";

/** What survives a full page load (linking Riot navigates away and back). */
interface Persisted {
  signedIn: boolean;
  displayName: string;
  linked: boolean;
  syncMode: FakeSyncMode;
  syncStartedAt: number;
  createdRooms: string[];
}

const STORAGE_KEY = "ff-fake-backend";
/** A fake sync takes this long. */
const SYNC_SECONDS = 150;

/** Maya's room: always open, you join it as a guest (LobbyGuest board). */
export const GUEST_ROOM = "K7QM2X";
/** Codes that fail like the real Gateway would. Any other unknown code is "Room not found". */
export const FULL_ROOM = "PACKED";
export const STARTED_ROOM = "BUSY22";

/**
 * The fake backend's server side (FF-513): the signed-in user, their Riot link and sync,
 * rooms and history. The MSW handlers (handlers.ts) and the dev panel both use it.
 */
export class FakeGateway {
  private state: Persisted;
  private readonly persist: boolean;
  private listeners = new Set<() => void>();
  private rooms = new Map<string, FakeRoom>();
  readonly history: QuizSummary[];
  /** Bumped on every change, for useSyncExternalStore. */
  version = 0;

  constructor(options: { persist?: boolean; signedIn?: boolean; linked?: boolean } = {}) {
    this.persist = options.persist ?? false;
    const defaults: Persisted = {
      signedIn: options.signedIn ?? true,
      displayName: FAKE_ME.displayName,
      linked: options.linked ?? true,
      syncMode: "synced",
      syncStartedAt: 0,
      createdRooms: [],
    };
    this.state = { ...defaults, ...(this.persist ? this.load() : {}) };
    this.history = fakeHistory(Date.now());
  }

  private load(): Partial<Persisted> {
    try {
      return JSON.parse(window.sessionStorage.getItem(STORAGE_KEY) ?? "{}") as Partial<Persisted>;
    } catch {
      return {};
    }
  }

  private change(patch: Partial<Persisted> = {}) {
    this.state = { ...this.state, ...patch };
    if (this.persist) {
      try {
        window.sessionStorage.setItem(STORAGE_KEY, JSON.stringify(this.state));
      } catch {
        // Not kept across reloads; fine for a fake.
      }
    }
    this.version += 1;
    for (const listener of this.listeners) listener();
  }

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  // ---------- account ----------

  get signedIn() {
    return this.state.signedIn;
  }

  get linked() {
    return this.state.linked;
  }

  get syncMode() {
    return this.state.syncMode;
  }

  signIn() {
    this.change({ signedIn: true });
  }

  signOut() {
    this.change({ signedIn: false });
  }

  me(): Me {
    return {
      ...FAKE_ME,
      displayName: this.state.displayName,
      riotLink: this.state.linked ? FAKE_ME.riotLink : null,
    };
  }

  rename(displayName: string) {
    this.change({ displayName });
  }

  deleteAccount() {
    this.change({ signedIn: false, displayName: FAKE_ME.displayName, linked: false, syncMode: "synced" });
  }

  link(): RiotLink {
    this.change({ linked: true, syncMode: "syncing", syncStartedAt: Date.now() });
    return FAKE_ME.riotLink!;
  }

  unlink() {
    this.change({ linked: false });
  }

  setSync(mode: FakeSyncMode) {
    this.change({ syncMode: mode, syncStartedAt: Date.now() });
  }

  setLinked(linked: boolean) {
    this.change({ linked, syncMode: linked ? "synced" : this.state.syncMode });
  }

  /** Planned "Live now" numbers: a believable crowd plus the rooms made this session. */
  liveStats(): LiveStats {
    const playing = [...this.rooms.values()].filter((r) => r.snapshot.phase !== "LOBBY").length;
    return { roomsPlaying: 23 + playing, playersOnline: 98 + this.rooms.size * 5 };
  }

  syncStatus(): SyncStatusResponse {
    if (!this.state.linked) return { status: "NOT_LINKED" };
    const lastSyncedAt = new Date(Date.now() - 3 * 60 * 60 * 1000).toISOString();
    switch (this.state.syncMode) {
      case "syncing": {
        const left = SYNC_SECONDS - Math.floor((Date.now() - this.state.syncStartedAt) / 1000);
        if (left > 0) return { status: "SYNCING", estimatedSeconds: left, matchesStored: 0 };
        this.change({ syncMode: "synced" });
        return { status: "SYNCED", lastSyncedAt: new Date().toISOString(), matchesStored: 41 };
      }
      case "stale":
        return { status: "NOT_SYNCED", lastSyncedAt: new Date(Date.now() - 30 * 60 * 60 * 1000).toISOString(), matchesStored: 39 };
      case "failed":
        return { status: "FAILED", lastSyncedAt, matchesStored: 41, errorMessage: "HenrikDev didn't answer. We'll try again." };
      case "synced":
        return { status: "SYNCED", lastSyncedAt, matchesStored: 41 };
    }
  }

  // ---------- rooms ----------

  /** A room by code: Maya's room and the rooms you made this session exist. */
  room(code: string): FakeRoom | undefined {
    const existing = this.rooms.get(code);
    if (existing) return existing;
    if (code === GUEST_ROOM) return this.store(this.makeGuestRoom());
    if (this.state.createdRooms.includes(code)) return this.store(this.makeHostRoom(code));
    return undefined;
  }

  createRoom(): FakeRoom {
    let code = "";
    do {
      code = Array.from({ length: 6 }, () => ROOM_CODE_ALPHABET[Math.floor(Math.random() * ROOM_CODE_ALPHABET.length)]).join("");
    } while (code === GUEST_ROOM || this.rooms.has(code));
    this.change({ createdRooms: [...this.state.createdRooms, code] });
    return this.store(this.makeHostRoom(code));
  }

  private store(room: FakeRoom) {
    this.rooms.set(room.snapshot.roomCode, room);
    return room;
  }

  private meAsPlayer() {
    return fakePlayer({ ...FAKE_PLAYERS.alex, displayName: this.state.displayName }, {
      dataStatus: this.state.linked ? (this.state.syncMode === "synced" ? "SYNCED" : "SYNCING") : "NOT_LINKED",
      dataEstimatedSeconds: this.state.syncMode === "syncing" ? SYNC_SECONDS : undefined,
    });
  }

  /** You host: your squad arrives over the first few seconds and one sync finishes. */
  private makeHostRoom(code: string) {
    const p = FAKE_PLAYERS;
    return new FakeRoom({
      code,
      hostUserId: ME_ID,
      meId: ME_ID,
      players: [this.meAsPlayer()],
      arrivals: [
        { afterMs: 1_500, run: (r) => r.addPlayer(fakePlayer(p.maya, { dataStatus: "SYNCING", dataEstimatedSeconds: 170 })) },
        { afterMs: 3_000, run: (r) => r.addPlayer(fakePlayer(p.sam)) },
        { afterMs: 4_500, run: (r) => r.addPlayer(fakePlayer(p.jordan, { noRecentMatches: true })) },
        { afterMs: 6_000, run: (r) => r.addPlayer(fakePlayer(p.riley, { connectionQuality: "FAIR" })) },
        { afterMs: 9_000, run: (r) => r.setDataStatus(p.maya.userId, "SYNCING", 60) },
        { afterMs: 14_000, run: (r) => r.setDataStatus(p.maya.userId, "SYNCED") },
      ],
    });
  }

  /** Maya hosts and starts once everyone's ready. */
  private makeGuestRoom() {
    const p = FAKE_PLAYERS;
    return new FakeRoom({
      code: GUEST_ROOM,
      hostUserId: p.maya.userId,
      meId: ME_ID,
      players: [
        fakePlayer(p.maya),
        fakePlayer(p.sam, { dataStatus: "SYNCING", dataEstimatedSeconds: 130 }),
        fakePlayer(p.jordan, { noRecentMatches: true }),
        fakePlayer(p.riley, { connectionQuality: "FAIR" }),
      ],
      arrivals: [{ afterMs: 10_000, run: (r) => r.setDataStatus(p.sam.userId, "SYNCED") }],
      botHostStartsAfterMs: 8_000,
    });
  }

  /** POST /api/rooms/{code}/join: you're added (or marked connected again). */
  join(room: FakeRoom) {
    room.addPlayer(this.meAsPlayer());
    return room.snapshot;
  }
}
