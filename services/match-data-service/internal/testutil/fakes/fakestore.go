// Package fakes holds fakes for match-data-service's own interfaces (store, sync worker,
// match source). Prefer these over mocking libraries.
package fakes

import (
	"context"
	"errors"
	"sort"
	"strings"
	"sync"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
)

// FakeStore is store.MatchStore in memory.
type FakeStore struct {
	mu       sync.Mutex
	players  map[string]store.Linked
	statuses map[string]store.Status
	records  map[string][]stats.Record
	requests map[string]int
	// FailComplete makes CompleteSync fail (and keep the old data).
	FailComplete bool
	Completed    []string
	Failed       []string
}

// NewFakeStore returns an empty store.
func NewFakeStore() *FakeStore {
	return &FakeStore{
		players: map[string]store.Linked{}, statuses: map[string]store.Status{},
		records: map[string][]stats.Record{}, requests: map[string]int{},
	}
}

func (f *FakeStore) StartSync(_ context.Context, player store.Linked, _ time.Time) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.players[player.UserID] = player
	st := f.statuses[player.UserID]
	st.UserID, st.Status, st.ErrorMessage = player.UserID, store.InProgress, ""
	f.statuses[player.UserID] = st
	return nil
}

func (f *FakeStore) CompleteSync(_ context.Context, userID string, records []stats.Record, pairs []store.PairUpdate, requests int, now time.Time) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	if f.FailComplete {
		return errors.New("disk full")
	}
	if _, ok := f.players[userID]; !ok {
		return errors.New("player no longer linked")
	}
	f.records[userID] = append([]stats.Record(nil), records...)
	for _, p := range pairs {
		for i, r := range f.records[p.UserID] {
			if r.MatchID == p.MatchID {
				for k, v := range p.Numbers {
					f.records[p.UserID][i].Numbers[k] = v
				}
			}
		}
	}
	f.requests[userID] = requests
	t := now
	f.statuses[userID] = store.Status{UserID: userID, Status: store.Complete, LastSyncedAt: &t}
	f.Completed = append(f.Completed, userID)
	return nil
}

func (f *FakeStore) FailSync(_ context.Context, userID, message string, _ time.Time) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	st := f.statuses[userID]
	st.Status, st.ErrorMessage = store.Failed, message
	f.statuses[userID] = st
	f.Failed = append(f.Failed, userID)
	return nil
}

func (f *FakeStore) FailInterrupted(_ context.Context, message string, _ time.Time) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	for id, st := range f.statuses {
		if st.Status == store.InProgress {
			st.Status, st.ErrorMessage = store.Failed, message
			f.statuses[id] = st
		}
	}
	return nil
}

func (f *FakeStore) LinkedPlayers(_ context.Context, game string) (map[string]string, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	linked := map[string]string{}
	for _, p := range f.players {
		if p.Game == game {
			linked[p.ExternalPlayerID] = p.UserID
		}
	}
	return linked, nil
}

func (f *FakeStore) AverageRequests(context.Context) (float64, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	if len(f.requests) == 0 {
		return 0, nil
	}
	total := 0
	for _, n := range f.requests {
		total += n
	}
	return float64(total) / float64(len(f.requests)), nil
}

func (f *FakeStore) Statuses(_ context.Context, userIDs []string) (map[string]store.Status, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	out := map[string]store.Status{}
	for _, id := range userIDs {
		if st, ok := f.statuses[id]; ok {
			st.MatchesStored = len(f.records[id])
			out[id] = st
		}
	}
	return out, nil
}

func (f *FakeStore) Stats(_ context.Context, userIDs []string, since time.Time, perPlayer int) ([]stats.Record, error) {
	f.mu.Lock()
	defer f.mu.Unlock()
	var out []stats.Record
	for _, id := range userIDs {
		mine := append([]stats.Record(nil), f.records[id]...)
		sort.SliceStable(mine, func(i, j int) bool { return mine[i].PlayedAt.After(mine[j].PlayedAt) })
		n := 0
		for _, r := range mine {
			if (!since.IsZero() && r.PlayedAt.Before(since)) || n >= perPlayer {
				continue
			}
			out = append(out, r)
			n++
		}
	}
	sort.SliceStable(out, func(i, j int) bool { return out[i].PlayedAt.After(out[j].PlayedAt) })
	return out, nil
}

func (f *FakeStore) Delete(_ context.Context, userID string) error {
	f.mu.Lock()
	defer f.mu.Unlock()
	delete(f.players, userID)
	delete(f.statuses, userID)
	delete(f.records, userID)
	delete(f.requests, userID)
	for id, records := range f.records {
		for _, r := range records {
			for key := range r.Numbers {
				if strings.HasSuffix(key, ":"+userID) {
					delete(r.Numbers, key)
				}
			}
		}
		f.records[id] = records
	}
	return nil
}

// Records are a player's stored records.
func (f *FakeStore) Records(userID string) []stats.Record {
	f.mu.Lock()
	defer f.mu.Unlock()
	return append([]stats.Record(nil), f.records[userID]...)
}

// Status is a player's stored status.
func (f *FakeStore) Status(userID string) store.Status {
	f.mu.Lock()
	defer f.mu.Unlock()
	return f.statuses[userID]
}

// Put stores records for a player directly, as a finished sync would.
func (f *FakeStore) Put(player store.Linked, records []stats.Record, now time.Time) {
	f.mu.Lock()
	defer f.mu.Unlock()
	f.players[player.UserID] = player
	f.records[player.UserID] = records
	t := now
	f.statuses[player.UserID] = store.Status{UserID: player.UserID, Status: store.Complete, LastSyncedAt: &t}
}
