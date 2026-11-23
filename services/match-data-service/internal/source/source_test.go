package source

import (
	"context"
	"encoding/json"
	"net/http"
	"net/http/httptest"
	"os"
	"strconv"
	"strings"
	"sync/atomic"
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/ratelimit"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

const (
	testKey = "HDEV-secret-test-key"
	sam     = "00000000-0000-4000-8000-000000000001"
)

// fakeHenrik serves the fixture matches like HenrikDev's v4 by-puuid endpoint: filtered by
// player and queue, newest first, paged by start and size.
type fakeHenrik struct {
	matches  []henrik.Match
	requests atomic.Int32
	// before answers the first requests with these statuses (e.g. 429) before working.
	before []int
}

func newFakeHenrik(t *testing.T) (*fakeHenrik, *httptest.Server) {
	t.Helper()
	fixtures, err := NewFixtures(time.Now)
	require.NoError(t, err)
	fake := &fakeHenrik{matches: fixtures.matches}
	server := httptest.NewServer(fake)
	t.Cleanup(server.Close)
	return fake, server
}

func (f *fakeHenrik) ServeHTTP(w http.ResponseWriter, r *http.Request) {
	n := int(f.requests.Add(1))
	if r.Header.Get("Authorization") != testKey {
		w.WriteHeader(http.StatusUnauthorized)
		return
	}
	if n <= len(f.before) {
		status := f.before[n-1]
		if status == http.StatusTooManyRequests {
			w.Header().Set("Retry-After", "2")
			body, _ := os.ReadFile("fixtures/henrikdev/rate_limited.json")
			w.WriteHeader(status)
			_, _ = w.Write(body)
			return
		}
		w.WriteHeader(status)
		return
	}
	// /valorant/v4/by-puuid/matches/{region}/{platform}/{puuid}
	parts := strings.Split(r.URL.Path, "/")
	puuid := parts[len(parts)-1]
	if parts[len(parts)-2] != "pc" {
		w.WriteHeader(http.StatusBadRequest)
		return
	}
	start, _ := strconv.Atoi(r.URL.Query().Get("start"))
	size, _ := strconv.Atoi(r.URL.Query().Get("size"))
	var mine []henrik.Match
	for _, m := range f.matches {
		if m.Metadata.Queue.ID == r.URL.Query().Get("mode") && hasPlayer(m, puuid) {
			mine = append(mine, m)
		}
	}
	page := []henrik.Match{}
	if start < len(mine) {
		page = mine[start:min(start+size, len(mine))]
	}
	w.Header().Set("Content-Type", "application/json")
	_ = json.NewEncoder(w).Encode(henrik.Response{Status: 200, Data: page})
}

func newSource(server *httptest.Server, key string, clock ratelimit.Clock) *HenrikDevSource {
	return NewHenrikDev(server.Client(), server.URL, key, ratelimit.New(clock), clock)
}

func oldestStart(matches []henrik.Match) time.Time {
	oldest, _ := matches[len(matches)-1].Metadata.Started()
	return oldest
}

func TestHenrikDevPagesThroughEveryQueueNewestFirst(t *testing.T) {
	fake, server := newFakeHenrik(t)
	source := newSource(server, testKey, ratelimit.SystemClock{})

	result, err := source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, oldestStart(fake.matches))

	require.NoError(t, err)
	assert.Len(t, result.Matches, 20, "Sam played all 20 fixture matches")
	for i := 1; i < len(result.Matches); i++ {
		assert.GreaterOrEqual(t, result.Matches[i-1].Metadata.StartedAt, result.Matches[i].Metadata.StartedAt)
	}
	// Competitive: two full pages and an empty one; Unrated: one empty page.
	assert.Equal(t, 4, result.Requests)
	assert.EqualValues(t, 4, fake.requests.Load())
}

func TestHenrikDevStopsAtMatchesOlderThanSince(t *testing.T) {
	fake, server := newFakeHenrik(t)
	source := newSource(server, testKey, ratelimit.SystemClock{})
	cutoff, _ := fake.matches[4].Metadata.Started()

	result, err := source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, cutoff)

	require.NoError(t, err)
	assert.Len(t, result.Matches, 5)
	// The first page already reached older matches: no second Competitive page.
	assert.Equal(t, 2, result.Requests)
}

