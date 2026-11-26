package stats

import (
	"compress/gzip"
	"encoding/json"
	"os"
	"strings"
	"testing"

	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/henrik"
	"github.com/SaswatM-62/FriendlyFire/services/match-data-service/internal/lookup"
	"github.com/stretchr/testify/assert"
	"github.com/stretchr/testify/require"
)

func tables(t *testing.T) lookup.Tables {
	t.Helper()
	tbl, err := lookup.Load()
	require.NoError(t, err)
	return tbl
}

// fixtureMatches are the anonymized real matches the offline mode serves.
func fixtureMatches(t *testing.T) []henrik.Match {
	t.Helper()
	file, err := os.Open("../source/fixtures/henrikdev/matches.json.gz")
	require.NoError(t, err)
	defer func() { _ = file.Close() }()
	gz, err := gzip.NewReader(file)
	require.NoError(t, err)
	var response henrik.Response
	require.NoError(t, json.NewDecoder(gz).Decode(&response))
	require.NotEmpty(t, response.Data)
	return response.Data
}

// everyone links every player in the match, so every scoreboard row gets checked.
func everyone(m henrik.Match) map[string]string {
	linked := map[string]string{}
	for _, p := range m.Players {
		linked[p.PUUID] = "user-" + p.PUUID
	}
	return linked
}

// The catalogue's consistency rules hold on real data: splits by side and by half add up
// to the scoreboard, and the event-based counts agree with it.
func TestRecordsAgreeWithTheScoreboardOnRealMatches(t *testing.T) {
	tbl := tables(t)
	rows := 0
	for _, m := range fixtureMatches(t) {
		records, err := Records(m, everyone(m), tbl)
		require.NoError(t, err)
		require.Len(t, records, len(m.Players), m.Metadata.MatchID)
		for _, r := range records {
			rows++
			n := r.Numbers
			id := m.Metadata.MatchID + "/" + r.UserID
			played := n["rounds_played"]
			assert.Equal(t, float64(r.Kills), n["attack_kills"]+n["defense_kills"], "kills by side "+id)
			assert.Equal(t, float64(r.Deaths), n["attack_deaths"]+n["defense_deaths"], "deaths by side "+id)
			assert.Equal(t, float64(r.Assists), n["attack_assists"]+n["defense_assists"], "assists by side "+id)
			assert.Equal(t, played, n["rounds_played_attack"]+n["rounds_played_defense"], "rounds by side "+id)
			assert.LessOrEqual(t, n["first_half_kills"]+n["second_half_kills"], float64(r.Kills), id)
			assert.LessOrEqual(t, n["kast_rounds"], played, id)
			assert.LessOrEqual(t, n["deathless_rounds"], played, id)
			assert.LessOrEqual(t, n["weapon_kills"]+n["ability_kills"], float64(r.Kills), id)
			assert.LessOrEqual(t, n["clutch_wins"], n["clutch_attempts"], id)

			byKills := 0.0
			for key, value := range n {
				if strings.HasPrefix(key, "rounds_with_kills:") {
					byKills += value
				}
				assert.NotEqual(t, "", strings.TrimSuffix(key, ":"), key)
				assert.False(t, strings.HasSuffix(key, ":"), "a key built from an empty parameter: "+key)
			}
			assert.Equal(t, played, byKills, "rounds_with_kills adds up to the rounds "+id)
			if position, ok := n["scoreboard_position"]; ok {
				assert.GreaterOrEqual(t, position, 1.0)
				assert.LessOrEqual(t, position, 10.0)
			}
			assert.Contains(t, []string{"win", "loss", "draw"}, r.Labels["result"])
			assert.Contains(t, []string{"attack", "defense"}, r.Labels["starting_side"])
			assert.NotEmpty(t, r.Labels["role"], "every agent is in the agent table")
		}
	}
	assert.Equal(t, 200, rows)
}

func TestOnlyLinkedPlayersGetRecords(t *testing.T) {
	m := fixtureMatches(t)[0]
	p := m.Players[3]

	records, err := Records(m, map[string]string{p.PUUID: "user-1"}, tables(t))

	require.NoError(t, err)
	require.Len(t, records, 1)
	r := records[0]
	assert.Equal(t, "user-1", r.UserID)
	assert.Equal(t, m.Metadata.MatchID, r.MatchID)
	assert.Equal(t, p.Stats.Kills, r.Kills)
	// Nothing in the record names another player.
	for key, value := range r.Labels {
		for _, other := range m.Players {
			assert.NotEqual(t, other.PUUID, value, key)
			assert.NotEqual(t, other.Name, value, key)
		}
	}
}

