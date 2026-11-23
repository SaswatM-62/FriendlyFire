package stats

import (
	"math"
	"strconv"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
)

// Isolated deaths: no living teammate within this many metres (PM decision).
const isolatedMetres = 20

// derived computes the catalogue's derived numbers for one player, walking every played
// round once.
func (b *builder) derived() {
	v, p, n := b.v, b.p, b.n
	me, myTeam := p.PUUID, p.TeamID
	roundsPlayed, ok := n["rounds_played"]
	if !ok {
		roundsPlayed = float64(len(v.rounds))
	}
	s := p.Stats
	n.ratio("acs", float64(s.Score), roundsPlayed)
	n.ratio("adr", float64(s.Damage.Dealt), roundsPlayed)
	n.ratio("headshot_percentage", 100*float64(s.Headshots), float64(s.Headshots+s.Bodyshots+s.Legshots))
	n.ratio("kd_ratio", float64(s.Kills), float64(s.Deaths))
	n.ratio("kda_ratio", float64(s.Kills+s.Assists), float64(s.Deaths))
	n.set("damage_delta", float64(s.Damage.Dealt-s.Damage.Received))

	// Counts start at 0: the event data is complete, so a missing event is a real zero.
	for _, key := range []string{
		"first_bloods", "first_deaths", "kast_rounds", "trade_kills", "traded_deaths", "deathless_rounds",
		"killed_then_died", "round_winning_kills", "untouched_multikill_rounds", "double_kills_under_1s",
		"clutch_attempts", "clutch_wins", "aces", "clutches", "closers", "max_round_damage", "max_round_kills",
		"weapon_kills", "ability_kills", "ability_kills_grenade", "ability_kills_ability1",
		"ability_kills_ability2", "ability_kills_ultimate", "rifle_kills", "smg_kills", "sniper_kills",
		"shotgun_kills", "machine_gun_kills", "eco_pistol_kills", "picked_up_gun_kills", "knife_kills",
		"alt_fire_kills", "damage_assists", "utility_assists", "post_plant_kills", "outgunned_kills",
		"pistol_round_kills", "plants", "defuses", "ninja_defuses", "exit_kills", "spike_deaths", "fall_deaths",
		"penalty_rounds", "rounds_without_armor", "isolated_deaths",
		"attack_kills", "defense_kills", "first_half_kills", "second_half_kills",
		"attack_deaths", "defense_deaths", "first_half_deaths", "second_half_deaths",
		"attack_assists", "defense_assists", "first_half_assists", "second_half_assists",
		"attack_kast_rounds", "defense_kast_rounds", "first_half_kast_rounds", "second_half_kast_rounds",
		"attack_damage_dealt", "defense_damage_dealt",
		"attack_first_bloods", "defense_first_bloods", "attack_first_deaths", "defense_first_deaths",
	} {
		n.set(key, 0)
	}

	friends := b.friends()
	for _, f := range friends {
		n.set("assists_for:"+b.linked[f], 0)
		n.set("finishes_for:"+b.linked[f], 0)
		n.set("trades_for:"+b.linked[f], 0)
	}

	var (
		fastestFirstBlood                    *int64
		fastestPlant, closestDefuse          *int64
		deathStreak, bestDeathStreak         int
		killStreak, bestKillStreak           int
		maxRoundCombatScore                  *int
		unspentTotal, unspentRounds          int
		longestKill                          *float64
		killTimeTotal, killTimeCount         float64
		teamDistanceTotal, teamDistanceCount float64
	)

	for _, r := range v.rounds {
		side, h := v.side(myTeam, r), half(r.ID)
		split := func(stat string, amount float64) {
			if side != "" {
				n.add(side+"_"+stat, amount)
			}
			if h != "" {
				n.add(h+"_half_"+stat, amount)
			}
		}
		kills := v.kills[r.ID]
		myStats := v.roundStats[r.ID][me]

		var myKills, myDeaths []henrik.Kill
		assists := 0
		for _, k := range kills {
			if v.enemyKill(k) && k.Killer.PUUID == me {
				myKills = append(myKills, k)
			}
			if v.death(k, me) {
				myDeaths = append(myDeaths, k)
			}
			if b.assisted(k) {
				assists++
				if v.damagedVictim(r.ID, me, k.Victim.PUUID) {
					n.add("damage_assists", 1)
				} else {
					n.add("utility_assists", 1)
				}
				for _, f := range friends {
					if k.Killer.PUUID == f && v.enemyKill(k) {
						n.add("assists_for:"+b.linked[f], 1)
					}
				}
			}
		}
		split("kills", float64(len(myKills)))
		split("deaths", float64(len(myDeaths)))
		split("assists", float64(assists))

		// Trades, KAST and deaths.
		traded := false
		for _, d := range myDeaths {
			if v.enemyKill(d) && b.teammateKilledWithin(kills, d.Killer.PUUID, d.TimeInRoundInMS) {
				traded = true
				n.add("traded_deaths", 1)
			}
			if b.isolated(d) {
				n.add("isolated_deaths", 1)
			}
		}
		for _, k := range kills {
			if k.Victim.PUUID == me && weaponType(k) == "Bomb" && v.played(k.Round) {
				n.add("spike_deaths", 1)
			}
			if k.Victim.PUUID == me && weaponType(k) == "Fall" && v.played(k.Round) {
				n.add("fall_deaths", 1)
			}
		}
		if len(myKills) > 0 || assists > 0 || len(myDeaths) == 0 || traded {
			n.add("kast_rounds", 1)
			split("kast_rounds", 1)
		}
		if len(myDeaths) == 0 {
			n.add("deathless_rounds", 1)
			deathStreak++
		} else {
			deathStreak = 0
		}
		bestDeathStreak = max(bestDeathStreak, deathStreak)
		if len(myKills) > 0 {
			killStreak++
		} else {
			killStreak = 0
		}
		bestKillStreak = max(bestKillStreak, killStreak)

		// Kills in this round.
		n.set("max_round_kills", math.Max(n["max_round_kills"], float64(len(myKills))))
		n.add("rounds_with_kills:"+strconv.Itoa(len(myKills)), 1)
		for i, k := range myKills {
			if b.tradedTeammate(kills, k) {
				n.add("trade_kills", 1)
			}
			for _, d := range myDeaths {
				if gap := d.TimeInRoundInMS - k.TimeInRoundInMS; gap >= 0 && gap <= tradeWindowMS {
					n.add("killed_then_died", 1)
					break
				}
			}
			if i > 0 && k.TimeInRoundInMS-myKills[i-1].TimeInRoundInMS < 1000 {
				n.add("double_kills_under_1s", 1)
			}
			b.classify(k, r, myStats)
			if r.Plant != nil && k.TimeInRoundInMS > r.Plant.RoundTimeInMS {
				n.add("post_plant_kills", 1)
				if r.Result == "Detonate" && k.TimeInRoundInMS > r.Plant.RoundTimeInMS+spikeTimerMS {
					n.add("exit_kills", 1)
				}
			}
			if victim := v.roundStats[r.ID][k.Victim.PUUID]; myStats != nil && victim != nil &&
				myStats.Economy.LoadoutValue != nil && victim.Economy.LoadoutValue != nil &&
				*myStats.Economy.LoadoutValue < *victim.Economy.LoadoutValue {
				n.add("outgunned_kills", 1)
			}
			if pistolRound(r.ID) {
				n.add("pistol_round_kills", 1)
			}
			if from, ok := locationOf(k.PlayerLocations, me); ok && k.Location != nil {
				d := metres(from, *k.Location)
				if longestKill == nil || d > *longestKill {
					longestKill = &d
				}
				if nearest, ok := b.nearestTeammate(k.PlayerLocations, from); ok {
					teamDistanceTotal += nearest
					teamDistanceCount++
				}
			}
			killTimeTotal += float64(k.TimeInRoundInMS) / 1000
			killTimeCount++
			for _, f := range friends {
				if v.damagedVictim(r.ID, f, k.Victim.PUUID) {
					n.add("finishes_for:"+b.linked[f], 1)
				}
				if b.avenged(kills, f, k) {
					n.add("trades_for:"+b.linked[f], 1)
				}
			}
		}
		if len(myKills) >= 2 && !v.hitBy(r.ID, me) {
			n.add("untouched_multikill_rounds", 1)
		}
		if r.Result == "Elimination" && r.WinningTeam == myTeam {
			if last, ok := lastEnemyKill(v, kills); ok && last.Killer.PUUID == me {
				n.add("round_winning_kills", 1)
			}
		}
		if b.clutched(r, kills) {
			n.add("clutch_attempts", 1)
			if r.WinningTeam == myTeam {
				n.add("clutch_wins", 1)
			}
		}
		if cp := r.CeremonyPlayer; cp != nil && cp.PUUID == me {
			switch r.Ceremony {
			case "CeremonyAce":
				n.add("aces", 1)
			case "CeremonyClutch":
				n.add("clutches", 1)
			case "CeremonyCloser":
				n.add("closers", 1)
			}
		}

		// First bloods and first deaths.
		if fb := r.FirstBlood; fb != nil && fb.PUUID == me {
			n.add("first_bloods", 1)
			if side != "" {
				n.add(side+"_first_bloods", 1)
			}
			if first, _, ok := v.earliestEnemyKill(r.ID); ok {
				t := first.TimeInRoundInMS
				if fastestFirstBlood == nil || t < *fastestFirstBlood {
					fastestFirstBlood = &t
				}
			}
		}
		if first, tied, ok := v.earliestEnemyKill(r.ID); ok && !tied && first.Victim.PUUID == me {
			n.add("first_deaths", 1)
			if side != "" {
				n.add(side+"_first_deaths", 1)
			}
		}

		// The player's own round: damage, score, economy.
		if myStats != nil {
			n.set("max_round_damage", math.Max(n["max_round_damage"], float64(b.damageToOpponents(myStats))))
			dealt := float64(damageDealt(myStats, me))
			if side != "" {
				n.add(side+"_damage_dealt", dealt)
			}
			score := myStats.Stats.Score
			if maxRoundCombatScore == nil || score > *maxRoundCombatScore {
				maxRoundCombatScore = &score
			}
			if myStats.ReceivedPenalty {
				n.add("penalty_rounds", 1)
			}
			econ := myStats.Economy
			if econ.Remaining != nil {
				unspentTotal += *econ.Remaining
				unspentRounds++
			}
			if econ.Armor == nil {
				n.add("rounds_without_armor", 1)
			}
			if econ.Weapon != nil && econ.Weapon.ID != nil && *econ.Weapon.ID != "" {
				n.add("rounds_with_loadout:"+lower(*econ.Weapon.ID), 1)
			}
		}

		// The spike.
		if pl := r.Plant; pl != nil && pl.Player != nil && pl.Player.PUUID == me {
			n.add("plants", 1)
			if pl.Site != "" {
				n.add("plants_site_"+pl.Site, 1)
			}
			t := pl.RoundTimeInMS
			if fastestPlant == nil || t < *fastestPlant {
				fastestPlant = &t
			}
		}
		if df := r.Defuse; df != nil && df.Player != nil && df.Player.PUUID == me {
			n.add("defuses", 1)
			if r.Plant != nil {
				left := spikeTimerMS - (df.RoundTimeInMS - r.Plant.RoundTimeInMS)
				if closestDefuse == nil || left < *closestDefuse {
					closestDefuse = &left
				}
			}
			if b.attackerAlive(r, kills, df.RoundTimeInMS) {
				n.add("ninja_defuses", 1)
			}
		}
	}

	n.set("kast_pct", 0)
	n.ratio("kast_pct", 100*n["kast_rounds"], roundsPlayed)
	if roundsPlayed == 0 {
		delete(n, "kast_pct")
	}
	n.set("longest_deathless_streak", float64(bestDeathStreak))
	n.set("longest_kill_streak_rounds", float64(bestKillStreak))
	if fastestFirstBlood != nil {
		n.set("fastest_first_blood_ms", float64(*fastestFirstBlood))
	}
	if maxRoundCombatScore != nil {
		n.set("max_round_combat_score", float64(*maxRoundCombatScore))
	}
	if longestKill != nil {
		n.set("longest_kill_m", *longestKill)
	}
	n.ratio("avg_kill_time_s", killTimeTotal, killTimeCount)
	n.ratio("avg_distance_to_team_m", teamDistanceTotal, teamDistanceCount)
	if fastestPlant != nil {
		n.set("fastest_plant_ms", float64(*fastestPlant))
	}
	if closestDefuse != nil {
		n.set("closest_defuse_ms_left", float64(*closestDefuse))
	}
	n.ratio("avg_unspent_credits", float64(unspentTotal), float64(unspentRounds))
	if spent, ok := n["credits_spent"]; ok {
		n.ratio("damage_per_1000_credits", 1000*float64(s.Damage.Dealt), spent)
	}
	if c := p.AbilityCasts; c != nil && c.Grenade != nil && c.Ability1 != nil && c.Ability2 != nil {
		n.ratio("utility_per_round", float64(*c.Grenade+*c.Ability1+*c.Ability2), roundsPlayed)
	}
	b.opponents()
}

