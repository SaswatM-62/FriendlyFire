// Package sync runs background match syncs. (Import it with an alias, e.g. matchsync,
// next to the standard library's sync.)
package sync

import (
	"context"
	"errors"
	"log/slog"
	"math"
	"strings"
	gosync "sync"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/lookup"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/source"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
)

// Window is how far back a sync fetches; only these matches are kept.
const Window = 30 * 24 * time.Hour

// DefaultRequests is the guess for a sync's requests before any sync has finished.
const DefaultRequests = 6

// interrupted is shown for a sync that was running when the service stopped.
const interrupted = "The sync was interrupted. Sign in again to sync your matches."

// Store is what the worker needs from storage (store.MatchStore; a fake in tests).
type Store interface {
	StartSync(ctx context.Context, player store.Linked, now time.Time) error
	CompleteSync(ctx context.Context, userID string, records []stats.Record, pairs []store.PairUpdate, requests int, now time.Time) error
	FailSync(ctx context.Context, userID, message string, now time.Time) error
	FailInterrupted(ctx context.Context, message string, now time.Time) error
	LinkedPlayers(ctx context.Context, game string) (map[string]string, error)
	AverageRequests(ctx context.Context) (float64, error)
}

// Estimator says how long n source requests will take (ratelimit.Limiter.Estimate for
// HenrikDev).
type Estimator func(requests int) time.Duration

// Progress is a queued or running sync, for GetSyncStatus.
type Progress struct {
	// QueuePosition is how many syncs are ahead (0 once it's running).
	QueuePosition int
	Estimate      time.Duration
}

// SyncWorker fetches players' matches in the background, one sync at a time (HenrikDev's
// rate limit is shared), at most one queued or running sync per player.
type SyncWorker struct {
	source   source.MatchSource
	store    Store
	tables   lookup.Tables
	estimate Estimator
	now      func() time.Time
	log      *slog.Logger

	mu      gosync.Mutex
	queue   []store.Linked
	running *running
	wake    chan struct{}
}

type running struct {
	player store.Linked
	ctx    context.Context
	cancel context.CancelFunc
	done   chan struct{}
}

// New returns a worker; call Run to start it.
func New(src source.MatchSource, st Store, tables lookup.Tables, estimate Estimator, now func() time.Time, log *slog.Logger) *SyncWorker {
	return &SyncWorker{
		source: src, store: st, tables: tables, estimate: estimate, now: now, log: log,
		wake: make(chan struct{}, 1),
	}
}

// RecoverInterrupted marks syncs that were running when the service last stopped as
// failed. Call it once at startup, before the service takes calls.
func (w *SyncWorker) RecoverInterrupted(ctx context.Context) error {
	return w.store.FailInterrupted(ctx, interrupted, w.now())
}

// Run processes the queue until ctx is done.
func (w *SyncWorker) Run(ctx context.Context) error {
	for {
		job, ok := w.next(ctx)
		if !ok {
			select {
			case <-ctx.Done():
				return nil
			case <-w.wake:
				continue
			}
		}
		w.runJob(ctx, job)
		if ctx.Err() != nil {
			return nil
		}
	}
}

// Enqueue records a sync as in progress and queues it, unless one is already queued or
// running for the player.
func (w *SyncWorker) Enqueue(ctx context.Context, player store.Linked) error {
	w.mu.Lock()
	defer w.mu.Unlock()
	if w.queuedOrRunning(player.UserID) {
		return nil
	}
	if err := w.store.StartSync(ctx, player, w.now()); err != nil {
		return err
	}
	w.queue = append(w.queue, player)
	select {
	case w.wake <- struct{}{}:
	default:
	}
	return nil
}

// Progress says whether a sync is queued or running for the player, and how long it
// should take, including the syncs ahead of it.
func (w *SyncWorker) Progress(ctx context.Context, userID string) (Progress, bool) {
	w.mu.Lock()
	position := -1
	if w.running != nil && w.running.player.UserID == userID {
		position = 0
	}
	ahead := 0
	if w.running != nil {
		ahead = 1
	}
	for i, p := range w.queue {
		if p.UserID == userID {
			position = ahead + i
			break
		}
	}
	w.mu.Unlock()
	if position < 0 {
		return Progress{}, false
	}
	perSync, err := w.store.AverageRequests(ctx)
	if err != nil || perSync <= 0 {
		perSync = DefaultRequests
	}
	requests := int(math.Ceil(perSync * float64(position+1)))
	return Progress{QueuePosition: position, Estimate: w.estimate(requests)}, true
}

