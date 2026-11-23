// Command refresh-lookups rewrites internal/lookup/data from valorant-api.com. Run it from
// the service folder after a patch adds an agent or a weapon:
//
//	go run ./cmd/refresh-lookups
package main

import (
	"encoding/json"
	"fmt"
	"net/http"
	"os"
	"path/filepath"
	"strings"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/lookup"
)

const api = "https://valorant-api.com/v1"

func main() {
	if err := run(); err != nil {
		fmt.Fprintln(os.Stderr, err)
		os.Exit(1)
	}
}

func run() error {
	client := &http.Client{Timeout: 30 * time.Second}
	var agents struct {
		Data []struct {
			UUID        string `json:"uuid"`
			DisplayName string `json:"displayName"`
			Role        struct {
				DisplayName string `json:"displayName"`
			} `json:"role"`
			Abilities []struct {
				Slot        string `json:"slot"`
				DisplayName string `json:"displayName"`
			} `json:"abilities"`
		} `json:"data"`
	}
	if err := get(client, api+"/agents?isPlayableCharacter=true", &agents); err != nil {
		return err
	}
	var weapons struct {
		Data []struct {
			UUID        string `json:"uuid"`
			DisplayName string `json:"displayName"`
			Category    string `json:"category"`
		} `json:"data"`
	}
	if err := get(client, api+"/weapons", &weapons); err != nil {
		return err
	}

	slots := map[string]string{"Grenade": "grenade", "Ability1": "ability1", "Ability2": "ability2", "Ultimate": "ultimate"}
	agentTable := map[string]lookup.Agent{}
	for _, a := range agents.Data {
		abilities := map[string]string{}
		for _, ability := range a.Abilities {
			if slot, ok := slots[ability.Slot]; ok {
				abilities[slot] = ability.DisplayName
			}
		}
		agentTable[strings.ToLower(a.UUID)] = lookup.Agent{Name: a.DisplayName, Role: a.Role.DisplayName, Abilities: abilities}
	}
	weaponTable := map[string]lookup.Weapon{}
	for _, w := range weapons.Data {
		weaponTable[strings.ToLower(w.UUID)] = lookup.Weapon{
			Name:  w.DisplayName,
			Class: strings.TrimPrefix(w.Category, "EEquippableCategory::"),
		}
	}
	for id, gun := range lookup.AbilityGuns {
		weaponTable[id] = gun
	}
	if err := write(filepath.Join("internal", "lookup", "data", "agents.json"), agentTable); err != nil {
		return err
	}
	return write(filepath.Join("internal", "lookup", "data", "weapons.json"), weaponTable)
}

func get(client *http.Client, url string, into any) error {
	response, err := client.Get(url)
	if err != nil {
		return err
	}
	defer func() { _ = response.Body.Close() }()
	if response.StatusCode != http.StatusOK {
		return fmt.Errorf("%s: HTTP %d", url, response.StatusCode)
	}
	return json.NewDecoder(response.Body).Decode(into)
}

func write(path string, table any) error {
	body, err := json.MarshalIndent(table, "", "  ")
	if err != nil {
		return err
	}
	return os.WriteFile(path, append(body, '\n'), 0o644)
}
