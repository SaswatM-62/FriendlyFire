package matchdata_test

import (
	"context"
	"io"
	"log/slog"
	"net"
	"testing"
	"time"

	matchdatav1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/matchdata/v1"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/matchdata"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
	matchsync "github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/sync"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil/fakes"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
	"go.uber.org/goleak"
	"google.golang.org/grpc"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/credentials/insecure"
	"google.golang.org/grpc/status"
	"google.golang.org/grpc/test/bufconn"
	"google.golang.org/protobuf/types/known/timestamppb"
)

func TestMain(m *testing.M) {
	goleak.VerifyTestMain(m)
}

var now = time.Date(2026, 11, 20, 12, 0, 0, 0, time.UTC)

type harness struct {
	client matchdatav1.MatchDataServiceClient
	store  *fakes.FakeStore
	syncer *fakes.FakeSyncer
}

// connect starts the service on an in-memory connection (bufconn), with fakes behind it.
func connect(t *testing.T) *harness {
	t.Helper()
	st, syncer := fakes.NewFakeStore(), fakes.NewFakeSyncer()
	listener := bufconn.Listen(1 << 20)
	server := grpc.NewServer()
	matchdatav1.RegisterMatchDataServiceServer(server,
		matchdata.NewServer(st, syncer, slog.New(slog.NewTextHandler(io.Discard, nil))))
	go func() { _ = server.Serve(listener) }()
	conn, err := grpc.NewClient("passthrough:///bufnet",
		grpc.WithContextDialer(func(ctx context.Context, _ string) (net.Conn, error) { return listener.DialContext(ctx) }),
		grpc.WithTransportCredentials(insecure.NewCredentials()))
	require.NoError(t, err)
	t.Cleanup(func() {
		_ = conn.Close()
		server.Stop()
	})
	return &harness{client: matchdatav1.NewMatchDataServiceClient(conn), store: st, syncer: syncer}
}

func linked(userID string) store.Linked {
	return store.Linked{UserID: userID, Game: stats.Game, ExternalPlayerID: "puuid-" + userID, Region: "na"}
}

func record(userID, matchID string, daysAgo int) stats.Record {
	return stats.Record{
		UserID: userID, MatchID: matchID, PlayedAt: now.AddDate(0, 0, -daysAgo), Won: true, Kills: 21, Deaths: 12,
		Assists: 4, Labels: map[string]string{"agent": "Jett"}, Numbers: map[string]float64{"headshots": 9},
	}
}

func TestSyncPlayerQueuesASyncAndReturnsAtOnce(t *testing.T) {
	h := connect(t)

	response, err := h.client.SyncPlayer(context.Background(), &matchdatav1.SyncPlayerRequest{
		UserId: "sam", Game: "valorant", ExternalPlayerId: "puuid-sam", Region: "NA"})

	require.NoError(t, err)
	assert.Equal(t, matchdatav1.SyncStatus_SYNC_STATUS_IN_PROGRESS, response.GetStatus())
	require.Len(t, h.syncer.Queued, 1)
	assert.Equal(t, store.Linked{UserID: "sam", Game: "valorant", ExternalPlayerID: "puuid-sam", Region: "na"}, h.syncer.Queued[0])
}

func TestSyncPlayerNeedsAPlayerAndAKnownGame(t *testing.T) {
	h := connect(t)
	for _, bad := range []*matchdatav1.SyncPlayerRequest{
		{Game: "valorant", ExternalPlayerId: "p", Region: "na"},
		{UserId: "sam", Game: "valorant", Region: "na"},
		{UserId: "sam", Game: "chess", ExternalPlayerId: "p", Region: "na"},
	} {
		_, err := h.client.SyncPlayer(context.Background(), bad)
		assert.Equal(t, codes.InvalidArgument, status.Code(err))
	}
	assert.Empty(t, h.syncer.Queued)
}

