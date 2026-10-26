// Package matchdata is friendlyfire.matchdata.v1.MatchDataService. Every RPC is a stub
// for now.
package matchdata

import (
	"context"

	matchdatav1 "github.com/SaswatM-62/FriendlyFire/gen/go/friendlyfire/matchdata/v1"
	"google.golang.org/grpc/codes"
	"google.golang.org/grpc/status"
)

// Server implements MatchDataService. It is the only code that reads or writes the
// `matchdata` tables, and it only holds data for players who linked their account.
type Server struct {
	matchdatav1.UnimplementedMatchDataServiceServer
}

// NewServer returns the service. The store, sync worker and match source get
// injected here once they exist.
func NewServer() *Server {
	return &Server{}
}

// SyncPlayer starts fetching a player's matches in the background.
func (s *Server) SyncPlayer(
	ctx context.Context, req *matchdatav1.SyncPlayerRequest,
) (*matchdatav1.SyncPlayerResponse, error) {
	// TODO(ff): start fetching a player's matches in the background and return at once
	//   - first sync: the last 30 days; later syncs: only matches newer than the last
	//     one stored; calling it while a sync is running does nothing
	//   - return the current status (usually IN_PROGRESS)
	//   - errors: none in the contract
	//   - uses: store.MatchStore, sync.SyncWorker (with source.MatchSource)
	return nil, notImplemented()
}

// GetSyncStatus returns the sync status of one or more players.
func (s *Server) GetSyncStatus(
	ctx context.Context, req *matchdatav1.GetSyncStatusRequest,
) (*matchdatav1.GetSyncStatusResponse, error) {
	// TODO(ff): sync status for one or more players
	//   - players we know nothing about come back as SYNC_STATUS_NOT_LINKED
	//   - include matches_stored, last_synced_at (unset if never finished) and a short,
	//     safe-to-show error_message when FAILED
	//   - errors: none in the contract
	//   - uses: store.MatchStore, sync.SyncWorker
	return nil, notImplemented()
}

// GetPlayerMatchStats returns stored match stats, newest first.
func (s *Server) GetPlayerMatchStats(
	ctx context.Context, req *matchdatav1.GetPlayerMatchStatsRequest,
) (*matchdatav1.GetPlayerMatchStatsResponse, error) {
	// TODO(ff): stored match stats for the given players, newest first
	//   - only matches at or after `since`; at most max_matches_per_player each
	//     (0 means 50)
	//   - a match several of the players played together appears once per player,
	//     with the same match_id
	//   - errors: none in the contract
	//   - uses: store.MatchStore
	return nil, notImplemented()
}

// DeletePlayerData deletes everything stored for a player.
func (s *Server) DeletePlayerData(
	ctx context.Context, req *matchdatav1.DeletePlayerDataRequest,
) (*matchdatav1.DeletePlayerDataResponse, error) {
	// TODO(ff): delete everything stored for a player (they unlinked or deleted their account)
	//   - stop a sync in progress for them first
	//   - errors: none in the contract
	//   - uses: sync.SyncWorker, store.MatchStore
	return nil, notImplemented()
}

func notImplemented() error {
	return status.Error(codes.Unimplemented, "not implemented yet")
}