func TestHenrikDevWaitsOutA429(t *testing.T) {
	fake, server := newFakeHenrik(t)
	fake.before = []int{http.StatusTooManyRequests}
	clock := testutil.NewFakeClock(time.Date(2026, 11, 20, 12, 0, 0, 0, time.UTC))
	source := newSource(server, testKey, clock)

	done := make(chan Result, 1)
	errs := make(chan error, 1)
	go func() {
		result, err := source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, oldestStart(fake.matches))
		done <- result
		errs <- err
	}()
	// The 429 said Retry-After: 2, so every caller pauses for 2 seconds.
	clock.BlockUntil(t, 1)
	clock.Advance(2 * time.Second)

	select {
	case result := <-done:
		require.NoError(t, <-errs)
		assert.Len(t, result.Matches, 20)
		assert.Equal(t, 5, result.Requests, "the 429 counts as a request")
	case <-time.After(5 * time.Second):
		t.Fatal("the fetch didn't continue after the pause")
	}
}

func TestARejectedKeyIsASafeErrorWithoutTheKey(t *testing.T) {
	_, server := newFakeHenrik(t)
	source := newSource(server, "HDEV-wrong-key", ratelimit.SystemClock{})

	_, err := source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, time.Time{})

	require.Error(t, err)
	assert.Equal(t, "Match syncing isn't set up correctly on this server.", SafeMessage(err))
	assert.NotContains(t, err.Error(), "HDEV-wrong-key")
	assert.NotContains(t, err.Error(), server.URL)
}

func TestTheKeyNeverAppearsInAnError(t *testing.T) {
	fake, server := newFakeHenrik(t)
	fake.before = []int{500, 500, 500, 500}
	clock := testutil.NewFakeClock(time.Date(2026, 11, 20, 12, 0, 0, 0, time.UTC))
	source := newSource(server, testKey, clock)

	errs := make(chan error, 1)
	go func() {
		_, err := source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, time.Time{})
		errs <- err
	}()
	for i := 1; i <= 3; i++ {
		clock.BlockUntil(t, 1)
		clock.Advance(time.Duration(i) * 2 * time.Second)
	}
	err := <-errs

	require.Error(t, err)
	assert.Equal(t, "The match data service is having trouble. Try again later.", SafeMessage(err))
	assert.NotContains(t, err.Error(), testKey)
	assert.EqualValues(t, 4, fake.requests.Load(), "four tries, then give up")
}

func TestWithoutAKeyNothingIsCalled(t *testing.T) {
	fake, server := newFakeHenrik(t)
	source := newSource(server, "", ratelimit.SystemClock{})

	_, err := source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, time.Time{})

	assert.Equal(t, "Match syncing isn't set up on this server yet.", SafeMessage(err))
	assert.Zero(t, fake.requests.Load())
}

func TestFixturesServeThePlayersMatchesInsideTheLast30Days(t *testing.T) {
	now := time.Date(2027, 3, 1, 12, 0, 0, 0, time.UTC)
	fixtures, err := NewFixtures(func() time.Time { return now })
	require.NoError(t, err)

	result, err := fixtures.Matches(context.Background(), Player{PUUID: sam}, now.AddDate(0, 0, -30))

	require.NoError(t, err)
	assert.Len(t, result.Matches, 20)
	assert.Zero(t, result.Requests)
	newest, err := result.Matches[0].Metadata.Started()
	require.NoError(t, err)
	assert.Equal(t, now.Add(-24*time.Hour), newest, "moved so the newest match is a day old")

	riley, err := fixtures.Matches(context.Background(), Player{PUUID: "00000000-0000-4000-8000-000000000005"}, time.Time{})
	require.NoError(t, err)
	assert.Len(t, riley.Matches, 6)
	stranger, err := fixtures.Matches(context.Background(), Player{PUUID: "someone-else"}, time.Time{})
	require.NoError(t, err)
	assert.Empty(t, stranger.Matches)
}

func TestHealthReportsTheLastRequestWithoutCallingHenrikDev(t *testing.T) {
	fake, server := newFakeHenrik(t)
	clock := testutil.NewFakeClock(time.Date(2026, 11, 20, 12, 0, 0, 0, time.UTC))
	source := newSource(server, "HDEV-wrong-key", clock)
	require.NoError(t, source.Health(), "nothing known yet")

	_, _ = source.Matches(context.Background(), Player{PUUID: sam, Region: "na"}, time.Time{})

	require.EqualError(t, source.Health(), "Match syncing isn't set up correctly on this server.")
	clock.Advance(6 * time.Minute)
	require.NoError(t, source.Health(), "an old failure doesn't count")
	assert.EqualValues(t, 1, fake.requests.Load(), "Health never calls HenrikDev")
	assert.EqualError(t, newSource(server, "", clock).Health(), "no API key")
}
