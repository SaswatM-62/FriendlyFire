package sync_test

import (
	"context"
	"io"
	"log/slog"
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/lookup"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/source"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
	matchsync "github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/sync"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil/fakes"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
)

func TestMain(m *testing.M) {
	goleak.VerifyTestMain(m)
}

var now = time.Date(2026, 12, 1, 12, 0, 0, 0, time.UTC)

// The fixtures' 5-stack are the mock RSO's example players.
func player(n int, userID string) store.Linked {
	return store.Linked{
		UserID: userID, Game: stats.Game, Region: "na",
		ExternalPlayerID: "00000000-0000-4000-8000-00000000000" + string(rune('0'+n)),
	}
}

type harness struct {
	worker *matchsync.SyncWorker
	store  *fakes.FakeStore
	source *fakes.GatedSource
	stop   func()
}

// start runs a worker on the fixtures; each fetch waits for source.Release().
func start(t *testing.T) *harness {
	t.Helper()
	fixtures, err := source.NewFixtures(func() time.Time { return now })
	require.NoError(t, err)
	tables, err := lookup.Load()
	require.NoError(t, err)
	st := fakes.NewFakeStore()
	src := fakes.NewGatedSource(fixtures)
	// A second per request, so estimates are easy to compare.
	estimate := func(requests int) time.Duration { return time.Duration(requests) * time.Second }
	w := matchsync.New(src, st, tables, estimate, func() time.Time { return now }, slog.New(slog.NewTextHandler(io.Discard, nil)))
	ctx, cancel := context.WithCancel(context.Background())
	done := make(chan struct{})
	go func() {
		_ = w.Run(ctx)
		close(done)
	}()
	h := &harness{worker: w, store: st, source: src}
	h.stop = func() {
		src.Release()
		cancel()
		<-done
	}
	t.Cleanup(h.stop)
	return h
}

func waitFor(t *testing.T, what string, condition func() bool) {
	t.Helper()
	require.Eventually(t, condition, 5*time.Second, 10*time.Millisecond, what)
}

func TestASyncStoresThePlayersLast30Days(t *testing.T) {
	h := start(t)
	sam := player(1, "sam")

	require.NoError(t, h.worker.Enqueue(context.Background(), sam))
	assert.Equal(t, store.InProgress, h.store.Status("sam").Status)
	h.source.Release()

	waitFor(t, "the sync finishes", func() bool { return h.store.Status("sam").Status == store.Complete })
	records := h.store.Records("sam")
	assert.Len(t, records, 20)
	for _, r := range records {
		assert.Equal(t, "sam", r.UserID)
		assert.False(t, r.PlayedAt.Before(now.Add(-matchsync.Window)))
	}
}

func TestSyncingWhileOneIsQueuedOrRunningDoesNothing(t *testing.T) {
	h := start(t)
	sam := player(1, "sam")
	require.NoError(t, h.worker.Enqueue(context.Background(), sam))
	<-h.source.Started

	require.NoError(t, h.worker.Enqueue(context.Background(), sam))
	h.source.Release()

	waitFor(t, "the sync finishes", func() bool { return h.store.Status("sam").Status == store.Complete })
	assert.Equal(t, []string{"sam"}, h.store.Completed, "one sync, not two")
}

func TestTheSecondPlayerInTheQueueGetsALongerEstimate(t *testing.T) {
	h := start(t)
	ctx := context.Background()
	require.NoError(t, h.worker.Enqueue(ctx, player(1, "sam")))
	<-h.source.Started
	require.NoError(t, h.worker.Enqueue(ctx, player(2, "alex")))

	first, ok := h.worker.Progress(ctx, "sam")
	require.True(t, ok)
	second, ok := h.worker.Progress(ctx, "alex")
	require.True(t, ok)

	assert.Equal(t, 0, first.QueuePosition)
	assert.Equal(t, 1, second.QueuePosition)
	assert.Greater(t, second.Estimate, first.Estimate)
	_, ok = h.worker.Progress(ctx, "kim")
	assert.False(t, ok, "nothing queued for Kim")
}

