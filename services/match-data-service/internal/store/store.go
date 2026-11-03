// Package store keeps matches and stats in the `matchdata` schema.
package store

// MatchStore reads and writes the `matchdata` tables.
type MatchStore struct {
	// TODO(ff): matches and per-player stats in the `matchdata` schema
	//   - save a synced match once, with one stats row per linked player in it
	//   - query stats per player, newest first, since a time, capped per player
	//   - remember each player's sync status and newest stored match
	//   - delete everything for a player
	//   - uses: the *pgxpool.Pool from main (tables come from a new migration in internal/migrations/sql)
}
