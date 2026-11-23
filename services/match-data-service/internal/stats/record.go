// Package stats turns a HenrikDev v4 match into one record per linked player, with the
// keys in docs/stats-catalogue.md. Everything is computed from the full match here, at
// sync time; only linked players' records are kept.
package stats

import (
	"fmt"
	"strings"
	"time"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/lookup"
)

// Game is the value of PlayerMatchStats.game for VALORANT.
const Game = "valorant"

// Record is one linked player's stats in one match (PlayerMatchStats).
type Record struct {
	UserID   string
	MatchID  string
	PlayedAt time.Time
	Won      bool
	Kills    int
	Deaths   int
	Assists  int
	Labels   map[string]string
	Numbers  map[string]float64
}

// Eligible: a complete Competitive or Unrated match. Everything else (Premier, Swiftplay,
// Deathmatch, custom games, unfinished matches) is dropped.
func Eligible(m henrik.Match) bool {
	queue := m.Metadata.Queue.ID
	return m.Metadata.IsCompleted && (queue == "competitive" || queue == "unrated")
}

// Records computes a record for every linked player in the match. linked maps a PUUID to
// the player's FriendlyFire user ID; other players are used for comparisons only.
func Records(m henrik.Match, linked map[string]string, tables lookup.Tables) ([]Record, error) {
	if !Eligible(m) {
		return nil, nil
	}
	playedAt, err := m.Metadata.Started()
	if err != nil {
		return nil, fmt.Errorf("match start time: %w", err)
	}
	v := newView(m)
	board := newScoreboard(v)
	var records []Record
	for i := range m.Players {
		p := &m.Players[i]
		userID, ok := linked[p.PUUID]
		if !ok {
			continue
		}
		team := v.teams[p.TeamID]
		won := team != nil && team.Won != nil && *team.Won
		r := Record{
			UserID:   userID,
			MatchID:  m.Metadata.MatchID,
			PlayedAt: playedAt.UTC(),
			Won:      won,
			Kills:    p.Stats.Kills,
			Deaths:   p.Stats.Deaths,
			Assists:  p.Stats.Assists,
			Labels:   map[string]string{},
			Numbers:  map[string]float64{},
		}
		b := &builder{v: v, p: p, team: team, tables: tables, linked: linked, labels: r.Labels, n: r.Numbers}
		b.labelsAndDirect()
		b.derived()
		b.teamShared()
		board.compare(p, r.Numbers)
		records = append(records, r)
	}
	return records, nil
}

// builder computes one player's record.
type builder struct {
	v      *matchView
	p      *henrik.Player
	team   *henrik.Team
	tables lookup.Tables
	linked map[string]string
	labels map[string]string
	n      numbers
}

type numbers map[string]float64

func (n numbers) set(key string, value float64) { n[key] = value }

func (n numbers) add(key string, value float64) { n[key] += value }

func (n numbers) int(key string, value *int) {
	if value != nil {
		n[key] = float64(*value)
	}
}

func (n numbers) int64(key string, value *int64) {
	if value != nil {
		n[key] = float64(*value)
	}
}

func (n numbers) float(key string, value *float64) {
	if value != nil {
		n[key] = *value
	}
}

// ratio sets a / b, leaving the key out when b is 0 (unknown, never infinity).
func (n numbers) ratio(key string, a, b float64) {
	if b != 0 {
		n[key] = a / b
	}
}

func (n numbers) flag(key string, on bool) {
	if on {
		n[key] = 1
	} else {
		n[key] = 0
	}
}

func setLabel(labels map[string]string, key, value string) {
	if value != "" {
		labels[key] = value
	}
}

func (b *builder) competitive() bool {
	return b.v.m.Metadata.Queue.ID == "competitive"
}

