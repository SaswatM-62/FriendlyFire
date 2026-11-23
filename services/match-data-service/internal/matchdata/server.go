// Package matchdata is friendlyfire.matchdata.v1.MatchDataService.
package matchdata

import (
	"context"
	"log/slog"
	"math"
	"strings"
	"time"

	matchdatav1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/matchdata/v1"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/store"
	matchsync "github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/sync"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
	"google.golang.org/protobuf/types/known/timestamppb"
)

const (
	// DefaultMatchesPerPlayer is GetPlayerMatchStats' cap when the request says 0.
	DefaultMatchesPerPlayer = 50
	// MaxMatchesPerPlayer caps any request.
	MaxMatchesPerPlayer = 500
)

// Store is what the service reads from storage (store.MatchStore; a fake in tests).
type Store interface {
	Statuses(ctx context.Context, userIDs []string) (map[string]store.Status, error)
	Stats(ctx context.Context, userIDs []string, since time.Time, perPlayer int) ([]stats.Record, error)
	Delete(ctx context.Context, userID string) error
}

// Syncer runs syncs (sync.SyncWorker).
type Syncer interface {
	Enqueue(ctx context.Context, player store.Linked) error
	Progress(ctx context.Context, userID string) (matchsync.Progress, bool)
	Cancel(userID string)
}

// Server implements MatchDataService. It is the only code that reads or writes the
// `matchdata` tables, and it only holds data for players who linked their account.
type Server struct {
	matchdatav1.UnimplementedMatchDataServiceServer
	store  Store
	syncer Syncer
	log    *slog.Logger
}

// NewServer returns the service.
func NewServer(st Store, syncer Syncer, log *slog.Logger) *Server {
	return &Server{store: st, syncer: syncer, log: log}
}

// SyncPlayer queues a sync of the player's last 30 days and returns at once. A sync
// already queued or running for them is left alone.
func (s *Server) SyncPlayer(
	ctx context.Context, req *matchdatav1.SyncPlayerRequest,
) (*matchdatav1.SyncPlayerResponse, error) {
	game := req.GetGame()
	if game == "" {
		game = stats.Game
	}
	if game != stats.Game {
		return nil, status.Errorf(codes.InvalidArgument, "game %q isn't supported", game)
	}
	if req.GetUserId() == "" || req.GetExternalPlayerId() == "" || req.GetRegion() == "" {
		return nil, status.Error(codes.InvalidArgument, "user_id, external_player_id and region are required")
	}
	player := store.Linked{
		UserID: req.GetUserId(), Game: game,
		ExternalPlayerID: req.GetExternalPlayerId(), Region: strings.ToLower(req.GetRegion()),
	}
	if err := s.syncer.Enqueue(ctx, player); err != nil {
		s.log.Error("couldn't queue a sync", "user_id", player.UserID, "err", err)
		return nil, status.Error(codes.Unavailable, "couldn't start the sync")
	}
	return &matchdatav1.SyncPlayerResponse{Status: matchdatav1.SyncStatus_SYNC_STATUS_IN_PROGRESS}, nil
}

// GetSyncStatus returns each requested player's raw sync status; players we know nothing
// about are NOT_LINKED.
func (s *Server) GetSyncStatus(
	ctx context.Context, req *matchdatav1.GetSyncStatusRequest,
) (*matchdatav1.GetSyncStatusResponse, error) {
	statuses, err := s.store.Statuses(ctx, req.GetUserIds())
	if err != nil {
		s.log.Error("couldn't read sync statuses", "err", err)
		return nil, status.Error(codes.Unavailable, "match data can't be read right now")
	}
	response := &matchdatav1.GetSyncStatusResponse{}
	for _, userID := range req.GetUserIds() {
		row := &matchdatav1.PlayerSyncStatus{UserId: userID, Status: matchdatav1.SyncStatus_SYNC_STATUS_NOT_LINKED}
		if st, ok := statuses[userID]; ok {
			row.MatchesStored = int32(st.MatchesStored)
			if st.LastSyncedAt != nil {
				row.LastSyncedAt = timestamppb.New(*st.LastSyncedAt)
			}
			switch st.Status {
			case store.Complete:
				row.Status = matchdatav1.SyncStatus_SYNC_STATUS_COMPLETE
			case store.Failed:
				row.Status = matchdatav1.SyncStatus_SYNC_STATUS_FAILED
				row.ErrorMessage = st.ErrorMessage
			default:
				row.Status = matchdatav1.SyncStatus_SYNC_STATUS_IN_PROGRESS
			}
		}
		if progress, ok := s.syncer.Progress(ctx, userID); ok {
			row.Status = matchdatav1.SyncStatus_SYNC_STATUS_IN_PROGRESS
			row.ErrorMessage = ""
			row.QueuePosition = int32(progress.QueuePosition)
			row.EstimatedSeconds = int32(math.Max(1, math.Ceil(progress.Estimate.Seconds())))
		}
		response.Statuses = append(response.Statuses, row)
	}
	return response, nil
}

// GetPlayerMatchStats returns the requested players' stored stats, newest first. Nothing
// about any other player is in the response.
func (s *Server) GetPlayerMatchStats(
	ctx context.Context, req *matchdatav1.GetPlayerMatchStatsRequest,
) (*matchdatav1.GetPlayerMatchStatsResponse, error) {
	if req.GetMaxMatchesPerPlayer() < 0 {
		return nil, status.Error(codes.InvalidArgument, "max_matches_per_player can't be negative")
	}
	perPlayer := int(req.GetMaxMatchesPerPlayer())
	if perPlayer == 0 {
		perPlayer = DefaultMatchesPerPlayer
	}
	perPlayer = min(perPlayer, MaxMatchesPerPlayer)
	var since time.Time
	if req.GetSince() != nil {
		since = req.GetSince().AsTime()
	}
	response := &matchdatav1.GetPlayerMatchStatsResponse{}
	if len(req.GetUserIds()) == 0 {
		return response, nil
	}
	records, err := s.store.Stats(ctx, req.GetUserIds(), since, perPlayer)
	if err != nil {
		s.log.Error("couldn't read match stats", "err", err)
		return nil, status.Error(codes.Unavailable, "match data can't be read right now")
	}
	for _, r := range records {
		response.Stats = append(response.Stats, &matchdatav1.PlayerMatchStats{
			MatchId:  r.MatchID,
			UserId:   r.UserID,
			Game:     stats.Game,
			PlayedAt: timestamppb.New(r.PlayedAt),
			Won:      r.Won,
			Kills:    int32(r.Kills),
			Deaths:   int32(r.Deaths),
			Assists:  int32(r.Assists),
			Labels:   r.Labels,
			Numbers:  r.Numbers,
		})
	}
	return response, nil
}

// DeletePlayerData stops a sync in progress for the player, then deletes everything
// stored for them, including friend-pair keys on other players' records that name them.
func (s *Server) DeletePlayerData(
	ctx context.Context, req *matchdatav1.DeletePlayerDataRequest,
) (*matchdatav1.DeletePlayerDataResponse, error) {
	if req.GetUserId() == "" {
		return nil, status.Error(codes.InvalidArgument, "user_id is required")
	}
	s.syncer.Cancel(req.GetUserId())
	if err := s.store.Delete(ctx, req.GetUserId()); err != nil {
		s.log.Error("couldn't delete player data", "user_id", req.GetUserId(), "err", err)
		return nil, status.Error(codes.Unavailable, "match data can't be deleted right now")
	}
	return &matchdatav1.DeletePlayerDataResponse{}, nil
}
