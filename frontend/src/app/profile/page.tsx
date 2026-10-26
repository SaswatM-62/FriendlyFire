// TODO(ff): the profile page
//   - show the display name (edit it with PATCH /api/me) and the linked Riot ID
//   - "Link Riot account": POST /api/link/riot/start, then send the browser to the
//     returned authorizeUrl; unlink with DELETE /api/link/riot
//   - the "syncing your matches" status: GET /api/link/riot/status (poll while
//     IN_PROGRESS)
//   - delete the account: DELETE /api/me, after a confirmation
//   - create a room (POST /api/rooms) or join one by code (POST /api/rooms/{code}/join)
export default function ProfilePage() {
  return (
    <main className="p-8">
      <h1 className="text-2xl font-bold">Profile</h1>
      <p>Coming soon.</p>
    </main>
  );
}
