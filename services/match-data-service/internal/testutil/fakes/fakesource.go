package fakes

import (
	"context"
	"sync"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/source"
)

// GatedSource wraps a source: each fetch waits until the test releases it (or the sync is
// cancelled), so tests can look at a sync while it runs. Err makes fetches fail.
type GatedSource struct {
	Inner source.MatchSource
	Err   error

	mu      sync.Mutex
	gate    chan struct{}
	Started chan string
}

// NewGatedSource returns a source whose fetches block until Release.
func NewGatedSource(inner source.MatchSource) *GatedSource {
	return &GatedSource{Inner: inner, gate: make(chan struct{}), Started: make(chan string, 16)}
}

// Release lets every waiting and future fetch through.
func (g *GatedSource) Release() {
	g.mu.Lock()
	defer g.mu.Unlock()
	select {
	case <-g.gate:
	default:
		close(g.gate)
	}
}

func (g *GatedSource) Matches(ctx context.Context, player source.Player, since time.Time) (source.Result, error) {
	g.Started <- player.PUUID
	select {
	case <-ctx.Done():
		return source.Result{}, &source.Error{Message: "The sync was stopped.", Err: ctx.Err()}
	case <-g.gate:
	}
	if g.Err != nil {
		return source.Result{Requests: 1}, g.Err
	}
	return g.Inner.Matches(ctx, player, since)
}
