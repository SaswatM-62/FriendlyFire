package config

import (
	"os"
	"testing"

	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func env(values map[string]string) func(string) string {
	return func(key string) string { return values[key] }
}

func TestLoadUsesDefaultsWhenNothingIsSet(t *testing.T) {
	t.Chdir(t.TempDir())

	cfg, err := Load(env(nil))

	require.NoError(t, err)
	assert.Equal(t, 8090, cfg.HTTPPort)
	assert.Equal(t, []string{"http://localhost:3000/link/riot/callback"}, cfg.RedirectURIs)
	assert.Equal(t, "testplayers.example.json", cfg.PlayersFile)
}

func TestLoadReadsEverySetting(t *testing.T) {
	cfg, err := Load(env(map[string]string{
		"HTTP_PORT":              "9000",
		"MOCK_RSO_REDIRECT_URIS": " https://app.example.com/cb , http://localhost:3000/cb?x=1,",
		"MOCK_RSO_PLAYERS_FILE":  "/config/players.json",
	}))

	require.NoError(t, err)
	assert.Equal(t, 9000, cfg.HTTPPort)
	assert.Equal(t, []string{"https://app.example.com/cb", "http://localhost:3000/cb?x=1"}, cfg.RedirectURIs)
	assert.Equal(t, "/config/players.json", cfg.PlayersFile)
}

func TestLoadRejectsANonNumericPort(t *testing.T) {
	_, err := Load(env(map[string]string{"HTTP_PORT": "ninety"}))

	require.ErrorContains(t, err, "HTTP_PORT")
}

func TestLoadRejectsBadRedirectURIs(t *testing.T) {
	cases := map[string]string{
		"only separators": " , ",
		"relative":        "/link/riot/callback",
		"no scheme":       "localhost:3000/callback",
		"another scheme":  "javascript://app.test/alert(1)",
		"no host":         "http:///callback",
		"a fragment":      "http://app.test/callback#top",
		"not a URL":       "http://app.test/%zz",
		"one of many":     "http://app.test/callback,ftp://app.test/callback",
	}
	for name, value := range cases {
		t.Run(name, func(t *testing.T) {
			_, err := Load(env(map[string]string{"MOCK_RSO_REDIRECT_URIS": value}))

			require.ErrorContains(t, err, "MOCK_RSO_REDIRECT_URIS")
		})
	}
}

func TestLoadPrefersYourOwnPlayersFileOverTheExample(t *testing.T) {
	t.Chdir(t.TempDir())
	require.NoError(t, os.WriteFile("testplayers.json", []byte("[]"), 0o600))

	cfg, err := Load(env(nil))

	require.NoError(t, err)
	assert.Equal(t, "testplayers.json", cfg.PlayersFile)
}

func TestLoadPrefersTheConfiguredPlayersFileOverYourOwn(t *testing.T) {
	t.Chdir(t.TempDir())
	require.NoError(t, os.WriteFile("testplayers.json", []byte("[]"), 0o600))

	cfg, err := Load(env(map[string]string{"MOCK_RSO_PLAYERS_FILE": "other.json"}))

	require.NoError(t, err)
	assert.Equal(t, "other.json", cfg.PlayersFile)
}
