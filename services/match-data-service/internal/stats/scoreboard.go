package stats

import (
	"strings"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
)

// toppedStats are the stats with topped_match_<stat> and topped_team_<stat> flags.
var toppedStats = []string{
	"performance_score", "kills", "deaths", "assists", "headshots", "bodyshots", "legshots", "damage_dealt",
	"damage_received", "combat_score", "acs", "adr", "first_bloods", "first_deaths", "plants", "defuses",
	"headshot_percentage", "max_round_damage", "max_round_kills",
}

// scoreboard holds every match player's comparable stats, linked or not. It's only used
// to compare; nothing about other players is kept.
type scoreboard struct {
	v      *matchView
	values map[string]map[string]float64
}

func newScoreboard(v *matchView) *scoreboard {
	b := &scoreboard{v: v, values: map[string]map[string]float64{}}
	firstDeaths := map[string]int{}
	for _, r := range v.rounds {
		if first, tied, ok := v.earliestEnemyKill(r.ID); ok && !tied {
			firstDeaths[first.Victim.PUUID]++
		}
	}
	for i := range v.m.Players {
		p := &v.m.Players[i]
		s := p.Stats
		values := map[string]float64{
			"kills": float64(s.Kills), "deaths": float64(s.Deaths), "assists": float64(s.Assists),
			"headshots": float64(s.Headshots), "bodyshots": float64(s.Bodyshots), "legshots": float64(s.Legshots),
			"damage_dealt": float64(s.Damage.Dealt), "damage_received": float64(s.Damage.Received),
			"combat_score": float64(s.Score), "first_deaths": float64(firstDeaths[p.PUUID]),
		}
		if p.Performance != nil && p.Performance.Score != nil {
			values["performance_score"] = *p.Performance.Score
		}
		if played := len(v.rounds); played > 0 {
			values["acs"] = float64(s.Score) / float64(played)
			values["adr"] = float64(s.Damage.Dealt) / float64(played)
		}
		if hits := s.Headshots + s.Bodyshots + s.Legshots; hits > 0 {
			values["headshot_percentage"] = 100 * float64(s.Headshots) / float64(hits)
		}
		var firstBloods, plants, defuses, maxDamage, maxKills float64
		for _, r := range v.rounds {
			if r.FirstBlood != nil && r.FirstBlood.PUUID == p.PUUID {
				firstBloods++
			}
			if r.Plant != nil && r.Plant.Player != nil && r.Plant.Player.PUUID == p.PUUID {
				plants++
			}
			if r.Defuse != nil && r.Defuse.Player != nil && r.Defuse.Player.PUUID == p.PUUID {
				defuses++
			}
			if rs := v.roundStats[r.ID][p.PUUID]; rs != nil {
				damage := 0
				for _, e := range rs.DamageEvents {
					if team, ok := v.teamOf[e.Player.PUUID]; ok && team != p.TeamID {
						damage += e.Damage
					}
				}
				maxDamage = max(maxDamage, float64(damage))
			}
			kills := 0
			for _, k := range v.kills[r.ID] {
				if v.enemyKill(k) && k.Killer.PUUID == p.PUUID {
					kills++
				}
			}
			maxKills = max(maxKills, float64(kills))
		}
		values["first_bloods"], values["plants"], values["defuses"] = firstBloods, plants, defuses
		values["max_round_damage"], values["max_round_kills"] = maxDamage, maxKills
		b.values[p.PUUID] = values
	}
	return b
}

// compare sets the player's positions, MVP flags and topped flags. A comparison where
// anyone's value is unknown is left out.
func (b *scoreboard) compare(p *henrik.Player, n numbers) {
	all := b.v.m.Players
	var team []henrik.Player
	for _, o := range all {
		if o.TeamID == p.TeamID {
			team = append(team, o)
		}
	}
	if position, ok := b.position(p.PUUID, all); ok {
		n.set("scoreboard_position", float64(position))
	}
	if position, ok := b.position(p.PUUID, team); ok {
		n.set("team_position", float64(position))
	}
	if t := b.v.teams[p.TeamID]; t != nil {
		n.int("team_placement", t.Placement)
		if t.MVP != nil {
			n.flag("team_mvp", t.MVP.PUUID == p.PUUID)
		}
	}
	if mvp := b.v.m.Metadata.MVP; mvp != nil && mvp.PUUID != "" {
		n.flag("match_mvp", mvp.PUUID == p.PUUID)
	}
	for _, stat := range toppedStats {
		if topped, ok := b.topped(p.PUUID, stat, all); ok {
			n.flag("topped_match_"+stat, topped)
		}
		if topped, ok := b.topped(p.PUUID, stat, team); ok {
			n.flag("topped_team_"+stat, topped)
		}
	}
}

