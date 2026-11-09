// Package config reads mock-rso-service's settings from environment variables
// (see .env.example at the repo root).
package config

import (
	"errors"
	"fmt"
	"net/url"
	"os"
	"strconv"
	"strings"
)

const (
	// Where the test players come from when MOCK_RSO_PLAYERS_FILE is empty, relative to
	// the service folder: your own list if you made one (gitignored), otherwise the
	// committed example.
	localPlayersFile   = "testplayers.json"
	examplePlayersFile = "testplayers.example.json"

	defaultRedirectURIs = "http://localhost:3000/link/riot/callback,http://localhost:8080/api/link/riot/callback"
)

// Config is everything mock-rso-service reads from the environment.
type Config struct {
	HTTPPort int
	// RedirectURIs are the only places /authorize may send the browser back to.
	RedirectURIs []string
	PlayersFile  string
}

// Load reads the config; getenv is os.Getenv outside tests.
func Load(getenv func(string) string) (Config, error) {
	port := 8090
	if value := getenv("HTTP_PORT"); value != "" {
		var err error
		if port, err = strconv.Atoi(value); err != nil {
			return Config{}, fmt.Errorf("HTTP_PORT must be a number, got %q", value)
		}
	}
	rawURIs := getenv("MOCK_RSO_REDIRECT_URIS")
	if rawURIs == "" {
		rawURIs = defaultRedirectURIs
	}
	redirectURIs, err := parseRedirectURIs(rawURIs)
	if err != nil {
		return Config{}, fmt.Errorf("MOCK_RSO_REDIRECT_URIS: %w", err)
	}
	return Config{HTTPPort: port, RedirectURIs: redirectURIs, PlayersFile: playersFile(getenv)}, nil
}

// parseRedirectURIs reads a comma-separated list of absolute http(s) URLs.
func parseRedirectURIs(raw string) ([]string, error) {
	var uris []string
	for _, part := range strings.Split(raw, ",") {
		uri := strings.TrimSpace(part)
		if uri == "" {
			continue
		}
		parsed, err := url.Parse(uri)
		if err != nil {
			return nil, fmt.Errorf("%q is not a valid URL", uri)
		}
		if (parsed.Scheme != "http" && parsed.Scheme != "https") || parsed.Host == "" || parsed.Fragment != "" {
			return nil, fmt.Errorf("%q must be an absolute http(s) URL without a fragment", uri)
		}
		uris = append(uris, uri)
	}
	if len(uris) == 0 {
		return nil, errors.New("must list at least one URL")
	}
	return uris, nil
}

func playersFile(getenv func(string) string) string {
	if value := getenv("MOCK_RSO_PLAYERS_FILE"); value != "" {
		return value
	}
	if _, err := os.Stat(localPlayersFile); err == nil {
		return localPlayersFile
	}
	return examplePlayersFile
}
