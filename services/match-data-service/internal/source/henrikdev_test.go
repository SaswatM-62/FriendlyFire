package source

import (
	"net/http"
	"net/http/httptest"
	"os"
	"testing"
)

// Example of how HenrikDevSource will be tested: serve a saved response with httptest.
// Tests never call the real API.
func TestHenrikDevSourceReadsSavedMatches(t *testing.T) {
	t.Skip("TODO(ff): enable when HenrikDevSource exists and testdata/henrikdev has a saved response")

	matches, err := os.ReadFile("testdata/henrikdev/matches.json")
	if err != nil {
		t.Fatal(err)
	}
	api := httptest.NewServer(http.HandlerFunc(func(w http.ResponseWriter, r *http.Request) {
		w.Header().Set("Content-Type", "application/json")
		_, _ = w.Write(matches)
	}))
	defer api.Close()

	// source := NewHenrikDevSource(api.Client(), api.URL, "test-key")
	// stats, err := source.Matches(ctx, "puuid", "na", since)
	// require.NoError(t, err) and check the parsed stats.
}