func TestGetSyncStatusForEveryKindOfPlayer(t *testing.T) {
	h := connect(t)
	ctx := context.Background()
	h.store.Put(linked("sam"), []stats.Record{record("sam", "m1", 1), record("sam", "m2", 2)}, now)
	h.store.Put(linked("alex"), nil, now)
	require.NoError(t, h.store.FailSync(ctx, "alex", "The match data service is busy.", now))
	h.store.Put(linked("kim"), []stats.Record{record("kim", "m1", 1)}, now)
	h.syncer.Running["kim"] = matchsync.Progress{QueuePosition: 2, Estimate: 90*time.Second + time.Millisecond}

	response, err := h.client.GetSyncStatus(ctx, &matchdatav1.GetSyncStatusRequest{UserIds: []string{"sam", "alex", "kim", "nobody"}})

	require.NoError(t, err)
	got := map[string]*matchdatav1.PlayerSyncStatus{}
	for _, s := range response.GetStatuses() {
		got[s.GetUserId()] = s
	}
	assert.Equal(t, matchdatav1.SyncStatus_SYNC_STATUS_COMPLETE, got["sam"].GetStatus())
	assert.EqualValues(t, 2, got["sam"].GetMatchesStored())
	assert.Equal(t, now, got["sam"].GetLastSyncedAt().AsTime())
	assert.Equal(t, matchdatav1.SyncStatus_SYNC_STATUS_FAILED, got["alex"].GetStatus())
	assert.Equal(t, "The match data service is busy.", got["alex"].GetErrorMessage())
	assert.Equal(t, matchdatav1.SyncStatus_SYNC_STATUS_IN_PROGRESS, got["kim"].GetStatus())
	assert.EqualValues(t, 2, got["kim"].GetQueuePosition())
	assert.EqualValues(t, 91, got["kim"].GetEstimatedSeconds(), "rounded up")
	assert.EqualValues(t, 1, got["kim"].GetMatchesStored(), "the old data still counts while syncing")
	assert.Equal(t, matchdatav1.SyncStatus_SYNC_STATUS_NOT_LINKED, got["nobody"].GetStatus())
	assert.Len(t, response.GetStatuses(), 4)
}

func TestGetPlayerMatchStatsNewestFirstForOnlyTheRequestedPlayers(t *testing.T) {
	h := connect(t)
	h.store.Put(linked("sam"), []stats.Record{record("sam", "m2", 2), record("sam", "m1", 1), record("sam", "m3", 9)}, now)
	h.store.Put(linked("alex"), []stats.Record{record("alex", "m1", 1)}, now)
	h.store.Put(linked("kim"), []stats.Record{record("kim", "m4", 1)}, now)

	response, err := h.client.GetPlayerMatchStats(context.Background(), &matchdatav1.GetPlayerMatchStatsRequest{
		UserIds: []string{"sam", "alex"}, Since: timestamppb.New(now.AddDate(0, 0, -5))})

	require.NoError(t, err)
	var got []string
	for _, s := range response.GetStats() {
		got = append(got, s.GetUserId()+"/"+s.GetMatchId())
		assert.NotEqual(t, "kim", s.GetUserId(), "nobody outside the request")
		assert.Equal(t, "valorant", s.GetGame())
	}
	assert.ElementsMatch(t, []string{"sam/m1", "alex/m1", "sam/m2"}, got)
	assert.Equal(t, "m2", response.GetStats()[len(got)-1].GetMatchId(), "newest first")
	first := response.GetStats()[0]
	assert.EqualValues(t, 21, first.GetKills())
	assert.Equal(t, "Jett", first.GetLabels()["agent"])
	assert.Equal(t, 9.0, first.GetNumbers()["headshots"])
}

func TestGetPlayerMatchStatsCapsEachPlayer(t *testing.T) {
	h := connect(t)
	var many []stats.Record
	for i := 0; i < 60; i++ {
		many = append(many, record("sam", "m"+string(rune('A'+i)), i%29))
	}
	h.store.Put(linked("sam"), many, now)
	ctx := context.Background()

	byDefault, err := h.client.GetPlayerMatchStats(ctx, &matchdatav1.GetPlayerMatchStatsRequest{UserIds: []string{"sam"}})
	require.NoError(t, err)
	capped, err := h.client.GetPlayerMatchStats(ctx, &matchdatav1.GetPlayerMatchStatsRequest{
		UserIds: []string{"sam"}, MaxMatchesPerPlayer: 5})
	require.NoError(t, err)
	_, err = h.client.GetPlayerMatchStats(ctx, &matchdatav1.GetPlayerMatchStatsRequest{
		UserIds: []string{"sam"}, MaxMatchesPerPlayer: -1})

	assert.Len(t, byDefault.GetStats(), 50)
	assert.Len(t, capped.GetStats(), 5)
	assert.Equal(t, codes.InvalidArgument, status.Code(err))
}

func TestDeletePlayerDataStopsTheSyncThenDeletesEverything(t *testing.T) {
	h := connect(t)
	h.store.Put(linked("sam"), []stats.Record{record("sam", "m1", 1)}, now)
	h.syncer.Running["sam"] = matchsync.Progress{}

	_, err := h.client.DeletePlayerData(context.Background(), &matchdatav1.DeletePlayerDataRequest{UserId: "sam"})

	require.NoError(t, err)
	assert.Equal(t, []string{"sam"}, h.syncer.Cancelled)
	assert.Empty(t, h.store.Records("sam"))
	response, err := h.client.GetSyncStatus(context.Background(), &matchdatav1.GetSyncStatusRequest{UserIds: []string{"sam"}})
	require.NoError(t, err)
	assert.Equal(t, matchdatav1.SyncStatus_SYNC_STATUS_NOT_LINKED, response.GetStatuses()[0].GetStatus())
}