// Cancel drops a queued sync for the player, or stops a running one and waits for it to
// end, so nothing is written for them afterwards.
func (w *SyncWorker) Cancel(userID string) {
	w.mu.Lock()
	kept := w.queue[:0]
	for _, p := range w.queue {
		if p.UserID != userID {
			kept = append(kept, p)
		}
	}
	w.queue = kept
	var wait chan struct{}
	if w.running != nil && w.running.player.UserID == userID {
		w.running.cancel()
		wait = w.running.done
	}
	w.mu.Unlock()
	if wait != nil {
		<-wait
	}
}

func (w *SyncWorker) queuedOrRunning(userID string) bool {
	if w.running != nil && w.running.player.UserID == userID {
		return true
	}
	for _, p := range w.queue {
		if p.UserID == userID {
			return true
		}
	}
	return false
}

// next takes the first queued sync and makes it the running one, with its own context so
// Cancel can stop it.
func (w *SyncWorker) next(ctx context.Context) (*running, bool) {
	w.mu.Lock()
	defer w.mu.Unlock()
	if len(w.queue) == 0 {
		return nil, false
	}
	player := w.queue[0]
	w.queue = w.queue[1:]
	jobCtx, cancel := context.WithCancel(ctx)
	w.running = &running{player: player, ctx: jobCtx, cancel: cancel, done: make(chan struct{})}
	return w.running, true
}

func (w *SyncWorker) runJob(ctx context.Context, job *running) {
	jobCtx := job.ctx
	defer func() {
		job.cancel()
		w.mu.Lock()
		w.running = nil
		w.mu.Unlock()
		close(job.done)
	}()

	player := job.player
	started := w.now()
	err := w.sync(jobCtx, player)
	switch {
	case err == nil:
		w.log.Info("sync finished", "user_id", player.UserID, "took", w.now().Sub(started).Round(time.Millisecond))
	case jobCtx.Err() != nil:
		// Cancelled (the player unlinked) or shutting down: nothing to record.
		w.log.Info("sync stopped", "user_id", player.UserID)
	default:
		w.log.Warn("sync failed", "user_id", player.UserID, "err", err)
		if failErr := w.store.FailSync(ctx, player.UserID, source.SafeMessage(err), w.now()); failErr != nil {
			w.log.Error("couldn't record a failed sync", "user_id", player.UserID, "err", failErr)
		}
	}
}

// sync fetches the last 30 days, works out every record, and replaces the player's data.
// Friends' stored records of the same matches get their pair keys about this player.
func (w *SyncWorker) sync(ctx context.Context, player store.Linked) error {
	since := w.now().Add(-Window)
	result, err := w.source.Matches(ctx, source.Player{PUUID: player.ExternalPlayerID, Region: player.Region}, since)
	if err != nil {
		return err
	}
	linked, err := w.store.LinkedPlayers(ctx, player.Game)
	if err != nil {
		return &source.Error{Message: "Couldn't save your matches. Try again later.", Err: err}
	}
	linked[player.ExternalPlayerID] = player.UserID
	var mine []stats.Record
	var pairs []store.PairUpdate
	for _, m := range result.Matches {
		records, err := stats.Records(m, linked, w.tables)
		if err != nil {
			w.log.Warn("skipping a match that can't be read", "match", m.Metadata.MatchID, "err", err)
			continue
		}
		for _, r := range records {
			if r.PlayedAt.Before(since) {
				continue
			}
			if r.UserID == player.UserID {
				mine = append(mine, r)
				continue
			}
			if keys := pairKeysAbout(r.Numbers, player.UserID); len(keys) > 0 {
				pairs = append(pairs, store.PairUpdate{UserID: r.UserID, MatchID: r.MatchID, Numbers: keys})
			}
		}
	}
	if err := ctx.Err(); err != nil {
		return err
	}
	if err := w.store.CompleteSync(ctx, player.UserID, mine, pairs, result.Requests, w.now()); err != nil {
		if errors.Is(err, context.Canceled) {
			return err
		}
		return &source.Error{Message: "Couldn't save your matches. Try again later.", Err: err}
	}
	return nil
}

// pairKeysAbout are a friend's pair keys that name this user.
func pairKeysAbout(numbers map[string]float64, userID string) map[string]float64 {
	keys := map[string]float64{}
	for key, value := range numbers {
		for _, prefix := range []string{"assists_for:", "finishes_for:", "trades_for:"} {
			if strings.HasPrefix(key, prefix) && strings.TrimPrefix(key, prefix) == userID {
				keys[key] = value
			}
		}
	}
	return keys
}
