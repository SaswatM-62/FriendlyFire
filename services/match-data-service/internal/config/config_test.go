package config

import (
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func TestLoadUsesDefaultsWhenNothingIsSet(t *testing.T) {
	cfg, err := Load(func(string) string { return "" })

	require.NoError(t, err)
	assert.Equal(t, 9095, cfg.GRPCPort)
	assert.Equal(t, "postgres://postgres:@localhost:5432/friendlyfire?search_path=matchdata", cfg.Postgres.ConnString())
	assert.Empty(t, cfg.HenrikDevAPIKey)
	assert.Equal(t, "https://api.henrikdev.xyz", cfg.HenrikDevBaseURL)
}

func TestLoadRejectsANonNumericPort(t *testing.T) {
	env := map[string]string{"GRPC_PORT": "ninety"}

	_, err := Load(func(key string) string { return env[key] })

	require.ErrorContains(t, err, "GRPC_PORT")
}

func TestTheMatchSourceIsTheFixturesWithoutAKey(t *testing.T) {
	cases := map[string]struct {
		env  map[string]string
		want string
	}{
		"nothing set":           {map[string]string{}, SourceFixtures},
		"a key":                 {map[string]string{"HENRIKDEV_API_KEY": "k"}, SourceHenrikDev},
		"fixtures with a key":   {map[string]string{"HENRIKDEV_API_KEY": "k", "MATCH_SOURCE": "fixtures"}, SourceFixtures},
		"henrikdev without key": {map[string]string{"MATCH_SOURCE": "henrikdev"}, SourceHenrikDev},
	}
	for name, tc := range cases {
		t.Run(name, func(t *testing.T) {
			cfg, err := Load(func(key string) string { return tc.env[key] })
			require.NoError(t, err)
			assert.Equal(t, tc.want, cfg.MatchSource)
		})
	}
}

func TestLoadRejectsAnUnknownMatchSource(t *testing.T) {
	_, err := Load(func(key string) string { return map[string]string{"MATCH_SOURCE": "riot"}[key] })

	require.ErrorContains(t, err, "MATCH_SOURCE")
}
