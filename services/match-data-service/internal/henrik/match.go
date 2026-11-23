// Package henrik holds the HenrikDev v4 match format (GET /valorant/v4/...matches...).
// Fields that can be null are pointers or slices; docs/stats-catalogue.md says what each
// one means and which quirks to expect.
package henrik

import "time"

// Response is one page of matches.
type Response struct {
	Status int     `json:"status"`
	Data   []Match `json:"data"`
}

// Match is one match, `M` in the stats catalogue.
type Match struct {
	Metadata Metadata `json:"metadata"`
	Players  []Player `json:"players"`
	Teams    []Team   `json:"teams"`
	Rounds   []Round  `json:"rounds"`
	Kills    []Kill   `json:"kills"`
}

type Metadata struct {
	MatchID        string    `json:"match_id"`
	Map            IDName    `json:"map"`
	GameVersion    string    `json:"game_version"`
	GameLengthInMS *int64    `json:"game_length_in_ms"`
	StartedAt      string    `json:"started_at"`
	IsCompleted    bool      `json:"is_completed"`
	Queue          Queue     `json:"queue"`
	Season         Season    `json:"season"`
	Platform       string    `json:"platform"`
	Region         string    `json:"region"`
	Cluster        string    `json:"cluster"`
	MVP            *Person   `json:"mvp"`
	PartyPenalties []Penalty `json:"party_rr_penaltys"`
}

// Started parses StartedAt.
func (m Metadata) Started() (time.Time, error) {
	return time.Parse(time.RFC3339Nano, m.StartedAt)
}

type IDName struct {
	ID   string `json:"id"`
	Name string `json:"name"`
}

type Queue struct {
	ID       string `json:"id"`
	Name     string `json:"name"`
	ModeType string `json:"mode_type"`
}

type Season struct {
	ID    string `json:"id"`
	Short string `json:"short"`
}

type Penalty struct {
	PartyID string  `json:"party_id"`
	Penalty float64 `json:"penalty"`
}

// Person is how events refer to a player.
type Person struct {
	PUUID string `json:"puuid"`
	Name  string `json:"name"`
	Tag   string `json:"tag"`
	Team  string `json:"team"`
}

type Player struct {
	PUUID               string         `json:"puuid"`
	Name                string         `json:"name"`
	Tag                 string         `json:"tag"`
	TeamID              string         `json:"team_id"`
	TeamNumber          *int           `json:"team_number"`
	Platform            string         `json:"platform"`
	PartyID             string         `json:"party_id"`
	Agent               IDName         `json:"agent"`
	Stats               PlayerStats    `json:"stats"`
	Performance         *Performance   `json:"performance"`
	AbilityCasts        *AbilityCasts  `json:"ability_casts"`
	Tier                Tier           `json:"tier"`
	Customization       *Customization `json:"customization"`
	AccountLevel        *int           `json:"account_level"`
	SessionPlaytimeInMS *int64         `json:"session_playtime_in_ms"`
	Behavior            *Behavior      `json:"behavior"`
	Economy             *Economy       `json:"economy"`
}

type PlayerStats struct {
	Score     int    `json:"score"`
	Kills     int    `json:"kills"`
	Deaths    int    `json:"deaths"`
	Assists   int    `json:"assists"`
	Headshots int    `json:"headshots"`
	Bodyshots int    `json:"bodyshots"`
	Legshots  int    `json:"legshots"`
	Damage    Damage `json:"damage"`
}

type Damage struct {
	Dealt    int `json:"dealt"`
	Received int `json:"received"`
}

type Performance struct {
	Score     *float64  `json:"score"`
	Breakdown Breakdown `json:"breakdown"`
	Ratings   Ratings   `json:"ratings"`
}

type Breakdown struct {
	Damage         *float64 `json:"damage"`
	AdjustedKills  *float64 `json:"adjusted_kills"`
	AdjustedDeaths *float64 `json:"adjusted_deaths"`
	Trades         *float64 `json:"trades"`
	Assists        *float64 `json:"assists"`
	UtilityUsage   *float64 `json:"utility_usage"`
	Plants         *float64 `json:"plants"`
	Defuses        *float64 `json:"defuses"`
}

type Ratings struct {
	Grade   *string           `json:"grade"`
	Combat  map[string]string `json:"combat"`
	Utility map[string]string `json:"utility"`
}

type AbilityCasts struct {
	Grenade  *int `json:"grenade"`
	Ability1 *int `json:"ability1"`
	Ability2 *int `json:"ability2"`
	Ultimate *int `json:"ultimate"`
}

type Tier struct {
	ID   int    `json:"id"`
	Name string `json:"name"`
}