// assisted: the player is in the kill's assistants and the victim is on the other team
// (the assists rule, so an enemy who fell or died to the spike counts).
func (b *builder) assisted(k henrik.Kill) bool {
	if !b.v.played(k.Round) {
		return false
	}
	victimTeam, ok := b.v.teamOf[k.Victim.PUUID]
	if !ok || victimTeam == b.p.TeamID {
		return false
	}
	for _, a := range k.Assistants {
		if a.PUUID == b.p.PUUID {
			return true
		}
	}
	return false
}

// friends are the other linked players on the player's team.
func (b *builder) friends() []string {
	var friends []string
	for _, other := range b.v.m.Players {
		if other.PUUID != b.p.PUUID && other.TeamID == b.p.TeamID {
			if _, ok := b.linked[other.PUUID]; ok {
				friends = append(friends, other.PUUID)
			}
		}
	}
	return friends
}

// teammateKilledWithin: a teammate killed `killer` within the trade window after `at`.
func (b *builder) teammateKilledWithin(kills []henrik.Kill, killer string, at int64) bool {
	for _, k := range kills {
		if b.v.enemyKill(k) && k.Victim.PUUID == killer && k.Killer.PUUID != b.p.PUUID &&
			b.v.teamOf[k.Killer.PUUID] == b.p.TeamID &&
			k.TimeInRoundInMS >= at && k.TimeInRoundInMS-at <= tradeWindowMS {
			return true
		}
	}
	return false
}

