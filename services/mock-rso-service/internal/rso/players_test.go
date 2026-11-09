package rso_test

import (
	"os"
	"path/filepath"
	"testing"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/rso"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func writePlayers(t *testing.T, content string) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), "players.json")
	require.NoError(t, os.WriteFile(path, []byte(content), 0o600))
	return path
}

func TestTheCommittedExampleFileIsValid(t *testing.T) {
	players, err := rso.LoadPlayers("../../testplayers.example.json")

	require.NoError(t, err)
	assert.Len(t, players, 4)
	assert.Equal(t, "Sam", players[0].GameName)
}

func TestLoadPlayersReadsEveryField(t *testing.T) {
	path := writePlayers(t, `[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na"}]`)

	players, err := rso.LoadPlayers(path)

	require.NoError(t, err)
	assert.Equal(t, []rso.Player{{PUUID: "p1", GameName: "Sam", TagLine: "NA1", Region: "na"}}, players)
}

func TestLoadPlayersRejectsABadFile(t *testing.T) {
	cases := map[string]struct{ content, wantErr string }{
		"not JSON":      {`nope`, "invalid character"},
		"not a list":    {`{"players": []}`, "cannot unmarshal"},
		"empty list":    {`[]`, "the list is empty"},
		"unknown field": {`[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na", "rank": "Iron"}]`, "unknown field"},
		"missing field": {`[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1"}]`, "player 1 needs"},
		"duplicate":     {`[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na"}, {"puuid": "p1", "gameName": "Alex", "tagLine": "EUW", "region": "eu"}]`, "player 2 has the same puuid"},
		"empty name":    {`[{"puuid": "p1", "gameName": "", "tagLine": "NA1", "region": "na"}]`, "player 1 needs"},
		"second is bad": {`[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na"}, {"puuid": "p2"}]`, "player 2 needs"},
	}
	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			_, err := rso.LoadPlayers(writePlayers(t, tc.content))

			require.ErrorContains(t, err, tc.wantErr)
		})
	}
}

func TestLoadPlayersErrorsNeverContainAPUUID(t *testing.T) {
	path := writePlayers(t, `[{"puuid": "secret-puuid", "gameName": "Sam", "tagLine": "NA1", "region": "na"}, {"puuid": "secret-puuid", "gameName": "Alex", "tagLine": "EUW", "region": "eu"}]`)

	_, err := rso.LoadPlayers(path)

	require.Error(t, err)
	assert.NotContains(t, err.Error(), "secret-puuid")
}

func TestLoadPlayersRejectsAMissingFile(t *testing.T) {
	_, err := rso.LoadPlayers(filepath.Join(t.TempDir(), "nope.json"))

	require.ErrorContains(t, err, "nope.json")
}
