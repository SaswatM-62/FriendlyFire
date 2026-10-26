// Package source fetches players' matches from outside APIs: HenrikDev in
// development, Riot's API in production (RiotSource, later).
package source

// MatchSource fetches one player's matches from an outside API.
type MatchSource interface {
	// TODO(ff): fetch a player's matches from an outside API
	//   - list matches for a PUUID and region, newest first, optionally only after a
	//     given match or time (the first sync covers 30 days)
	//   - return game-neutral stats: kills/deaths/assists, won, plus labels (agent, map,
	//     mode) and numbers (headshots, damage, combat score, rounds...)
	//   - respect ratelimit (HenrikDev allows 30 requests per minute)
}

// HenrikDevSource is the MatchSource for the HenrikDev API.
type HenrikDevSource struct {
	// TODO(ff): MatchSource for the HenrikDev API (HENRIKDEV_API_KEY)
	//   - an *http.Client and base URL injected through the constructor, so tests can
	//     point it at an httptest server serving testdata/henrikdev/*.json
	//   - never log or return the API key
}