// tradedTeammate: the player's kill was on an enemy who killed a teammate in the previous 3 s.
func (b *builder) tradedTeammate(kills []henrik.Kill, mine henrik.Kill) bool {
	for _, k := range kills {
		if b.v.enemyKill(k) && k.Killer.PUUID == mine.Victim.PUUID && k.Victim.PUUID != b.p.PUUID &&
			b.v.teamOf[k.Victim.PUUID] == b.p.TeamID &&
			k.TimeInRoundInMS <= mine.TimeInRoundInMS && mine.TimeInRoundInMS-k.TimeInRoundInMS <= tradeWindowMS {
			return true
		}
	}
	return false
}

// avenged: the player's kill was on the enemy who killed `friend` within the previous 3 s.
func (b *builder) avenged(kills []henrik.Kill, friend string, mine henrik.Kill) bool {
	for _, k := range kills {
		if b.v.enemyKill(k) && k.Victim.PUUID == friend && k.Killer.PUUID == mine.Victim.PUUID &&
			k.TimeInRoundInMS <= mine.TimeInRoundInMS && mine.TimeInRoundInMS-k.TimeInRoundInMS <= tradeWindowMS {
			return true
		}
	}
	return false
}

// classify counts a kill by weapon type: bought guns and the knife, abilities and ability guns.
func (b *builder) classify(k henrik.Kill, r *henrik.Round, myStats *henrik.RoundStats) {
	n := b.n
	id := weaponID(k)
	if k.SecondaryFireMode && weaponType(k) == "Weapon" && weaponNamed(k) {
		n.add("alt_fire_kills", 1)
	}
	switch weaponType(k) {
	case "Melee":
		n.add("weapon_kills", 1)
		n.add("knife_kills", 1)
		if id != "" {
			n.add("kills_with_weapon:"+id, 1)
		}
	case "Ability":
		n.add("ability_kills", 1)
		if slot := abilitySlot(id); slot != "" {
			n.add("ability_kills_"+slot, 1)
		}
	case "Weapon":
		if !weaponNamed(k) {
			// An ability gun (Headhunter, Tour De Force, Overdrive).
			n.add("ability_kills", 1)
			if gun, ok := b.tables.Weapon(id); ok && gun.AbilitySlot != "" {
				n.add("ability_kills_"+gun.AbilitySlot, 1)
			}
			return
		}
		n.add("weapon_kills", 1)
		if id != "" {
			n.add("kills_with_weapon:"+id, 1)
		}
		gun, known := b.tables.Weapon(id)
		if !known {
			return
		}
		if key, ok := classKeys[gun.Class]; ok {
			n.add(key, 1)
		}
		if gun.Class == "Sidearm" && !pistolRound(r.ID) {
			n.add("eco_pistol_kills", 1)
		}
		if gun.Class != "Sidearm" && gun.Class != "Melee" && myStats != nil && myStats.Economy.Weapon != nil &&
			myStats.Economy.Weapon.ID != nil && lower(*myStats.Economy.Weapon.ID) != id {
			n.add("picked_up_gun_kills", 1)
		}
	}
}

