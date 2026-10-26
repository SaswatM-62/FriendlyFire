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
}

func TestLoadRejectsANonNumericPort(t *testing.T) {
	env := map[string]string{"GRPC_PORT": "ninety"}

	_, err := Load(func(key string) string { return env[key] })

	require.ErrorContains(t, err, "GRPC_PORT")
}
