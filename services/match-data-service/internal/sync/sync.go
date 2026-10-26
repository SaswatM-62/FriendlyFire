// Package sync runs background match syncs. (Import it with an alias, e.g. matchsync,
// next to the standard library's sync.)
package sync

// SyncWorker fetches players' matches in the background.
type SyncWorker struct {
	// TODO(ff): run match syncs in the background
	//   - one sync at a time per player; SyncPlayer while one is running does nothing
	//   - first sync: the last 30 days; then only matches newer than the newest stored
	//   - record IN_PROGRESS / COMPLETE / FAILED with a safe-to-show error
	//   - stop cleanly on shutdown (tests check for leaked goroutines with goleak)
	//   - uses: source.MatchSource, store.MatchStore, ratelimit
}
