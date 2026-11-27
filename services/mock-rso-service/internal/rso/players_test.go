package rso_test

import (
	"context"
	"errors"
	"os"
	"path/filepath"
	"testing"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/lookup"
	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/rso"
	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/testutil"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func writePlayers(t *testing.T, content string) string {
	t.Helper()
	path := filepath.Join(t.TempDir(), "players.json")
	require.NoError(t, os.WriteFile(path, []byte(content), 0o600))
	return path
}

// load reads a players file with no account lookup (HENRIKDEV_API_KEY not set).
func load(path string) ([]rso.Player, error) {
	loaded, err := rso.LoadPlayers(context.Background(), path, nil)
	return loaded.Players, err
}

func TestTheCommittedExampleFileIsValid(t *testing.T) {
	players, err := load("../../testplayers.example.json")

	require.NoError(t, err)
	assert.Len(t, players, 5, "Sam, Alex, Kim, Jordan and Riley: the 5-stack in match-data-service's fixtures")
	assert.Equal(t, "Sam", players[0].GameName)
	assert.Zero(t, rso.WithoutEmail(players), "every example player has an email")
}

func TestLoadPlayersReadsEveryField(t *testing.T) {
	path := writePlayers(t, `[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na", "email": "sam@example.test"}]`)

	players, err := load(path)

	require.NoError(t, err)
	assert.Equal(t, []rso.Player{{PUUID: "p1", GameName: "Sam", TagLine: "NA1", Region: "na", Email: "sam@example.test"}}, players)
}

func TestAPlayerWithoutAnEmailLoadsButIsCounted(t *testing.T) {
	path := writePlayers(t, `[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na"}]`)

	players, err := load(path)

	require.NoError(t, err)
	assert.Equal(t, 1, rso.WithoutEmail(players))
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
		"same Riot ID":  {`[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na"}, {"puuid": "p2", "gameName": "sam", "tagLine": "na1", "region": "na"}]`, "player 2 has the same Riot ID"},
		"bad email":     {`[{"puuid": "p1", "gameName": "Sam", "tagLine": "NA1", "region": "na", "email": "sam"}]`, "player 1 has an email without an @"},
	}
	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			_, err := load(writePlayers(t, tc.content))

			require.ErrorContains(t, err, tc.wantErr)
		})
	}
}

func TestLoadPlayersErrorsNeverContainAPUUID(t *testing.T) {
	path := writePlayers(t, `[{"puuid": "secret-puuid", "gameName": "Sam", "tagLine": "NA1", "region": "na"}, {"puuid": "secret-puuid", "gameName": "Alex", "tagLine": "EUW", "region": "eu"}]`)

	_, err := load(path)

	require.Error(t, err)
	assert.NotContains(t, err.Error(), "secret-puuid")
}

func TestLoadPlayersRejectsAMissingFile(t *testing.T) {
	_, err := load(filepath.Join(t.TempDir(), "nope.json"))

	require.ErrorContains(t, err, "nope.json")
}

func TestAMissingPUUIDAndRegionAreLookedUpFromTheRiotID(t *testing.T) {
	path := writePlayers(t, `[{"gameName": "Sam", "tagLine": "NA1", "email": "sam@example.test"}]`)
	accounts := &testutil.FakeAccounts{Accounts: map[string]lookup.Account{"sam#na1": {PUUID: "p-sam", Region: "na"}}}

	loaded, err := rso.LoadPlayers(context.Background(), path, accounts)

	require.NoError(t, err)
	assert.Equal(t, []rso.Player{{PUUID: "p-sam", GameName: "Sam", TagLine: "NA1", Region: "na", Email: "sam@example.test"}}, loaded.Players)
	assert.Equal(t, 1, loaded.LookedUp)
	assert.Equal(t, []string{"Sam#NA1"}, accounts.Calls)
}

func TestAPUUIDInTheFileIsNeverLookedUp(t *testing.T) {
	// The fixtures' players need their placeholder PUUIDs, whatever HenrikDev says.
	path := writePlayers(t, `[{"puuid": "00000000-0000-4000-8000-000000000001", "gameName": "Sam", "tagLine": "NA1", "region": "na"}]`)
	accounts := &testutil.FakeAccounts{Accounts: map[string]lookup.Account{"sam#na1": {PUUID: "real", Region: "eu"}}}

	loaded, err := rso.LoadPlayers(context.Background(), path, accounts)

	require.NoError(t, err)
	assert.Equal(t, "00000000-0000-4000-8000-000000000001", loaded.Players[0].PUUID)
	assert.Zero(t, loaded.LookedUp)
	assert.Empty(t, accounts.Calls)
}