var classKeys = map[string]string{
	"Rifle":   "rifle_kills",
	"SMG":     "smg_kills",
	"Sniper":  "sniper_kills",
	"Shotgun": "shotgun_kills",
	"Heavy":   "machine_gun_kills",
}

func abilitySlot(id string) string {
	switch id {
	case "grenadeability":
		return "grenade"
	case "ability1":
		return "ability1"
	case "ability2":
		return "ability2"
	case "ultimate":
		return "ultimate"
	}
	return ""
}

// damageToOpponents is the player's damage to the other team in one round.
func (b *builder) damageToOpponents(s *henrik.RoundStats) int {
	total := 0
	for _, e := range s.DamageEvents {
		if team, ok := b.v.teamOf[e.Player.PUUID]; ok && team != b.p.TeamID {
			total += e.Damage
		}
	}
	return total
}

// hitBy: another player's damage event hit the player this round.
func (v *matchView) hitBy(round int, puuid string) bool {
	for attacker, s := range v.roundStats[round] {
		if attacker == puuid {
			continue
		}
		for _, e := range s.DamageEvents {
			if e.Player.PUUID == puuid && e.Damage > 0 {
				return true
			}
		}
	}
	return false
}

func lastEnemyKill(v *matchView, kills []henrik.Kill) (henrik.Kill, bool) {
	for i := len(kills) - 1; i >= 0; i-- {
		if v.enemyKill(kills[i]) {
			return kills[i], true
		}
	}
	return henrik.Kill{}, false
}

