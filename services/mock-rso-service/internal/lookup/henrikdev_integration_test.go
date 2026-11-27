//go:build integration

package lookup_test

import (
	"context"
	"net/http"
	"net/http/httptest"
	"testing"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/lookup"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

// henrik serves one canned answer and remembers the request it got.
func henrik(t *testing.T, status int, body string) (*lookup.HenrikDev, *http.Request) {
	t.Helper()
	seen := &http.Request{}
	server := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		*seen = *r.Clone(context.Background())
		w.WriteHeader(status)
		_, _ = w.Write([]byte(body))
	}))
	t.Cleanup(server.Close)
	return lookup.NewHenrikDev(server.URL+"/", "HDEV-test-key", &http.Client{Timeout: 5 * time.Second}), seen
}

func TestAccountReturnsThePUUIDAndRegion(t *testing.T) {
	client, seen := henrik(t, http.StatusOK, `{"status":200,"data":{"puuid":"a933bd2b-24a3-5e8d-9a94-403495f536cf","region":"ap","account_level":120,"name":"SHADOW影","tag":"4YT"}}`)

	account, err := client.Account(context.Background(), "SHADOW影", "4YT")

	require.NoError(t, err)
	assert.Equal(t, lookup.Account{PUUID: "a933bd2b-24a3-5e8d-9a94-403495f536cf", Region: "ap"}, account)
	assert.Equal(t, "/valorant/v2/account/SHADOW%E5%BD%B1/4YT", seen.URL.EscapedPath(), "name and tag are path-escaped")
	assert.Equal(t, "HDEV-test-key", seen.Header.Get("Authorization"))
}

func TestANameWithASpaceOrSlashStaysOneSegment(t *testing.T) {
	client, seen := henrik(t, http.StatusOK, `{"data":{"puuid":"p","region":"na"}}`)

	_, err := client.Account(context.Background(), "the law/sm", "CYPHR")

	require.NoError(t, err)
	assert.Equal(t, "/valorant/v2/account/the%20law%2Fsm/CYPHR", seen.URL.EscapedPath())
}

func TestAccountErrors(t *testing.T) {
	cases := map[string]struct {
		status  int
		body    string
		wantErr string
	}{
		"unknown Riot ID": {http.StatusNotFound, `{"errors":[{"message":"Not found"}]}`, lookup.ErrNotFound.Error()},
		"bad key":         {http.StatusUnauthorized, `{}`, "refused HENRIKDEV_API_KEY (HTTP 401)"},
		"forbidden":       {http.StatusForbidden, `{}`, "refused HENRIKDEV_API_KEY (HTTP 403)"},
		"rate limit":      {http.StatusTooManyRequests, `{}`, "rate limit"},
		"server error":    {http.StatusBadGateway, `{}`, "HTTP 502"},
		"not JSON":        {http.StatusOK, `<html>`, "couldn't read"},
		"no PUUID":        {http.StatusOK, `{"data":{"region":"na"}}`, "without a PUUID"},
	}
	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			client, _ := henrik(t, tc.status, tc.body)

			_, err := client.Account(context.Background(), "Sam", "NA1")

			require.ErrorContains(t, err, tc.wantErr)
		})
	}
}

func TestAnUnknownRiotIDIsErrNotFound(t *testing.T) {
	client, _ := henrik(t, http.StatusNotFound, `{}`)

	_, err := client.Account(context.Background(), "Typo", "NA1")

	assert.ErrorIs(t, err, lookup.ErrNotFound)
}

func TestAnUnreachableServerSaysSo(t *testing.T) {
	client := lookup.NewHenrikDev("http://127.0.0.1:1", "k", &http.Client{Timeout: time.Second})

	_, err := client.Account(context.Background(), "Sam", "NA1")

	require.ErrorContains(t, err, "couldn't reach HenrikDev")
}
