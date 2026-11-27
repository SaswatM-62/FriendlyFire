import { waitFor } from "@testing-library/react";
import { describe, expect, it } from "vitest";
import { RoomPage } from "@/components/room/RoomPage";
import { takeCachedRoom } from "@/lib/roomCache";
import { renderWithApp } from "@/test/render";
import { server } from "@/test/server";

describe("RoomPage", () => {
  it("opens a room by rejoining it, so a refresh shows you as back, never by only reading it", async () => {
    takeCachedRoom("K7QM2X"); // an earlier test's join left it in the hand-off cache
    const seen: string[] = [];
    server.events.on("request:start", ({ request }) => {
      if (new URL(request.url).pathname.startsWith("/api/rooms/K7QM2X")) seen.push(`${request.method} ${new URL(request.url).pathname}`);
    });
    renderWithApp(<RoomPage rawCode="K7QM2X" />);

    await waitFor(() => expect(seen).toContain("POST /api/rooms/K7QM2X/join"));
    expect(seen).not.toContain("GET /api/rooms/K7QM2X");
    server.events.removeAllListeners("request:start");
  });
});
