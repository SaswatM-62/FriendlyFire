// Package store keeps linked players, their sync status and their match stats in the
// `matchdata` schema.
package store

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/stats"
	"github.com/jackc/pgx/v5"
	"github.com/jackc/pgx/v5/pgxpool"
)

// Sync statuses as stored.
const (
	InProgress = "IN_PROGRESS"
	Complete   = "COMPLETE"
	Failed     = "FAILED"
)

// Linked is a player who linked their game account.
type Linked struct {
	UserID           string
	Game             string
	ExternalPlayerID string
	Region           string
}

// Status is a player's stored sync status.
type Status struct {
	UserID        string
	Status        string
	ErrorMessage  string
	LastSyncedAt  *time.Time
	MatchesStored int
}

// PairUpdate adds friend-pair keys to another linked player's stored record of a match:
// a sync of one player refreshes the keys their friends' records have about them.
type PairUpdate struct {
	UserID  string
	MatchID string
	Numbers map[string]float64
}

// MatchStore reads and writes the `matchdata` tables.
type MatchStore struct {
	pool *pgxpool.Pool
}

// New returns a store on the service's pool.
func New(pool *pgxpool.Pool) *MatchStore {
	return &MatchStore{pool: pool}
}

// StartSync remembers the player's game account and marks a sync in progress. Their
// stored matches and last sync time stay.
func (s *MatchStore) StartSync(ctx context.Context, player Linked, now time.Time) error {
	_, err := s.pool.Exec(ctx, `
		INSERT INTO players (user_id, game, external_player_id, region, status, error_message, updated_at)
		VALUES ($1, $2, $3, $4, 'IN_PROGRESS', '', $5)
		ON CONFLICT (user_id) DO UPDATE SET game = $2, external_player_id = $3, region = $4,
			status = 'IN_PROGRESS', error_message = '', updated_at = $5`,
		player.UserID, player.Game, player.ExternalPlayerID, player.Region, now)
	return err
}

// CompleteSync replaces everything stored for the player with these records, in one
// transaction: if anything fails, the old data stays. Matches older than the new ones'
// window go with it, so only the last 30 days stay.
func (s *MatchStore) CompleteSync(
	ctx context.Context, userID string, records []stats.Record, pairs []PairUpdate, requests int, now time.Time,
) (err error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer func() {
		if err != nil {
			_ = tx.Rollback(ctx)
		}
	}()
	if _, err = tx.Exec(ctx, `DELETE FROM player_match_stats WHERE user_id = $1`, userID); err != nil {
		return err
	}
	for _, r := range records {
		labels, err := json.Marshal(r.Labels)
		if err != nil {
			return err
		}
		numbers, err := json.Marshal(r.Numbers)
		if err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, `INSERT INTO matches (game, match_id, played_at) VALUES ($1, $2, $3)
			ON CONFLICT DO NOTHING`, stats.Game, r.MatchID, r.PlayedAt); err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, `
			INSERT INTO player_match_stats (user_id, game, match_id, played_at, won, kills, deaths, assists, labels, numbers)
			VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)`,
			userID, stats.Game, r.MatchID, r.PlayedAt, r.Won, r.Kills, r.Deaths, r.Assists, labels, numbers); err != nil {
			return err
		}
	}
	for _, p := range pairs {
		numbers, err := json.Marshal(p.Numbers)
		if err != nil {
			return err
		}
		if _, err = tx.Exec(ctx, `UPDATE player_match_stats SET numbers = numbers || $1
			WHERE user_id = $2 AND game = $3 AND match_id = $4`, numbers, p.UserID, stats.Game, p.MatchID); err != nil {
			return err
		}
	}
	if err = deleteOrphanMatches(ctx, tx); err != nil {
		return err
	}
	tag, err := tx.Exec(ctx, `UPDATE players SET status = 'COMPLETE', error_message = '', last_synced_at = $2,
		last_sync_requests = $3, updated_at = $2 WHERE user_id = $1`, userID, now, requests)
	if err != nil {
		return err
	}
	if tag.RowsAffected() == 0 {
		// The player was deleted while the sync ran: keep nothing.
		return errors.New("player no longer linked")
	}
	return tx.Commit(ctx)
}

// FailSync records a failed sync with a safe-to-show message. Stored matches stay.
func (s *MatchStore) FailSync(ctx context.Context, userID, message string, now time.Time) error {
	_, err := s.pool.Exec(ctx, `UPDATE players SET status = 'FAILED', error_message = $2, updated_at = $3
		WHERE user_id = $1`, userID, message, now)
	return err
}

// FailInterrupted marks syncs that were running when the service stopped as failed.
func (s *MatchStore) FailInterrupted(ctx context.Context, message string, now time.Time) error {
	_, err := s.pool.Exec(ctx, `UPDATE players SET status = 'FAILED', error_message = $1, updated_at = $2
		WHERE status = 'IN_PROGRESS'`, message, now)
	return err
}

// LinkedPlayers maps each linked player's external ID (PUUID) to their user ID.
func (s *MatchStore) LinkedPlayers(ctx context.Context, game string) (map[string]string, error) {
	rows, err := s.pool.Query(ctx, `SELECT external_player_id, user_id FROM players WHERE game = $1`, game)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	linked := map[string]string{}
	for rows.Next() {
		var external, userID string
		if err := rows.Scan(&external, &userID); err != nil {
			return nil, err
		}
		linked[external] = userID
	}
	return linked, rows.Err()
}