// position is 1 + the number of players with a higher Performance Score (ties share).
func (b *scoreboard) position(puuid string, among []henrik.Player) (int, bool) {
	mine, ok := b.values[puuid]["performance_score"]
	if !ok {
		return 0, false
	}
	position := 1
	for _, o := range among {
		theirs, ok := b.values[o.PUUID]["performance_score"]
		if !ok {
			return 0, false
		}
		if theirs > mine {
			position++
		}
	}
	return position, true
}

// topped: the player has the highest value among these players (every tied leader counts).
func (b *scoreboard) topped(puuid, stat string, among []henrik.Player) (bool, bool) {
	mine, ok := b.values[puuid][stat]
	if !ok {
		return false, false
	}
	for _, o := range among {
		theirs, ok := b.values[o.PUUID][stat]
		if !ok {
			return false, false
		}
		if theirs > mine {
			return false, true
		}
	}
	return true, true
}

// teamShared computes the stats teammates share within a match.
func (b *builder) teamShared() {
	v, n, myTeam := b.v, b.n, b.p.TeamID
	for _, key := range []string{
		"rounds_won_attack", "rounds_won_defense", "rounds_played_attack", "rounds_played_defense",
		"rounds_won_first_half", "rounds_won_second_half", "rounds_played_first_half", "rounds_played_second_half",
		"pistol_rounds_won", "eco_rounds_won", "flawless_rounds", "thrifty_rounds", "team_ace_rounds",
	} {
		n.set(key, 0)
	}
	mine, theirs, deficit := 0, 0, 0
	for _, r := range v.rounds {
		won := r.WinningTeam == myTeam
		if side := v.side(myTeam, r); side != "" {
			n.add("rounds_played_"+side, 1)
			if won {
				n.add("rounds_won_"+side, 1)
			}
		}
		if h := half(r.ID); h != "" {
			n.add("rounds_played_"+h+"_half", 1)
			if won {
				n.add("rounds_won_"+h+"_half", 1)
			}
		}
		if won && pistolRound(r.ID) {
			n.add("pistol_rounds_won", 1)
		}
		if won && !pistolRound(r.ID) {
			if ours, opp, ok := b.loadouts(r); ok && ours < 5000 && opp >= 15000 {
				n.add("eco_rounds_won", 1)
			}
		}
		if r.CeremonyTeam == myTeam {
			switch r.Ceremony {
			case "CeremonyFlawless":
				n.add("flawless_rounds", 1)
			case "CeremonyThrifty":
				n.add("thrifty_rounds", 1)
			case "CeremonyTeamAce":
				n.add("team_ace_rounds", 1)
			}
		}
		if r.WinningTeam != "" {
			if won {
				mine++
			} else {
				theirs++
			}
			deficit = max(deficit, theirs-mine)
		}
	}
	n.set("max_deficit", float64(deficit))
	n.flag("overtime", len(v.rounds) > 24)

	parties := map[string]int{}
	opponentParties := map[string]int{}
	var tiers, opponentTiers []int
	highestLevel := -1
	for _, o := range v.m.Players {
		if o.PartyID != "" {
			parties[o.PartyID]++
			if o.TeamID != myTeam {
				opponentParties[o.PartyID]++
			}
		}
		if o.Tier.ID > 0 {
			tiers = append(tiers, o.Tier.ID)
			if o.TeamID != myTeam {
				opponentTiers = append(opponentTiers, o.Tier.ID)
			}
		}
		if o.TeamID != myTeam && o.AccountLevel != nil {
			highestLevel = max(highestLevel, *o.AccountLevel)
		}
	}
	if b.p.PartyID != "" {
		n.set("party_size", float64(parties[b.p.PartyID]))
	}
	biggest := 0
	for _, size := range opponentParties {
		biggest = max(biggest, size)
	}
	if biggest > 0 {
		n.set("biggest_opponent_party", float64(biggest))
	}
	if highestLevel >= 0 {
		n.set("highest_level_opponent", float64(highestLevel))
	}
	if b.competitive() {
		if len(tiers) > 0 {
			n.set("lobby_avg_tier", average(tiers))
		}
		if len(opponentTiers) > 0 {
			highest := 0
			for _, t := range opponentTiers {
				highest = max(highest, t)
			}
			n.set("highest_opponent_tier", float64(highest))
			n.set("avg_opponent_tier", average(opponentTiers))
		}
	}
}

// loadouts are the two teams' total loadout values in a round.
func (b *builder) loadouts(r *henrik.Round) (ours, theirs int, ok bool) {
	for puuid, s := range b.v.roundStats[r.ID] {
		if s.Economy.LoadoutValue == nil {
			return 0, 0, false
		}
		if b.v.teamOf[puuid] == b.p.TeamID {
			ours += *s.Economy.LoadoutValue
		} else {
			theirs += *s.Economy.LoadoutValue
		}
	}
	return ours, theirs, true
}

func average(values []int) float64 {
	total := 0
	for _, v := range values {
		total += v
	}
	return float64(total) / float64(len(values))
}

func lower(s string) string {
	return strings.ToLower(s)
}
