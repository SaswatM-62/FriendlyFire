// TODO(ff): the room page (lobby, questions, reveal, votes, final standings)
//   - read roomCode from the route; load the room with GET /api/rooms/{code}, joining
//     first (POST /api/rooms/{code}/join) if the player isn't in it yet
//   - subscribe to /topic/room/{code} over STOMP; every event carries a full snapshot,
//     so replace the state each time
//   - send actions to /app/room/{code}/leave, /kick, /settings, /start, /answer,
//     /vote and /connection-quality
//   - a network bar from the /app/ping round trips (see RealtimeStatus)
export default function RoomPage() {
  return (
    <main className="p-8">
      <h1 className="text-2xl font-bold">Room</h1>
      <p>Coming soon.</p>
    </main>
  );
}