type Customization struct {
	Card                 string     `json:"card"`
	Title                string     `json:"title"`
	PreferredLevelBorder string     `json:"preferred_level_border"`
	Bloomline            *Bloomline `json:"bloomline"`
}

type Bloomline struct {
	Level             *int   `json:"level"`
	PlankBackgroundID string `json:"plank_background_id"`
	HypeID            string `json:"hype_id"`
	ClipID            string `json:"clip_id"`
}

type Behavior struct {
	AFKRounds     *float64      `json:"afk_rounds"`
	FriendlyFire  *FriendlyFire `json:"friendly_fire"`
	RoundsInSpawn *float64      `json:"rounds_in_spawn"`
}

type FriendlyFire struct {
	Incoming *float64 `json:"incoming"`
	Outgoing *float64 `json:"outgoing"`
}

type Economy struct {
	Spent        Amount `json:"spent"`
	LoadoutValue Amount `json:"loadout_value"`
}

type Amount struct {
	Overall *int     `json:"overall"`
	Average *float64 `json:"average"`
}

type Team struct {
	TeamID    string     `json:"team_id"`
	Placement *int       `json:"placement"`
	MVP       *Person    `json:"mvp"`
	Rounds    TeamRounds `json:"rounds"`
	Won       *bool      `json:"won"`
}

type TeamRounds struct {
	Won  int `json:"won"`
	Lost int `json:"lost"`
}

// Round is one round, `R` in the stats catalogue. Result "Surrendered" marks a
// placeholder that wasn't played.
type Round struct {
	ID              int          `json:"id"`
	Result          string       `json:"result"`
	Ceremony        string       `json:"ceremony"`
	WinningTeam     string       `json:"winning_team"`
	WinningTeamRole string       `json:"winning_team_role"`
	FirstBlood      *Person      `json:"first_blood"`
	CeremonyPlayer  *Person      `json:"ceremony_player"`
	CeremonyTeam    string       `json:"ceremony_team"`
	Plant           *SpikeEvent  `json:"plant"`
	Defuse          *SpikeEvent  `json:"defuse"`
	Stats           []RoundStats `json:"stats"`
}

type SpikeEvent struct {
	RoundTimeInMS   int64            `json:"round_time_in_ms"`
	Site            string           `json:"site"`
	Location        *Location        `json:"location"`
	Player          *Person          `json:"player"`
	PlayerLocations []PlayerLocation `json:"player_locations"`
}

// RoundStats is one player's round, `S` in the stats catalogue.
type RoundStats struct {
	Player          Person        `json:"player"`
	DamageEvents    []DamageEvent `json:"damage_events"`
	Stats           RoundScore    `json:"stats"`
	Economy         RoundEconomy  `json:"economy"`
	WasAFK          bool          `json:"was_afk"`
	ReceivedPenalty bool          `json:"received_penalty"`
	StayedInSpawn   bool          `json:"stayed_in_spawn"`
}

// DamageEvent is damage the round's player dealt to Player.
type DamageEvent struct {
	Player    Person `json:"player"`
	Bodyshots int    `json:"bodyshots"`
	Headshots int    `json:"headshots"`
	Legshots  int    `json:"legshots"`
	Damage    int    `json:"damage"`
}

type RoundScore struct {
	Score     int `json:"score"`
	Kills     int `json:"kills"`
	Headshots int `json:"headshots"`
	Bodyshots int `json:"bodyshots"`
	Legshots  int `json:"legshots"`
}

type RoundEconomy struct {
	LoadoutValue *int    `json:"loadout_value"`
	Remaining    *int    `json:"remaining"`
	Weapon       *Weapon `json:"weapon"`
	Armor        *IDName `json:"armor"`
}

// Kill is one kill event, `K` in the stats catalogue. Round is the round's ID.
type Kill struct {
	TimeInRoundInMS   int64            `json:"time_in_round_in_ms"`
	TimeInMatchInMS   int64            `json:"time_in_match_in_ms"`
	Round             int              `json:"round"`
	Killer            Person           `json:"killer"`
	Victim            Person           `json:"victim"`
	Assistants        []Person         `json:"assistants"`
	Location          *Location        `json:"location"`
	Weapon            Weapon           `json:"weapon"`
	SecondaryFireMode bool             `json:"secondary_fire_mode"`
	PlayerLocations   []PlayerLocation `json:"player_locations"`
}

// Weapon is a kill's weapon. Name and ID can be null or empty (see the catalogue's quirks).
type Weapon struct {
	ID   *string `json:"id"`
	Name *string `json:"name"`
	Type *string `json:"type"`
}

type Location struct {
	X float64 `json:"x"`
	Y float64 `json:"y"`
}

type PlayerLocation struct {
	Player   Person   `json:"player"`
	Location Location `json:"location"`
}