func TestMatchesOutsideCompetitiveAndUnratedOrUnfinishedAreSkipped(t *testing.T) {
	m := fixtureMatches(t)[0]
	deathmatch := m
	deathmatch.Metadata.Queue.ID = "deathmatch"
	unfinished := m
	unfinished.Metadata.IsCompleted = false

	for _, skipped := range []henrik.Match{deathmatch, unfinished} {
		records, err := Records(skipped, everyone(skipped), tables(t))
		require.NoError(t, err)
		assert.Empty(t, records)
	}
}

func TestUnratedHasNoRankStats(t *testing.T) {
	m := fixtureMatches(t)[0]
	m.Metadata.Queue.ID = "unrated"

	records, err := Records(m, everyone(m), tables(t))

	require.NoError(t, err)
	for _, r := range records {
		for _, key := range []string{"rank_tier", "kills_on_higher_ranked", "lobby_avg_tier", "highest_opponent_tier"} {
			assert.NotContains(t, r.Numbers, key)
		}
		assert.NotContains(t, r.Labels, "rank")
	}
}

func TestEveryWeaponAndAgentKeyIsNamedOnTheRecord(t *testing.T) {
	tbl := tables(t)
	named := 0
	for _, m := range fixtureMatches(t) {
		records, err := Records(m, everyone(m), tbl)
		require.NoError(t, err)
		for _, r := range records {
			for key := range r.Numbers {
				for _, prefix := range []string{"kills_with_weapon:", "rounds_with_loadout:", "kills_vs_agent:"} {
					if id, ok := strings.CutPrefix(key, prefix); ok {
						assert.NotEmpty(t, r.Labels["name:"+id], key)
						named++
					}
				}
			}
		}
	}
	assert.Positive(t, named)
}

func TestNamesComeFromTheLookupTables(t *testing.T) {
	m := fixtureMatches(t)[0]
	records, err := Records(m, everyone(m), tables(t))
	require.NoError(t, err)

	names := map[string]bool{}
	for _, r := range records {
		for key, value := range r.Labels {
			if strings.HasPrefix(key, "name:") {
				names[value] = true
			}
		}
	}
	// Every match has someone holding a Vandal or Phantom, and a Classic in the pistol round.
	assert.True(t, names["Vandal"] || names["Phantom"], names)
	assert.True(t, names["Classic"], names)
}

// ---------- Hand-built matches for the rules real data rarely shows ----------

// game builds a 2-against-2 match: a1, a2 (team Red) against b1, b2 (Blue), with Red
// attacking in round 0. Rounds are added as a test needs them.
type game struct{ m henrik.Match }

func newGame() *game {
	m := henrik.Match{
		Metadata: henrik.Metadata{
			MatchID: "m1", StartedAt: "2026-11-10T20:00:00Z", IsCompleted: true,
			Queue: henrik.Queue{ID: "competitive", Name: "Competitive", ModeType: "Standard"},
			Map:   henrik.IDName{ID: "map-1", Name: "Ascent"},
		},
	}
	for _, p := range []struct{ puuid, team string }{{"a1", "Red"}, {"a2", "Red"}, {"b1", "Blue"}, {"b2", "Blue"}} {
		score := 200.0
		m.Players = append(m.Players, henrik.Player{
			PUUID: p.puuid, TeamID: p.team, PartyID: "party-" + p.team,
			Agent:       henrik.IDName{ID: "add6443a-41bd-e414-f6ad-e58d267f4e95", Name: "Jett"},
			Tier:        henrik.Tier{ID: 15, Name: "Gold 1"},
			Performance: &henrik.Performance{Score: &score},
		})
	}
	won, lost := true, false
	m.Teams = []henrik.Team{
		{TeamID: "Red", Won: &won, Rounds: henrik.TeamRounds{Won: 1, Lost: 0}},
		{TeamID: "Blue", Won: &lost, Rounds: henrik.TeamRounds{Won: 0, Lost: 1}},
	}
	return &game{m: m}
}

func (g *game) round(id int, winner, result string, kills ...henrik.Kill) *game {
	role := "Attacker"
	if (winner == "Red") != (id < 12) {
		role = "Defender"
	}
	r := henrik.Round{ID: id, Result: result, WinningTeam: winner, WinningTeamRole: role}
	for _, p := range g.m.Players {
		r.Stats = append(r.Stats, henrik.RoundStats{Player: henrik.Person{PUUID: p.PUUID, Team: p.TeamID}})
	}
	for i := range kills {
		kills[i].Round = id
	}
	g.m.Rounds = append(g.m.Rounds, r)
	g.m.Kills = append(g.m.Kills, kills...)
	return g
}

