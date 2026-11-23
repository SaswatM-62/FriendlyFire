//go:build integration

package store_test

import (
	"context"
	"math"
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/migrations"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
	"github.com/jackc/pgx/v5/pgxpool"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"github.com/testcontainers/testcontainers-go"
	"github.com/testcontainers/testcontainers-go/modules/postgres"
)

var now = time.Date(2026, 11, 20, 12, 0, 0, 0, time.UTC)

// newStore starts a fresh Postgres in Docker with the migrations applied.
func newStore(t *testing.T) (*store.MatchStore, *pgxpool.Pool) {
	t.Helper()
	ctx := context.Background()
	container, err := postgres.Run(ctx, "postgres:16.15-alpine",
		postgres.WithDatabase("friendlyfire"),
		postgres.WithInitScripts(),
		postgres.BasicWaitStrategies(),
	)
	testcontainers.CleanupContainer(t, container)
	require.NoError(t, err)
	connString, err := container.ConnectionString(ctx, "sslmode=disable", "search_path=matchdata")
	require.NoError(t, err)
	pool, err := pgxpool.New(ctx, connString)
	require.NoError(t, err)
	t.Cleanup(pool.Close)
	_, err = pool.Exec(ctx, "CREATE SCHEMA matchdata")
	require.NoError(t, err)
	require.NoError(t, migrations.Up(ctx, connString))
	return store.New(pool), pool
}

func linked(userID string) store.Linked {
	return store.Linked{UserID: userID, Game: stats.Game, ExternalPlayerID: "puuid-" + userID, Region: "na"}
}

func record(userID, matchID string, daysAgo int, numbers map[string]float64) stats.Record {
	if numbers == nil {
		numbers = map[string]float64{"headshots": 3}
	}
	return stats.Record{
		UserID: userID, MatchID: matchID, PlayedAt: now.AddDate(0, 0, -daysAgo), Won: true,
		Kills: 20, Deaths: 10, Assists: 5,
		Labels:  map[string]string{"agent": "Jett", "queue_id": "competitive"},
		Numbers: numbers,
	}
}

func sync(t *testing.T, s *store.MatchStore, userID string, records ...stats.Record) {
	t.Helper()
	ctx := context.Background()
	require.NoError(t, s.StartSync(ctx, linked(userID), now))
	require.NoError(t, s.CompleteSync(ctx, userID, records, nil, 4, now))
}

func TestASyncReplacesThePlayersDataAndASharedMatchIsStoredOnce(t *testing.T) {
	s, pool := newStore(t)
	ctx := context.Background()
	sync(t, s, "sam", record("sam", "m1", 1, nil), record("sam", "m2", 2, nil))
	sync(t, s, "alex", record("alex", "m1", 1, nil))

	sync(t, s, "sam", record("sam", "m1", 1, nil), record("sam", "m3", 3, nil))

	var matches int
	require.NoError(t, pool.QueryRow(ctx, "SELECT count(*) FROM matches").Scan(&matches))
	assert.Equal(t, 2, matches, "m1 once for both players, m3; m2 went with Sam's old data")
	statuses, err := s.Statuses(ctx, []string{"sam", "alex", "nobody"})
	require.NoError(t, err)
	assert.Equal(t, 2, statuses["sam"].MatchesStored)
	assert.Equal(t, store.Complete, statuses["sam"].Status)
	require.NotNil(t, statuses["sam"].LastSyncedAt)
	assert.True(t, statuses["sam"].LastSyncedAt.Equal(now))
	assert.NotContains(t, statuses, "nobody")
}

func TestAReplaceThatFailsHalfwayLeavesTheOldDataUntouched(t *testing.T) {
	s, _ := newStore(t)
	ctx := context.Background()
	sync(t, s, "sam", record("sam", "m1", 1, nil))

	// The second record can't be stored (NaN isn't valid JSON), after the first went in.
	broken := record("sam", "m3", 3, map[string]float64{"kd_ratio": math.NaN()})
	err := s.CompleteSync(ctx, "sam", []stats.Record{record("sam", "m2", 2, nil), broken}, nil, 4, now)

	require.Error(t, err)
	stored, err := s.Stats(ctx, []string{"sam"}, time.Time{}, 50)
	require.NoError(t, err)
	require.Len(t, stored, 1)
	assert.Equal(t, "m1", stored[0].MatchID)
}

