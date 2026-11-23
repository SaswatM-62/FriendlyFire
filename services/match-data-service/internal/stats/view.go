package stats

import (
	"math"
	"sort"
	"strings"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
)

// Trade window for trade kills, traded deaths, KAST and trades_for (PM decision: 3 s).
const tradeWindowMS = 3000

// Spike timer: it explodes this long after the plant.
const spikeTimerMS = 45000

// matchView indexes one match for the stat rules: players and teams by ID, played rounds
// in order, and each round's kills in time order.
type matchView struct {
	m          henrik.Match
	players    map[string]*henrik.Player
	teamOf     map[string]string
	teams      map[string]*henrik.Team
	rounds     []*henrik.Round
	roundStats map[int]map[string]*henrik.RoundStats
	kills      map[int][]henrik.Kill
}

func newView(m henrik.Match) *matchView {
	v := &matchView{
		m:          m,
		players:    map[string]*henrik.Player{},
		teamOf:     map[string]string{},
		teams:      map[string]*henrik.Team{},
		roundStats: map[int]map[string]*henrik.RoundStats{},
		kills:      map[int][]henrik.Kill{},
	}
	for i := range m.Players {
		p := &m.Players[i]
		v.players[p.PUUID] = p
		v.teamOf[p.PUUID] = p.TeamID
	}
	for i := range m.Teams {
		v.teams[m.Teams[i].TeamID] = &m.Teams[i]
	}
	for i := range m.Rounds {
		r := &m.Rounds[i]
		if r.Result == "Surrendered" {
			continue
		}
		v.rounds = append(v.rounds, r)
		byPlayer := map[string]*henrik.RoundStats{}
		for j := range r.Stats {
			byPlayer[r.Stats[j].Player.PUUID] = &r.Stats[j]
		}
		v.roundStats[r.ID] = byPlayer
	}
	sort.Slice(v.rounds, func(i, j int) bool { return v.rounds[i].ID < v.rounds[j].ID })
	for _, k := range m.Kills {
		v.kills[k.Round] = append(v.kills[k.Round], k)
	}
	for round := range v.kills {
		sort.SliceStable(v.kills[round], func(i, j int) bool {
			return v.kills[round][i].TimeInRoundInMS < v.kills[round][j].TimeInRoundInMS
		})
	}
	return v
}

// played says whether a round ID is a played round (not a surrender placeholder).
func (v *matchView) played(round int) bool {
	_, ok := v.roundStats[round]
	return ok
}

// enemyKill: the killer is a match player on the other team from the victim. Self-kills,
// teammate kills and kills the spike was credited for aren't kills.
func (v *matchView) enemyKill(k henrik.Kill) bool {
	killerTeam, ok := v.teamOf[k.Killer.PUUID]
	if !ok || k.Killer.PUUID == "" {
		return false
	}
	victimTeam, ok := v.teamOf[k.Victim.PUUID]
	return ok && killerTeam != victimTeam && v.played(k.Round)
}

// death: a kill event whose victim is the player, except deaths to the spike.
func (v *matchView) death(k henrik.Kill, puuid string) bool {
	return k.Victim.PUUID == puuid && weaponType(k) != "Bomb" && v.played(k.Round)
}

// side is the team's side in a round, "attack" or "defense", from the winner and its role;
// empty when the round doesn't say.
func (v *matchView) side(team string, r *henrik.Round) string {
	var role string
	switch strings.ToLower(r.WinningTeamRole) {
	case "attacker", "attack":
		role = "attack"
	case "defender", "defense":
		role = "defense"
	}
	if role == "" || r.WinningTeam == "" {
		return ""
	}
	if team == r.WinningTeam {
		return role
	}
	if role == "attack" {
		return "defense"
	}
	return "attack"
}

// half is "first" (rounds 1–12), "second" (13–24) or "" (overtime).
func half(roundID int) string {
	switch {
	case roundID < 12:
		return "first"
	case roundID < 24:
		return "second"
	default:
		return ""
	}
}

func pistolRound(roundID int) bool {
	return roundID == 0 || roundID == 12
}

// earliestEnemyKill is the round's first enemy kill, and whether another enemy kill shares
// its time (then first_deaths leaves the round out).
func (v *matchView) earliestEnemyKill(round int) (henrik.Kill, bool, bool) {
	var first henrik.Kill
	found, tied := false, false
	for _, k := range v.kills[round] {
		if !v.enemyKill(k) {
			continue
		}
		if !found {
			first, found = k, true
			continue
		}
		if k.TimeInRoundInMS == first.TimeInRoundInMS {
			tied = true
		}
		break
	}
	return first, tied, found
}

// damageDealt is the player's damage in a round under the damage rule: every damage event
// except the spike's 999 against themselves.
func damageDealt(s *henrik.RoundStats, puuid string) int {
	total := 0
	for _, e := range s.DamageEvents {
		if e.Player.PUUID == puuid && e.Damage >= 999 {
			continue
		}
		total += e.Damage
	}
	return total
}

// damagedVictim says whether the player damaged someone in a round.
func (v *matchView) damagedVictim(round int, attacker, victim string) bool {
	s := v.roundStats[round][attacker]
	if s == nil {
		return false
	}
	for _, e := range s.DamageEvents {
		if e.Player.PUUID == victim && e.Damage > 0 {
			return true
		}
	}
	return false
}

func weaponType(k henrik.Kill) string {
	if k.Weapon.Type == nil {
		return ""
	}
	return *k.Weapon.Type
}

func weaponID(k henrik.Kill) string {
	if k.Weapon.ID == nil {
		return ""
	}
	return strings.ToLower(*k.Weapon.ID)
}

func weaponNamed(k henrik.Kill) bool {
	return k.Weapon.Name != nil && *k.Weapon.Name != ""
}

func locationOf(locations []henrik.PlayerLocation, puuid string) (henrik.Location, bool) {
	for _, l := range locations {
		if l.Player.PUUID == puuid {
			return l.Location, true
		}
	}
	return henrik.Location{}, false
}

// metres between two map positions (game units are centimetres).
func metres(a, b henrik.Location) float64 {
	return math.Hypot(a.X-b.X, a.Y-b.Y) / 100
}
