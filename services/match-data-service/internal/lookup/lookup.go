// Package lookup holds the tables match-data-service owns: agents (role and the ability in
// each slot) and weapons (name, class, and the slot of an agent's ability gun). They come
// from valorant-api.com; refresh them each patch with `go run ./cmd/refresh-lookups`.
package lookup

import (
	_ "embed"
	"encoding/json"
	"fmt"
	"strings"
)

// Agent is one playable agent.
type Agent struct {
	Name string `json:"name"`
	// Role is Duelist, Initiator, Controller or Sentinel.
	Role string `json:"role"`
	// Abilities names the ability in each slot: grenade, ability1, ability2, ultimate.
	Abilities map[string]string `json:"abilities"`
}

// Weapon is a gun, the knife, or an agent's ability gun.
type Weapon struct {
	Name string `json:"name"`
	// Class is Rifle, SMG, Sniper, Shotgun, Heavy, Sidearm or Melee; empty for ability guns.
	Class string `json:"class,omitempty"`
	// AbilitySlot is set for ability guns (Headhunter: ability1; Tour De Force and
	// Overdrive: ultimate), which count as ability kills.
	AbilitySlot string `json:"ability_slot,omitempty"`
}

// Tables are both tables, keyed by lower-case ID.
type Tables struct {
	Agents  map[string]Agent
	Weapons map[string]Weapon
}

//go:embed data/agents.json
var agentsJSON []byte

//go:embed data/weapons.json
var weaponsJSON []byte

// Load reads the embedded tables.
func Load() (Tables, error) {
	var tables Tables
	if err := json.Unmarshal(agentsJSON, &tables.Agents); err != nil {
		return Tables{}, fmt.Errorf("agents table: %w", err)
	}
	if err := json.Unmarshal(weaponsJSON, &tables.Weapons); err != nil {
		return Tables{}, fmt.Errorf("weapons table: %w", err)
	}
	return tables, nil
}

// Agent finds an agent by ID.
func (t Tables) Agent(id string) (Agent, bool) {
	agent, ok := t.Agents[strings.ToLower(id)]
	return agent, ok
}

// Weapon finds a weapon by ID.
func (t Tables) Weapon(id string) (Weapon, bool) {
	weapon, ok := t.Weapons[strings.ToLower(id)]
	return weapon, ok
}