// Statuses returns the stored status of each requested player we know; others are left out.
func (s *MatchStore) Statuses(ctx context.Context, userIDs []string) (map[string]Status, error) {
	rows, err := s.pool.Query(ctx, `
		SELECT p.user_id, p.status, p.error_message, p.last_synced_at,
			(SELECT count(*) FROM player_match_stats m WHERE m.user_id = p.user_id)
		FROM players p WHERE p.user_id = ANY($1)`, userIDs)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	statuses := map[string]Status{}
	for rows.Next() {
		var st Status
		if err := rows.Scan(&st.UserID, &st.Status, &st.ErrorMessage, &st.LastSyncedAt, &st.MatchesStored); err != nil {
			return nil, err
		}
		statuses[st.UserID] = st
	}
	return statuses, rows.Err()
}

// Stats returns the requested players' stored records, newest first, at most perPlayer
// each, only matches at or after since (zero: all). Only these players' rows come back.
func (s *MatchStore) Stats(ctx context.Context, userIDs []string, since time.Time, perPlayer int) ([]stats.Record, error) {
	var sinceArg any
	if !since.IsZero() {
		sinceArg = since
	}
	rows, err := s.pool.Query(ctx, `
		SELECT user_id, match_id, played_at, won, kills, deaths, assists, labels, numbers FROM (
			SELECT *, row_number() OVER (PARTITION BY user_id ORDER BY played_at DESC, match_id) AS n
			FROM player_match_stats
			WHERE user_id = ANY($1) AND ($2::timestamptz IS NULL OR played_at >= $2)
		) ranked
		WHERE n <= $3
		ORDER BY played_at DESC, match_id, user_id`, userIDs, sinceArg, perPlayer)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	var records []stats.Record
	for rows.Next() {
		var r stats.Record
		var labels, numbers []byte
		if err := rows.Scan(&r.UserID, &r.MatchID, &r.PlayedAt, &r.Won, &r.Kills, &r.Deaths, &r.Assists,
			&labels, &numbers); err != nil {
			return nil, err
		}
		if err := json.Unmarshal(labels, &r.Labels); err != nil {
			return nil, fmt.Errorf("labels of %s: %w", r.MatchID, err)
		}
		if err := json.Unmarshal(numbers, &r.Numbers); err != nil {
			return nil, fmt.Errorf("numbers of %s: %w", r.MatchID, err)
		}
		r.PlayedAt = r.PlayedAt.UTC()
		records = append(records, r)
	}
	return records, rows.Err()
}

// StoredMatches says which of these matches the player has a record of.
func (s *MatchStore) StoredMatches(ctx context.Context, userID string, matchIDs []string) (map[string]bool, error) {
	rows, err := s.pool.Query(ctx, `SELECT match_id FROM player_match_stats
		WHERE user_id = $1 AND game = $2 AND match_id = ANY($3)`, userID, stats.Game, matchIDs)
	if err != nil {
		return nil, err
	}
	defer rows.Close()
	stored := map[string]bool{}
	for rows.Next() {
		var id string
		if err := rows.Scan(&id); err != nil {
			return nil, err
		}
		stored[id] = true
	}
	return stored, rows.Err()
}

// AverageRequests is the mean number of requests of past syncs; 0 if none finished yet.
func (s *MatchStore) AverageRequests(ctx context.Context) (float64, error) {
	var average *float64
	err := s.pool.QueryRow(ctx, `SELECT avg(last_sync_requests)::float8 FROM players
		WHERE last_sync_requests IS NOT NULL`).Scan(&average)
	if err != nil || average == nil {
		return 0, err
	}
	return *average, nil
}

// Delete removes everything stored for the player, and every friend-pair key on other
// players' records that names them.
func (s *MatchStore) Delete(ctx context.Context, userID string) (err error) {
	tx, err := s.pool.Begin(ctx)
	if err != nil {
		return err
	}
	defer func() {
		if err != nil {
			_ = tx.Rollback(ctx)
		}
	}()
	if _, err = tx.Exec(ctx, `DELETE FROM players WHERE user_id = $1`, userID); err != nil {
		return err
	}
	if _, err = tx.Exec(ctx, `UPDATE player_match_stats
		SET numbers = numbers - ARRAY['assists_for:' || $1::text, 'finishes_for:' || $1::text, 'trades_for:' || $1::text]
		WHERE numbers ?| ARRAY['assists_for:' || $1::text, 'finishes_for:' || $1::text, 'trades_for:' || $1::text]`, userID); err != nil {
		return err
	}
	if err = deleteOrphanMatches(ctx, tx); err != nil {
		return err
	}
	return tx.Commit(ctx)
}

func deleteOrphanMatches(ctx context.Context, tx pgx.Tx) error {
	_, err := tx.Exec(ctx, `DELETE FROM matches m WHERE NOT EXISTS (
		SELECT 1 FROM player_match_stats p WHERE p.game = m.game AND p.match_id = m.match_id)`)
	return err
}