func TestStatsAreNewestFirstCappedPerPlayerAndOnlyForTheRequestedPlayers(t *testing.T) {
	s, _ := newStore(t)
	ctx := context.Background()
	sync(t, s, "sam", record("sam", "m1", 1, nil), record("sam", "m2", 5, nil), record("sam", "m3", 10, nil))
	sync(t, s, "alex", record("alex", "m1", 1, nil), record("alex", "m4", 2, nil))
	sync(t, s, "kim", record("kim", "m5", 1, nil))

	got, err := s.Stats(ctx, []string{"sam", "alex"}, now.AddDate(0, 0, -7), 50)
	require.NoError(t, err)
	var ids []string
	for _, r := range got {
		ids = append(ids, r.UserID+"/"+r.MatchID)
		assert.NotEqual(t, "kim", r.UserID)
	}
	assert.Equal(t, []string{"alex/m1", "sam/m1", "alex/m4", "sam/m2"}, ids)
	assert.Equal(t, map[string]float64{"headshots": 3}, got[0].Numbers)
	assert.Equal(t, "Jett", got[0].Labels["agent"])

	capped, err := s.Stats(ctx, []string{"sam"}, time.Time{}, 2)
	require.NoError(t, err)
	assert.Len(t, capped, 2)
}

func TestAFailedSyncKeepsTheDataAndSaysWhy(t *testing.T) {
	s, _ := newStore(t)
	ctx := context.Background()
	sync(t, s, "sam", record("sam", "m1", 1, nil))
	require.NoError(t, s.StartSync(ctx, linked("sam"), now))

	require.NoError(t, s.FailSync(ctx, "sam", "The match data service is busy.", now))

	statuses, err := s.Statuses(ctx, []string{"sam"})
	require.NoError(t, err)
	assert.Equal(t, store.Failed, statuses["sam"].Status)
	assert.Equal(t, "The match data service is busy.", statuses["sam"].ErrorMessage)
	assert.Equal(t, 1, statuses["sam"].MatchesStored)
}

func TestFriendPairKeysAreRefreshedAndRemovedWithTheFriend(t *testing.T) {
	s, _ := newStore(t)
	ctx := context.Background()
	sync(t, s, "sam", record("sam", "m1", 1, nil))
	require.NoError(t, s.StartSync(ctx, linked("alex"), now))
	pairs := []store.PairUpdate{{UserID: "sam", MatchID: "m1", Numbers: map[string]float64{"assists_for:alex": 2}}}
	require.NoError(t, s.CompleteSync(ctx, "alex", []stats.Record{record("alex", "m1", 1, nil)}, pairs, 4, now))

	got, err := s.Stats(ctx, []string{"sam"}, time.Time{}, 50)
	require.NoError(t, err)
	assert.Equal(t, 2.0, got[0].Numbers["assists_for:alex"], "Alex's sync added the key to Sam's record")
	assert.Equal(t, 3.0, got[0].Numbers["headshots"])

	require.NoError(t, s.Delete(ctx, "alex"))

	got, err = s.Stats(ctx, []string{"sam"}, time.Time{}, 50)
	require.NoError(t, err)
	assert.NotContains(t, got[0].Numbers, "assists_for:alex")
	statuses, err := s.Statuses(ctx, []string{"alex"})
	require.NoError(t, err)
	assert.Empty(t, statuses)
}

func TestASyncFinishingAfterThePlayerWasDeletedKeepsNothing(t *testing.T) {
	s, _ := newStore(t)
	ctx := context.Background()
	require.NoError(t, s.StartSync(ctx, linked("sam"), now))
	require.NoError(t, s.Delete(ctx, "sam"))

	err := s.CompleteSync(ctx, "sam", []stats.Record{record("sam", "m1", 1, nil)}, nil, 4, now)

	require.Error(t, err)
	got, err := s.Stats(ctx, []string{"sam"}, time.Time{}, 50)
	require.NoError(t, err)
	assert.Empty(t, got)
}

func TestLinkedPlayersAverageRequestsAndInterruptedSyncs(t *testing.T) {
	s, _ := newStore(t)
	ctx := context.Background()
	sync(t, s, "sam")
	require.NoError(t, s.StartSync(ctx, linked("alex"), now))

	players, err := s.LinkedPlayers(ctx, stats.Game)
	require.NoError(t, err)
	assert.Equal(t, map[string]string{"puuid-sam": "sam", "puuid-alex": "alex"}, players)
	average, err := s.AverageRequests(ctx)
	require.NoError(t, err)
	assert.Equal(t, 4.0, average)

	require.NoError(t, s.FailInterrupted(ctx, "Interrupted.", now))
	statuses, err := s.Statuses(ctx, []string{"alex", "sam"})
	require.NoError(t, err)
	assert.Equal(t, store.Failed, statuses["alex"].Status)
	assert.Equal(t, store.Complete, statuses["sam"].Status)
}
