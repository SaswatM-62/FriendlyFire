package fakes

import (
	"context"
	"sync"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
	matchsync "github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/sync"
)

// FakeSyncer records what the server asks of the sync worker.
type FakeSyncer struct {
	mu        sync.Mutex
	Queued    []store.Linked
	Cancelled []string
	// Running gives a player a queued or running sync, for GetSyncStatus.
	Running map[string]matchsync.Progress
}

// NewFakeSyncer returns a syncer with nothing running.
func NewFakeSyncer() *FakeSyncer {
	return &FakeSyncer{Running: map[string]matchsync.Progress{}}
}

func (f *FakeSyncer) Enqueue(_ context.Context, player store.Linked) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.Queued = append(f.Queued, player)
	f.Running[player.UserID] = matchsync.Progress{Estimate: 12 * time.Second}
	return nil
}

func (f *FakeSyncer) Progress(_ context.Context, userID string) (matchsync.Progress, bool) {
	f.mu.Lock()
	defer f.mu.Unlock()
	progress, ok := f.Running[userID]
	return progress, ok
}

func (f *FakeSyncer) Cancel(userID string) {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.Cancelled = append(f.Cancelled, userID)
	delete(f.Running, userID)
}