func TestARegionInTheFileWinsOverTheLookedUpOne(t *testing.T) {
	path := writePlayers(t, `[{"gameName": "Sam", "tagLine": "NA1", "region": "latam"}]`)
	accounts := &testutil.FakeAccounts{Accounts: map[string]lookup.Account{"sam#na1": {PUUID: "p-sam", Region: "na"}}}

	loaded, err := rso.LoadPlayers(context.Background(), path, accounts)

	require.NoError(t, err)
	assert.Equal(t, "latam", loaded.Players[0].Region)
}

func TestAPlayerWhoseLookupFailsIsLeftOutAndTheRestLoad(t *testing.T) {
	path := writePlayers(t, `[
		{"gameName": "Typo", "tagLine": "NA1"},
		{"puuid": "p-alex", "gameName": "Alex", "tagLine": "EUW", "region": "eu"},
		{"gameName": "Kim", "tagLine": "KR1"},
		{"gameName": "Sam", "tagLine": "NA1"}
	]`)
	accounts := &testutil.FakeAccounts{
		Accounts: map[string]lookup.Account{"sam#na1": {PUUID: "p-sam", Region: "na"}},
		Errors:   map[string]error{"Kim#KR1": errors.New("HenrikDev answered HTTP 503")},
	}

	loaded, err := rso.LoadPlayers(context.Background(), path, accounts)

	require.NoError(t, err)
	require.Len(t, loaded.Players, 2)
	assert.Equal(t, "Alex", loaded.Players[0].GameName)
	assert.Equal(t, "Sam", loaded.Players[1].GameName)
	assert.Equal(t, []rso.LeftOut{
		{Position: 1, RiotID: "Typo#NA1", Reason: "HenrikDev has no account with this Riot ID (check the spelling and the tag)"},
		{Position: 3, RiotID: "Kim#KR1", Reason: "HenrikDev answered HTTP 503"},
	}, loaded.LeftOut)
}

func TestWithoutAnAPIKeyAPlayerWithoutAPUUIDIsLeftOut(t *testing.T) {
	path := writePlayers(t, `[{"gameName": "Sam", "tagLine": "NA1"}, {"puuid": "p-alex", "gameName": "Alex", "tagLine": "EUW", "region": "eu"}]`)

	loaded, err := rso.LoadPlayers(context.Background(), path, nil)

	require.NoError(t, err)
	assert.Len(t, loaded.Players, 1)
	require.Len(t, loaded.LeftOut, 1)
	assert.Contains(t, loaded.LeftOut[0].Reason, "HENRIKDEV_API_KEY")
}

func TestAFileWhereNobodyCanBeLoadedIsRejected(t *testing.T) {
	path := writePlayers(t, `[{"gameName": "Sam", "tagLine": "NA1"}]`)

	_, err := rso.LoadPlayers(context.Background(), path, nil)

	require.ErrorContains(t, err, "none could be loaded")
}

func TestErrorsStillNameThePlayersPlaceInTheFile(t *testing.T) {
	// Player 1 is left out, so the duplicate is the file's player 3, not the second loaded.
	path := writePlayers(t, `[
		{"gameName": "Typo", "tagLine": "NA1"},
		{"puuid": "p1", "gameName": "Alex", "tagLine": "EUW", "region": "eu"},
		{"puuid": "p1", "gameName": "Kim", "tagLine": "KR1", "region": "ap"}
	]`)

	_, err := rso.LoadPlayers(context.Background(), path, &testutil.FakeAccounts{})

	require.ErrorContains(t, err, "player 3 has the same puuid")
}

func TestALookedUpPUUIDThatsAlreadyInTheFileIsADuplicate(t *testing.T) {
	path := writePlayers(t, `[{"puuid": "p-sam", "gameName": "Sam", "tagLine": "NA1", "region": "na"}, {"gameName": "SamAlt", "tagLine": "NA2"}]`)
	accounts := &testutil.FakeAccounts{Accounts: map[string]lookup.Account{"samalt#na2": {PUUID: "p-sam", Region: "na"}}}

	_, err := rso.LoadPlayers(context.Background(), path, accounts)

	require.ErrorContains(t, err, "player 2 has the same puuid")
	assert.NotContains(t, err.Error(), "p-sam")
}