// clutched: at some point in the round the player was their team's last player alive with
// an enemy left, by kill order (a revive blurs this).
func (b *builder) clutched(r *henrik.Round, kills []henrik.Kill) bool {
	alive := map[string]bool{}
	for puuid := range b.v.players {
		alive[puuid] = true
	}
	for _, k := range kills {
		delete(alive, k.Victim.PUUID)
		if !alive[b.p.PUUID] {
			return false
		}
		teammates, enemies := 0, 0
		for puuid := range alive {
			if b.v.teamOf[puuid] == b.p.TeamID {
				teammates++
			} else {
				enemies++
			}
		}
		if teammates == 1 && enemies > 0 {
			return true
		}
	}
	return false
}

// attackerAlive: an attacker was still alive when the spike was defused.
func (b *builder) attackerAlive(r *henrik.Round, kills []henrik.Kill, at int64) bool {
	for puuid, team := range b.v.teamOf {
		if b.v.side(team, r) != "attack" {
			continue
		}
		dead := false
		for _, k := range kills {
			if k.Victim.PUUID == puuid && k.TimeInRoundInMS <= at {
				dead = true
				break
			}
		}
		if !dead {
			return true
		}
	}
	return false
}

// isolated: no living teammate within 20 m of where the player died; a death with no
// teammate alive doesn't count.
func (b *builder) isolated(d henrik.Kill) bool {
	if d.Location == nil {
		return false
	}
	nearest, ok := b.nearestTeammate(d.PlayerLocations, *d.Location)
	return ok && nearest > isolatedMetres
}

// nearestTeammate is the distance from a point to the closest living teammate in an event's
// player positions.
func (b *builder) nearestTeammate(locations []henrik.PlayerLocation, from henrik.Location) (float64, bool) {
	best, found := 0.0, false
	for _, l := range locations {
		if l.Player.PUUID == b.p.PUUID || b.v.teamOf[l.Player.PUUID] != b.p.TeamID {
			continue
		}
		d := metres(from, l.Location)
		if !found || d < best {
			best, found = d, true
		}
	}
	return best, found
}

