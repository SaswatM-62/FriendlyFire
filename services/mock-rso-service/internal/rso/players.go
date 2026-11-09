package rso

import (
	"encoding/json"
	"errors"
	"fmt"
	"os"
)

// Player is a test player someone can "sign in" as. The JSON names are Riot's, and
// this is also the /riot/account/v1/accounts/me response.
type Player struct {
	PUUID    string `json:"puuid"`
	GameName string `json:"gameName"`
	TagLine  string `json:"tagLine"`
	Region   string `json:"region"`
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
	for i, player := range players {
		if player.PUUID == "" || player.GameName == "" || player.TagLine == "" || player.Region == "" {
			return fmt.Errorf("player %d needs a puuid, gameName, tagLine and region", i+1)
		}
		if seen[player.PUUID] {
			return fmt.Errorf("player %d has the same puuid as an earlier player", i+1)
		}
		seen[player.PUUID] = true
	}
	return nil
}
