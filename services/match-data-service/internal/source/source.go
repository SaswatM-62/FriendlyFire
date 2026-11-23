// Package source fetches players' matches from outside APIs: HenrikDev in development,
// Riot's API in production (later), or the saved fixtures offline.
package source

import (
	"context"
	"errors"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
)

// Player is who to fetch matches for.
type Player struct {
	PUUID  string
	Region string
}

// Result is a fetch's matches, newest first, and how many requests it took.
type Result struct {
	Matches  []henrik.Match
	Requests int
}

// MatchSource fetches one player's Competitive and Unrated matches.
type MatchSource interface {
	// Matches returns the player's matches that started at or after since, newest first.
	// Errors are *Error, with a message safe to show the player.
	Matches(ctx context.Context, player Player, since time.Time) (Result, error)
}

// Error is a failed fetch. Message is short and safe to show (never the API key, a URL or
// a raw upstream error); Err is the cause, for logs.
type Error struct {
	Message string
	Err     error
}

func (e *Error) Error() string {
	if e.Err != nil {
		return e.Message + ": " + e.Err.Error()
	}
	return e.Message
}

func (e *Error) Unwrap() error { return e.Err }

// SafeMessage is the message to show for any error a source returns.
func SafeMessage(err error) string {
	var sourceErr *Error
	if errors.As(err, &sourceErr) {
		return sourceErr.Message
	}
	return "Couldn't get your matches. Try again later."
}

// Queues are the queues FriendlyFire uses.
var Queues = []string{"competitive", "unrated"}