func (b *builder) labelsAndDirect() {
	m, p, l, n := b.v.m.Metadata, b.p, b.labels, b.n
	setLabel(l, "agent", p.Agent.Name)
	setLabel(l, "agent_id", p.Agent.ID)
	if agent, ok := b.tables.Agent(p.Agent.ID); ok {
		setLabel(l, "role", agent.Role)
	}
	setLabel(l, "map", m.Map.Name)
	setLabel(l, "map_id", m.Map.ID)
	setLabel(l, "mode", m.Queue.Name)
	setLabel(l, "queue_id", m.Queue.ID)
	setLabel(l, "mode_type", m.Queue.ModeType)
	setLabel(l, "team_id", p.TeamID)
	if b.competitive() && p.Tier.ID > 0 {
		setLabel(l, "rank", p.Tier.Name)
		n.set("rank_tier", float64(p.Tier.ID))
	}
	setLabel(l, "season", m.Season.Short)
	setLabel(l, "season_id", m.Season.ID)
	setLabel(l, "region", m.Region)
	setLabel(l, "cluster", m.Cluster)
	setLabel(l, "platform", p.Platform)
	setLabel(l, "match_platform", m.Platform)
	setLabel(l, "game_version", m.GameVersion)
	if t := b.team; t != nil && t.Won != nil {
		switch {
		case *t.Won:
			l["result"] = "win"
		case t.Rounds.Won == t.Rounds.Lost:
			l["result"] = "draw"
		case t.Rounds.Won < t.Rounds.Lost:
			l["result"] = "loss"
		}
	}
	if len(b.v.rounds) > 0 && b.v.rounds[0].ID == 0 {
		setLabel(l, "starting_side", b.v.side(p.TeamID, b.v.rounds[0]))
	}

	s := p.Stats
	n.set("headshots", float64(s.Headshots))
	n.set("bodyshots", float64(s.Bodyshots))
	n.set("legshots", float64(s.Legshots))
	n.set("damage_dealt", float64(s.Damage.Dealt))
	n.set("damage_received", float64(s.Damage.Received))
	n.set("combat_score", float64(s.Score))
	if t := b.team; t != nil {
		n.set("rounds_won", float64(t.Rounds.Won))
		n.set("rounds_lost", float64(t.Rounds.Lost))
		n.set("rounds_played", float64(t.Rounds.Won+t.Rounds.Lost))
	}
	n.int64("match_length_ms", m.GameLengthInMS)
	n.int("account_level", p.AccountLevel)
	n.int("team_number", p.TeamNumber)
	n.int64("session_playtime_ms", p.SessionPlaytimeInMS)
	if c := p.AbilityCasts; c != nil {
		n.int("ability_casts_grenade", c.Grenade)
		n.int("ability_casts_ability1", c.Ability1)
		n.int("ability_casts_ability2", c.Ability2)
		n.int("ability_casts_ultimate", c.Ultimate)
	}
	if e := p.Economy; e != nil {
		n.int("credits_spent", e.Spent.Overall)
		n.float("credits_spent_average", e.Spent.Average)
		n.int("loadout_value", e.LoadoutValue.Overall)
		n.float("loadout_value_average", e.LoadoutValue.Average)
	}

	// Provider fields: Riot's Performance Score and its inputs, ratings, behaviour, cosmetics.
	if perf := p.Performance; perf != nil {
		n.float("performance_score", perf.Score)
		bd := perf.Breakdown
		n.float("performance_damage", bd.Damage)
		n.float("performance_adjusted_kills", bd.AdjustedKills)
		n.float("performance_adjusted_deaths", bd.AdjustedDeaths)
		n.float("performance_trades", bd.Trades)
		n.float("performance_assists", bd.Assists)
		n.float("performance_utility_usage", bd.UtilityUsage)
		n.float("performance_plants", bd.Plants)
		n.float("performance_defuses", bd.Defuses)
		if perf.Ratings.Grade != nil {
			setLabel(l, "performance_grade", *perf.Ratings.Grade)
		}
		for key, value := range perf.Ratings.Combat {
			setLabel(l, "performance_combat_"+key, value)
		}
		for key, value := range perf.Ratings.Utility {
			// assists, defuses, plants, utility_usage → performance_utility_assists ... _usage
			setLabel(l, "performance_utility_"+strings.TrimPrefix(key, "utility_"), value)
		}
	}
	if bh := p.Behavior; bh != nil {
		n.float("afk_rounds", bh.AFKRounds)
		n.float("rounds_in_spawn", bh.RoundsInSpawn)
		if ff := bh.FriendlyFire; ff != nil {
			n.float("friendly_fire_incoming", ff.Incoming)
			n.float("friendly_fire_outgoing", ff.Outgoing)
		}
	}
	if c := p.Customization; c != nil {
		setLabel(l, "card_id", c.Card)
		setLabel(l, "title_id", c.Title)
		setLabel(l, "preferred_level_border_id", c.PreferredLevelBorder)
		if bl := c.Bloomline; bl != nil {
			setLabel(l, "bloomline_clip_id", bl.ClipID)
			setLabel(l, "bloomline_hype_id", bl.HypeID)
			setLabel(l, "bloomline_plank_background_id", bl.PlankBackgroundID)
			n.int("bloomline_level", bl.Level)
		}
	}
}
