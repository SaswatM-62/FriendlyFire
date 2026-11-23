package source

import (
	"bytes"
	"compress/gzip"
	"context"
	_ "embed"
	"encoding/json"
	"fmt"
	"sort"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
)

// The anonymized fixtures (fixtures/henrikdev/README.md): 20 real Competitive matches with
// every name and ID replaced. The 5-stack in them is the mock RSO's example test players.
//
//go:embed fixtures/henrikdev/matches.json.gz
var fixtureMatches []byte

// FixtureSource serves the saved matches instead of calling HenrikDev (MATCH_SOURCE=
// fixtures): no API key, no rate limit, a sync takes a moment. The matches are moved in
// time so the newest one started a day before now, keeping their spacing, so they're
// always inside the last 30 days.
type FixtureSource struct {
	matches []henrik.Match
	now     func() time.Time
}

// NewFixtures loads the embedded fixtures.
func NewFixtures(now func() time.Time) (*FixtureSource, error) {
	gz, err := gzip.NewReader(bytes.NewReader(fixtureMatches))
	if err != nil {
		return nil, fmt.Errorf("fixtures: %w", err)
	}
	var page henrik.Response
	if err := json.NewDecoder(gz).Decode(&page); err != nil {
		return nil, fmt.Errorf("fixtures: %w", err)
	}
	sort.SliceStable(page.Data, func(i, j int) bool {
		return page.Data[i].Metadata.StartedAt > page.Data[j].Metadata.StartedAt
	})
	return &FixtureSource{matches: page.Data, now: now}, nil
}

// Matches returns the fixture matches the player is in.
func (s *FixtureSource) Matches(ctx context.Context, player Player, since time.Time) (Result, error) {
	if len(s.matches) == 0 {
		return Result{}, nil
	}
	newest, err := s.matches[0].Metadata.Started()
	if err != nil {
		return Result{}, &Error{Message: "The saved matches are broken.", Err: err}
	}
	shift := s.now().Add(-24 * time.Hour).Sub(newest)
	var result Result
	for _, m := range s.matches {
		if err := ctx.Err(); err != nil {
			return Result{}, &Error{Message: "The sync was stopped.", Err: err}
		}
		if !hasPlayer(m, player.PUUID) {
			continue
		}
		started, err := m.Metadata.Started()
		if err != nil {
			continue
		}
		moved := started.Add(shift)
		if moved.Before(since) {
			continue
		}
		m.Metadata.StartedAt = moved.UTC().Format(time.RFC3339Nano)
		result.Matches = append(result.Matches, m)
	}
	return result, nil
}

func hasPlayer(m henrik.Match, puuid string) bool {
	for _, p := range m.Players {
		if p.PUUID == puuid {
			return true
		}
	}
	return false
}