func kill(at int64, killer, victim string, weapon ...string) henrik.Kill {
	k := henrik.Kill{
		TimeInRoundInMS: at,
		Killer:          henrik.Person{PUUID: killer},
		Victim:          henrik.Person{PUUID: victim},
	}
	vandal := "9c82e19d-4575-0200-1a81-3eacf00cf872"
	name, kind := "Vandal", "Weapon"
	k.Weapon = henrik.Weapon{ID: &vandal, Name: &name, Type: &kind}
	if len(weapon) == 3 {
		id, name, kind := weapon[0], weapon[1], weapon[2]
		k.Weapon = henrik.Weapon{Type: &kind}
		if id != "" {
			k.Weapon.ID = &id
		}
		if name != "" {
			k.Weapon.Name = &name
		}
	}
	return k
}

func (g *game) records(t *testing.T, linked map[string]string) map[string]Record {
	t.Helper()
	records, err := Records(g.m, linked, tables(t))
	require.NoError(t, err)
	byUser := map[string]Record{}
	for _, r := range records {
		byUser[r.UserID] = r
	}
	return byUser
}

func all(g *game) map[string]string {
	return map[string]string{"a1": "u-a1", "a2": "u-a2", "b1": "u-b1", "b2": "u-b2"}
}

func TestATradeIsAKillWithinThreeSecondsOfTheTeammatesDeath(t *testing.T) {
	g := newGame().round(0, "Red", "Elimination",
		kill(10_000, "b1", "a2"), // b1 kills a2...
		kill(12_500, "a1", "b1"), // ...a1 trades 2.5 s later
		kill(20_000, "b2", "a1"),
		kill(23_500, "a2", "b2"), // 3.5 s: too late to count as a trade (and a2 was dead: revives happen)
	)

	r := g.records(t, all(g))

	assert.Equal(t, 1.0, r["u-a1"].Numbers["trade_kills"])
	assert.Equal(t, 1.0, r["u-a2"].Numbers["traded_deaths"])
	assert.Equal(t, 0.0, r["u-a1"].Numbers["traded_deaths"])
	assert.Equal(t, 0.0, r["u-a2"].Numbers["trade_kills"])
	// KAST: a2 died but was traded; a1 killed.
	assert.Equal(t, 1.0, r["u-a2"].Numbers["kast_rounds"])
}

func TestFirstBloodsFirstDeathsAndATieAtTheStart(t *testing.T) {
	g := newGame()
	g.round(0, "Red", "Elimination", kill(5_000, "a1", "b1"))
	g.m.Rounds[0].FirstBlood = &henrik.Person{PUUID: "a1"}
	g.round(1, "Red", "Elimination", kill(7_000, "a1", "b2"), kill(7_000, "b1", "a2"))
	g.m.Rounds[1].FirstBlood = &henrik.Person{PUUID: "a1"}

	r := g.records(t, all(g))

	assert.Equal(t, 2.0, r["u-a1"].Numbers["first_bloods"])
	assert.Equal(t, 5000.0, r["u-a1"].Numbers["fastest_first_blood_ms"])
	assert.Equal(t, 1.0, r["u-b1"].Numbers["first_deaths"])
	// Round 1's two earliest kills share a time, so nobody's first death counts there.
	assert.Equal(t, 0.0, r["u-a2"].Numbers["first_deaths"])
	assert.Equal(t, 0.0, r["u-b2"].Numbers["first_deaths"])
}

func TestSurrenderedRoundsAreSkipped(t *testing.T) {
	g := newGame().round(0, "Red", "Elimination", kill(5_000, "a1", "b1"))
	g.round(1, "Red", "Surrendered", kill(1_000, "a1", "b2"))

	r := g.records(t, all(g))

	assert.Equal(t, 1.0, r["u-a1"].Numbers["attack_kills"])
	assert.Equal(t, 1.0, r["u-a1"].Numbers["rounds_with_kills:1"])
	assert.NotContains(t, r["u-a1"].Numbers, "rounds_with_kills:0")
}