func TestAFailedSyncKeepsTheOldDataAndASafeMessage(t *testing.T) {
	h := start(t)
	sam := player(1, "sam")
	h.store.Put(sam, []stats.Record{{UserID: "sam", MatchID: "old", PlayedAt: now, Numbers: map[string]float64{}}}, now)
	h.source.Err = &source.Error{Message: "The match data service is busy. Try again in a few minutes."}

	require.NoError(t, h.worker.Enqueue(context.Background(), sam))
	h.source.Release()

	waitFor(t, "the sync fails", func() bool { return h.store.Status("sam").Status == store.Failed })
	assert.Equal(t, "The match data service is busy. Try again in a few minutes.", h.store.Status("sam").ErrorMessage)
	require.Len(t, h.store.Records("sam"), 1)
	assert.Equal(t, "old", h.store.Records("sam")[0].MatchID)
}

func TestCancelStopsARunningSyncAndNothingIsWritten(t *testing.T) {
	h := start(t)
	sam := player(1, "sam")
	require.NoError(t, h.worker.Enqueue(context.Background(), sam))
	<-h.source.Started

	h.worker.Cancel("sam")

	_, running := h.worker.Progress(context.Background(), "sam")
	assert.False(t, running)
	assert.Empty(t, h.store.Completed)
	assert.Empty(t, h.store.Failed)
}

func TestCancelDropsAQueuedSync(t *testing.T) {
	h := start(t)
	ctx := context.Background()
	require.NoError(t, h.worker.Enqueue(ctx, player(1, "sam")))
	<-h.source.Started
	require.NoError(t, h.worker.Enqueue(ctx, player(2, "alex")))

	h.worker.Cancel("alex")
	h.source.Release()

	waitFor(t, "Sam's sync finishes", func() bool { return h.store.Status("sam").Status == store.Complete })
	assert.Equal(t, []string{"sam"}, h.store.Completed)
}

func TestASyncGivesFriendsRecordsTheirPairKeysAboutThePlayer(t *testing.T) {
	h := start(t)
	ctx := context.Background()
	require.NoError(t, h.worker.Enqueue(ctx, player(1, "sam")))
	h.source.Release()
	waitFor(t, "Sam's sync", func() bool { return h.store.Status("sam").Status == store.Complete })
	for _, r := range h.store.Records("sam") {
		assert.NotContains(t, r.Numbers, "assists_for:alex", "Alex isn't linked yet")
	}

	require.NoError(t, h.worker.Enqueue(ctx, player(2, "alex")))
	waitFor(t, "Alex's sync", func() bool { return h.store.Status("alex").Status == store.Complete })

	withKey := 0
	for _, r := range h.store.Records("sam") {
		if _, ok := r.Numbers["assists_for:alex"]; ok {
			withKey++
		}
	}
	assert.Positive(t, withKey, "Alex's sync added Sam's keys about Alex to the matches they shared")
	for _, r := range h.store.Records("alex") {
		_, ok := r.Numbers["assists_for:sam"]
		if r.Labels["team_id"] != "" && ok {
			return
		}
	}
	t.Fatal("Alex's own records name Sam")
}

func TestASyncInterruptedByAShutdownIsMarkedFailedNextTime(t *testing.T) {
	st := fakes.NewFakeStore()
	require.NoError(t, st.StartSync(context.Background(), player(1, "sam"), now))
	tables, err := lookup.Load()
	require.NoError(t, err)
	w := matchsync.New(fakes.NewGatedSource(nil), st, tables, func(int) time.Duration { return 0 },
		func() time.Time { return now }, slog.New(slog.NewTextHandler(io.Discard, nil)))

	require.NoError(t, w.RecoverInterrupted(context.Background()))

	assert.Equal(t, store.Failed, st.Status("sam").Status)
	assert.Equal(t, "The sync was interrupted. Sign in again to sync your matches.", st.Status("sam").ErrorMessage)
}
