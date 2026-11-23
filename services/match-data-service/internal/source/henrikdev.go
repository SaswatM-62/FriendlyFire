package source

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"io"
	"net/http"
	"net/url"
	"sort"
	"strconv"
	"sync"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/ratelimit"
)

const (
	// pageSize is the most HenrikDev's v4 matches endpoint returns per request.
	pageSize = 10
	// maxPages per queue stops a runaway fetch (a busy player has ~10 pages a month).
	maxPages = 30
	// maxAttempts per request when HenrikDev answers 429 or 5xx.
	maxAttempts = 4
	// platform: FriendlyFire supports PC players.
	platform = "pc"
)

// HenrikDevSource is the MatchSource for the HenrikDev API. Every request goes through
// the shared rate limiter.
type HenrikDevSource struct {
	client  *http.Client
	baseURL string
	apiKey  string
	limiter *ratelimit.Limiter
	clock   ratelimit.Clock

	mu       sync.Mutex
	lastErr  error
	lastSeen time.Time
}

// Health reports whether HenrikDev can be used, from the last real request: health checks
// don't call it, so they never use up the rate limit. nil when there's no reason to think
// it's down.
func (s *HenrikDevSource) Health() error {
	if s.apiKey == "" {
		return errors.New("no API key")
	}
	s.mu.Lock()
	defer s.mu.Unlock()
	if s.lastErr != nil && s.clock.Now().Sub(s.lastSeen) < 5*time.Minute {
		return s.lastErr
	}
	return nil
}

func (s *HenrikDevSource) remember(err error) {
	s.mu.Lock()
	defer s.mu.Unlock()
	s.lastErr, s.lastSeen = err, s.clock.Now()
}

// NewHenrikDev returns a source for baseURL (https://api.henrikdev.xyz outside tests).
// The key goes in the Authorization header and is never logged or returned.
func NewHenrikDev(client *http.Client, baseURL, apiKey string, limiter *ratelimit.Limiter, clock ratelimit.Clock) *HenrikDevSource {
	return &HenrikDevSource{client: client, baseURL: baseURL, apiKey: apiKey, limiter: limiter, clock: clock}
}

// Matches pages through each queue, newest first, until a page is short or reaches
// matches older than since.
func (s *HenrikDevSource) Matches(ctx context.Context, player Player, since time.Time) (Result, error) {
	if s.apiKey == "" {
		return Result{}, &Error{Message: "Match syncing isn't set up on this server yet."}
	}
	var result Result
	seen := map[string]bool{}
	for _, queue := range Queues {
		for page := 0; page < maxPages; page++ {
			matches, err := s.page(ctx, player, queue, page*pageSize, &result.Requests)
			if err != nil {
				return Result{Requests: result.Requests}, err
			}
			older := false
			for _, m := range matches {
				started, err := m.Metadata.Started()
				if err != nil || started.Before(since) {
					older = true
					continue
				}
				if !seen[m.Metadata.MatchID] {
					seen[m.Metadata.MatchID] = true
					result.Matches = append(result.Matches, m)
				}
			}
			if len(matches) < pageSize || older {
				break
			}
		}
	}
	sort.SliceStable(result.Matches, func(i, j int) bool {
		return result.Matches[i].Metadata.StartedAt > result.Matches[j].Metadata.StartedAt
	})
	return result, nil
}

func (s *HenrikDevSource) page(ctx context.Context, player Player, queue string, start int, requests *int) ([]henrik.Match, error) {
	query := url.Values{"mode": {queue}, "size": {strconv.Itoa(pageSize)}, "start": {strconv.Itoa(start)}}
	target := fmt.Sprintf("%s/valorant/v4/by-puuid/matches/%s/%s/%s?%s",
		s.baseURL, url.PathEscape(player.Region), platform, url.PathEscape(player.PUUID), query.Encode())
	for attempt := 1; ; attempt++ {
		if err := s.limiter.Wait(ctx); err != nil {
			return nil, &Error{Message: "The sync was stopped.", Err: err}
		}
		*requests++
		matches, retryAfter, err := s.get(ctx, target)
		if err == nil {
			s.remember(nil)
			return matches, nil
		}
		if ctx.Err() == nil {
			s.remember(errors.New(SafeMessage(err)))
		}
		var retry *retryable
		if !errors.As(err, &retry) || attempt == maxAttempts {
			return nil, err
		}
		if retryAfter > 0 {
			// Everyone waits: the limit is shared by every sync.
			s.limiter.PauseFor(retryAfter)
		} else {
			select {
			case <-ctx.Done():
				return nil, &Error{Message: "The sync was stopped.", Err: ctx.Err()}
			case <-s.clock.After(time.Duration(attempt) * 2 * time.Second):
			}
		}
	}
}

// retryable marks a failure worth trying again (429, 5xx, a network error).
type retryable struct{ err error }

func (r *retryable) Error() string { return r.err.Error() }

func (s *HenrikDevSource) get(ctx context.Context, target string) ([]henrik.Match, time.Duration, error) {
	request, err := http.NewRequestWithContext(ctx, http.MethodGet, target, nil)
	if err != nil {
		return nil, 0, &Error{Message: "Couldn't get your matches. Try again later.", Err: err}
	}
	request.Header.Set("Authorization", s.apiKey)
	request.Header.Set("Accept", "application/json")
	response, err := s.client.Do(request)
	if err != nil {
		// Never wrap the request error as-is: it has the URL in it, not the key, but keep
		// the logs short anyway.
		return nil, 0, &Error{Message: "The match data service can't be reached. Try again later.",
			Err: &retryable{err: errors.New("request failed")}}
	}
	defer func() { _ = response.Body.Close() }()
	switch {
	case response.StatusCode == http.StatusOK:
		var page henrik.Response
		if err := json.NewDecoder(response.Body).Decode(&page); err != nil {
			return nil, 0, &Error{Message: "The match data service sent something unexpected.", Err: err}
		}
		return page.Data, 0, nil
	case response.StatusCode == http.StatusNotFound:
		// No matches in this queue (or none at all).
		return nil, 0, nil
	case response.StatusCode == http.StatusTooManyRequests:
		wait, _ := ratelimit.RetryAfter(response.Header.Get("Retry-After"), s.clock.Now())
		if wait <= 0 {
			wait = ratelimit.Window
		}
		return nil, wait, &Error{Message: "The match data service is busy. Try again in a few minutes.",
			Err: &retryable{err: errors.New("HTTP 429")}}
	case response.StatusCode == http.StatusUnauthorized || response.StatusCode == http.StatusForbidden:
		return nil, 0, &Error{Message: "Match syncing isn't set up correctly on this server.",
			Err: fmt.Errorf("HTTP %d", response.StatusCode)}
	case response.StatusCode >= 500:
		_, _ = io.Copy(io.Discard, response.Body)
		return nil, 0, &Error{Message: "The match data service is having trouble. Try again later.",
			Err: &retryable{err: fmt.Errorf("HTTP %d", response.StatusCode)}}
	default:
		return nil, 0, &Error{Message: "Couldn't get your matches. Try again later.",
			Err: fmt.Errorf("HTTP %d", response.StatusCode)}
	}
}
