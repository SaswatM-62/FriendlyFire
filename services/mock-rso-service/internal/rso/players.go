package rso

import (
	"context"
	"encoding/json"
	"errors"
	"fmt"
	"os"
	"strings"

	"github.com/SaswatM-62/FriendlyFire/services/mock-rso-service/internal/lookup"
)

// Player is a test player someone can "sign in" as. The JSON names are Riot's, plus
// Email: only the FriendlyFire account with this email can link the player (the
// mock's stand-in for signing in to Riot). Players without one can't be linked.
// PUUID and Region may be left out of the file: LoadPlayers looks them up.
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

// AccountLookup finds the PUUID and region of a Riot ID (lookup.HenrikDev).
type AccountLookup interface {
	Account(ctx context.Context, gameName, tagLine string) (lookup.Account, error)
}

// Loaded is the test players file after looking up missing PUUIDs.
type Loaded struct {
	Players []Player
	// LookedUp counts the players whose PUUID came from the lookup.
	LookedUp int
	// LeftOut are players without a PUUID that couldn't be looked up; the rest still load.
	LeftOut []LeftOut
}

// LeftOut is a player whose PUUID couldn't be looked up, for the startup log.
type LeftOut struct {
	Position int
	RiotID   string
	Reason   string
}

// LoadPlayers reads the test players from a JSON file (see testplayers.example.json).
// A player without a puuid gets theirs, and an empty region, from accounts (nil when
// HENRIKDEV_API_KEY isn't set). A puuid in the file is never looked up: the fixtures
// need their placeholder PUUIDs.
func LoadPlayers(ctx context.Context, path string, accounts AccountLookup) (Loaded, error) {
	file, err := os.Open(path)
	if err != nil {
		return Loaded{}, fmt.Errorf("test players: %w", err)
	}
	defer func() { _ = file.Close() }()

	var players []Player
	decoder := json.NewDecoder(file)
	decoder.DisallowUnknownFields()
	if err := decoder.Decode(&players); err != nil {
		return Loaded{}, fmt.Errorf("test players in %s: %w", path, err)
	}
	if len(players) == 0 {
		return Loaded{}, fmt.Errorf("test players in %s: the list is empty", path)
	}

	var loaded Loaded
	var kept []numbered
	for i, player := range players {
		position := i + 1
		if player.PUUID == "" && player.GameName != "" && player.TagLine != "" {
			found, reason := lookUp(ctx, accounts, player)
			if reason != "" {
				loaded.LeftOut = append(loaded.LeftOut, LeftOut{Position: position, RiotID: player.RiotID(), Reason: reason})
				continue
			}
			player.PUUID = found.PUUID
			if player.Region == "" {
				player.Region = found.Region
			}
			loaded.LookedUp++
		}
		kept = append(kept, numbered{position: position, player: player})
	}
	if len(kept) == 0 {
		return Loaded{}, fmt.Errorf("test players in %s: none could be loaded (%d without a PUUID that couldn't be looked up)", path, len(loaded.LeftOut))
	}
	if err := validatePlayers(kept); err != nil {
		return Loaded{}, fmt.Errorf("test players in %s: %w", path, err)
	}
	for _, entry := range kept {
		loaded.Players = append(loaded.Players, entry.player)
	}
	return loaded, nil
}

// lookUp returns the player's account, or why it couldn't be found.
func lookUp(ctx context.Context, accounts AccountLookup, player Player) (lookup.Account, string) {
	if accounts == nil {
		return lookup.Account{}, "no puuid, and HENRIKDEV_API_KEY isn't set to look it up"
	}
	found, err := accounts.Account(ctx, player.GameName, player.TagLine)
	switch {
	case errors.Is(err, lookup.ErrNotFound):
		return lookup.Account{}, "HenrikDev has no account with this Riot ID (check the spelling and the tag)"
	case err != nil:
		return lookup.Account{}, err.Error()
	}
	return found, ""
}

// numbered keeps a player's place in the file, so errors name the right one.
type numbered struct {
	position int
	player   Player
}

// validatePlayers names players by position: PUUIDs don't belong in logs.
func validatePlayers(players []numbered) error {
	seen := make(map[string]bool, len(players))
	riotIDs := make(map[string]bool, len(players))
	for _, entry := range players {
		player, n := entry.player, entry.position
		if player.PUUID == "" || player.GameName == "" || player.TagLine == "" || player.Region == "" {
			return fmt.Errorf("player %d needs a puuid, gameName, tagLine and region", n)
		}
		if seen[player.PUUID] {
			return fmt.Errorf("player %d has the same puuid as an earlier player", n)
		}
		seen[player.PUUID] = true
		// Riot IDs aren't case-sensitive, so the sign-in page couldn't tell these apart.
		riotID := strings.ToLower(player.RiotID())
		if riotIDs[riotID] {
			return fmt.Errorf("player %d has the same Riot ID as an earlier player", n)
		}
		riotIDs[riotID] = true
		if player.Email != "" && !strings.Contains(player.Email, "@") {
			return fmt.Errorf("player %d has an email without an @", n)
		}
	}
	return nil
}