func TestAbilityGunsAndAbilitiesAreAbilityKillsByslot(t *testing.T) {
	tourDeForce := "39099fb5-4293-def4-1e09-2e9080ce7456"
	g := newGame().round(0, "Red", "Elimination",
		kill(1_000, "a1", "b1", tourDeForce, "", "Weapon"),
		kill(2_000, "a1", "b2", "GrenadeAbility", "", "Ability"),
	)
	g.round(1, "Red", "Elimination",
		kill(1_000, "a1", "b1", "2f59173c-4bed-b6c3-2191-dea9b58be9c7", "", "Melee"),
		kill(2_000, "a1", "b2", "", "", ""), // no weapon data at all
	)

	n := g.records(t, all(g))["u-a1"].Numbers

	assert.Equal(t, 2.0, n["ability_kills"])
	assert.Equal(t, 1.0, n["ability_kills_ultimate"])
	assert.Equal(t, 1.0, n["ability_kills_grenade"])
	assert.Equal(t, 1.0, n["knife_kills"])
	assert.Equal(t, 1.0, n["weapon_kills"], "the knife; the kill with no weapon data is neither")
	assert.NotContains(t, n, "kills_with_weapon:"+tourDeForce)
}

func TestFriendPairsUseTheFriendsUserID(t *testing.T) {
	g := newGame().round(0, "Red", "Elimination",
		kill(1_000, "a2", "b1"),
		kill(4_000, "b2", "a2"),
		kill(5_000, "a1", "b2"), // avenges a2 within 3 s
	)
	g.m.Kills[0].Assistants = []henrik.Person{{PUUID: "a1"}} // a1 assists a2's kill
	g.m.Rounds[0].Stats[1].DamageEvents = []henrik.DamageEvent{{Player: henrik.Person{PUUID: "b2"}, Damage: 40}}

	// Only a1 and a2 are linked: b1 and b2 are strangers.
	r := g.records(t, map[string]string{"a1": "u-a1", "a2": "u-a2"})

	n := r["u-a1"].Numbers
	assert.Equal(t, 1.0, n["assists_for:u-a2"])
	assert.Equal(t, 1.0, n["trades_for:u-a2"])
	assert.Equal(t, 1.0, n["finishes_for:u-a2"], "a2 damaged b2, a1 finished them")
	assert.NotContains(t, n, "assists_for:u-b1")
	assert.Equal(t, 0.0, r["u-a2"].Numbers["assists_for:u-a1"])
}

func TestRevengeAndDominations(t *testing.T) {
	var kills []henrik.Kill
	for i := 0; i < 6; i++ {
		kills = append(kills, kill(int64(1_000*(i+1)), "a1", "b1"))
	}
	g := newGame().round(0, "Red", "Elimination", kills...)
	g.round(1, "Red", "Elimination", kill(1_000, "b1", "a1"), kill(2_000, "a1", "b1"))

	n := g.records(t, all(g))["u-a1"].Numbers

	assert.Equal(t, 1.0, n["revenge_kills"])
	assert.Equal(t, 1.0, n["dominations"], "7 kills on b1 against 1 death: a margin of 6")
	assert.Equal(t, 7.0, n["max_kills_one_opponent"])
	assert.Equal(t, 6.0, n["max_duel_margin"])
}

func TestUnknownValuesAreLeftOut(t *testing.T) {
	g := newGame().round(0, "Red", "Elimination", kill(1_000, "a1", "b1"))
	g.m.Players[3].Performance.Score = nil // b2's Performance Score is null

	n := g.records(t, all(g))["u-a1"].Numbers

	assert.NotContains(t, n, "kd_ratio", "no deaths: K/D is unknown, never infinity")
	assert.NotContains(t, n, "scoreboard_position", "one unknown score leaves the comparison out")
	assert.NotContains(t, n, "topped_match_performance_score")
	assert.Contains(t, n, "topped_match_kills")
}

func TestADrawIsADraw(t *testing.T) {
	g := newGame().round(0, "Red", "Elimination")
	lost := false
	g.m.Teams[0].Won, g.m.Teams[1].Won = &lost, &lost
	g.m.Teams[0].Rounds = henrik.TeamRounds{Won: 12, Lost: 12}
	g.m.Teams[1].Rounds = henrik.TeamRounds{Won: 12, Lost: 12}

	r := g.records(t, all(g))["u-a1"]

	assert.False(t, r.Won)
	assert.Equal(t, "draw", r.Labels["result"])
}

func TestAClutchIsBeingYourTeamsLastPlayerAlive(t *testing.T) {
	g := newGame().round(0, "Red", "Elimination",
		kill(1_000, "b1", "a2"), // a1 is alone against two
		kill(2_000, "a1", "b1"),
		kill(3_000, "a1", "b2"),
	)

	n := g.records(t, all(g))["u-a1"].Numbers

	assert.Equal(t, 1.0, n["clutch_attempts"])
	assert.Equal(t, 1.0, n["clutch_wins"])
	assert.Equal(t, 1.0, n["round_winning_kills"])
}