// opponents computes the stats against individual opponents and their ranks.
func (b *builder) opponents() {
	v, n, me := b.v, b.n, b.p.PUUID
	killsOn, deathsTo := map[string]int{}, map[string]int{}
	var opponents []*henrik.Player
	for i := range v.m.Players {
		if o := &v.m.Players[i]; o.TeamID != b.p.TeamID {
			opponents = append(opponents, o)
			killsOn[o.PUUID], deathsTo[o.PUUID] = 0, 0
		}
	}
	var enemyMVP string
	for id, t := range v.teams {
		if id != b.p.TeamID && t.MVP != nil {
			enemyMVP = t.MVP.PUUID
		}
	}
	n.set("revenge_kills", 0)
	n.set("kills_on_enemy_mvp", 0)
	owed := map[string]bool{}
	for _, r := range v.rounds {
		for _, k := range v.kills[r.ID] {
			if !v.enemyKill(k) {
				continue
			}
			switch {
			case k.Killer.PUUID == me:
				killsOn[k.Victim.PUUID]++
				if owed[k.Victim.PUUID] {
					n.add("revenge_kills", 1)
				}
				owed[k.Victim.PUUID] = false
				if k.Victim.PUUID == enemyMVP {
					n.add("kills_on_enemy_mvp", 1)
				}
				if victim := v.players[k.Victim.PUUID]; victim != nil && victim.Agent.ID != "" {
					n.add("kills_vs_agent:"+lower(victim.Agent.ID), 1)
				}
			case k.Victim.PUUID == me:
				deathsTo[k.Killer.PUUID]++
				owed[k.Killer.PUUID] = true
			}
		}
	}
	dominations, dominatedBy := 0, 0
	maxMargin, maxKills, maxDeaths := math.MinInt, 0, 0
	for _, o := range opponents {
		margin := killsOn[o.PUUID] - deathsTo[o.PUUID]
		if margin > 5 {
			dominations++
		}
		if margin < -5 {
			dominatedBy++
		}
		maxMargin = max(maxMargin, margin)
		maxKills = max(maxKills, killsOn[o.PUUID])
		maxDeaths = max(maxDeaths, deathsTo[o.PUUID])
	}
	if len(opponents) > 0 {
		n.set("dominations", float64(dominations))
		n.set("dominated_by", float64(dominatedBy))
		n.set("max_duel_margin", float64(maxMargin))
		n.set("max_kills_one_opponent", float64(maxKills))
		n.set("max_deaths_to_one_opponent", float64(maxDeaths))
	}

	// Rank-based stats: Competitive only, and only ranked players (tier above 0).
	if !b.competitive() {
		return
	}
	var ranked []*henrik.Player
	for _, o := range opponents {
		if o.Tier.ID > 0 {
			ranked = append(ranked, o)
		}
	}
	if len(ranked) == 0 {
		return
	}
	top, bottom := ranked[0].Tier.ID, ranked[0].Tier.ID
	for _, o := range ranked {
		top, bottom = max(top, o.Tier.ID), min(bottom, o.Tier.ID)
	}
	onTop, onBottom := 0, 0
	highest, lowest := 0, 0
	for _, o := range ranked {
		if o.Tier.ID == top {
			onTop += killsOn[o.PUUID]
		}
		if o.Tier.ID == bottom {
			onBottom += killsOn[o.PUUID]
		}
		if killsOn[o.PUUID] > 0 {
			if highest == 0 || o.Tier.ID > highest {
				highest = o.Tier.ID
			}
			if lowest == 0 || o.Tier.ID < lowest {
				lowest = o.Tier.ID
			}
		}
	}
	n.set("kills_on_top_ranked_opponent", float64(onTop))
	n.set("kills_on_bottom_ranked_opponent", float64(onBottom))
	if highest > 0 {
		n.set("highest_tier_killed", float64(highest))
		n.set("lowest_tier_killed", float64(lowest))
	}
	mine := b.p.Tier.ID
	if mine <= 0 {
		return
	}
	higher, lowerKills, deathsLower := 0, 0, 0
	for _, o := range ranked {
		if o.Tier.ID > mine {
			higher += killsOn[o.PUUID]
		}
		if o.Tier.ID < mine {
			lowerKills += killsOn[o.PUUID]
			deathsLower += deathsTo[o.PUUID]
		}
	}
	n.set("kills_on_higher_ranked", float64(higher))
	n.set("kills_on_lower_ranked", float64(lowerKills))
	n.set("deaths_to_lower_ranked", float64(deathsLower))
}
