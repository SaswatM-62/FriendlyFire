package rso

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"strings"
)

// Player is a test player someone can "sign in" as. The JSON names are Riot's, plus
// Email: only the FriendlyFire account with this email can link the player (the
// mock's stand-in for signing in to Riot). Players without one can't be linked.
type Player struct {
	PUUID    string `json:"puuid"`
	GameName string `json:"gameName"`
	TagLine  string `json:"tagLine"`
	Region   string `json:"region"`
	Email    string `json:"email,omitempty"`
}

// RiotID is "TheLaw#6391".
func (p Player) RiotID() string {
	return p.GameName + "#" + p.TagLine
}

// WithoutEmail counts the players nobody can link, for the startup log.
func WithoutEmail(players []Player) int {
	count := 0
	for _, player := range players {
		if player.Email == "" {
			count++
		}
	}
	return count
}

// LoadPlayers reads the test players from a JSON file (see testplayers.example.json).
func LoadPlayers(path string) ([]Player, error) {
	file, err := os.Open(path)
	if err != nil {
		return nil, fmt.Errorf("test players: %w", err)
	}
	defer func() { _ = file.Close() }()

	var players []Player
	decoder := json.NewDecoder(file)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&players); err != nil {
		return nil, fmt.Errorf("test players in %s: %w", path, err)
	}
	if err := validatePlayers(players); err != nil {
		return nil, fmt.Errorf("test players in %s: %w", path, err)
	}
	return players, nil
}

// validatePlayers names players by position: PUUIDs don't belong in logs.
func validatePlayers(players []Player) error {
	if len(players) == 0 {
		return errors.New("the list is empty")
	}
	seen := make(map[string]bool, len(players))
	riotIDs := make(map[string]bool, len(players))
	for i, player := range players {
		if player.PUUID == "" || player.GameName == "" || player.TagLine == "" || player.Region == "" {
			return fmt.Errorf("player %d needs a puuid, gameName, tagLine and region", i+1)
		}
		if seen[player.PUUID] {
			return fmt.Errorf("player %d has the same puuid as an earlier player", i+1)
		}
		seen[player.PUUID] = true
		// Riot IDs aren't case-sensitive, so the sign-in page couldn't tell these apart.
		riotID := strings.ToLower(player.RiotID())
		if riotIDs[riotID] {
			return fmt.Errorf("player %d has the same Riot ID as an earlier player", i+1)
		}
		riotIDs[riotID] = true
		if player.Email != "" && !strings.Contains(player.Email, "@") {
			return fmt.Errorf("player %d has an email without an @", i+1)
		}
	}
	return nil
}
