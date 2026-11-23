# Player match stats catalogue

The stat vocabulary for VALORANT matches. match-data-service turns each HenrikDev v4 match into one `PlayerMatchStats` record per linked player (top-level fields plus the `labels` and `numbers` maps), and quiz templates read those keys. Every key is case-sensitive, and existing keys keep their names.

Every rule here was checked against real HenrikDev v4 data: 84 Competitive matches (840 player rows, 1,800 played rounds, 13,261 kill events) from two players, one of them always in a 5-stack, plus an earlier fetch with 2 Unrated matches. The sample files are not kept in the repository. Measured percentages come from one friend group, so treat them as rough.

## Scope

- **Queues:** Competitive and Unrated only. Accept `M.metadata.queue.id` equal to `competitive` or `unrated`; don't filter on `mode_type` (`Standard` isn't a queue). The sync drops Premier, Swiftplay, Deathmatch and custom games. Unrated rests on 2 matches, so its fields are lightly tested.
- **Who is stored:** linked players only. Match and team comparisons are computed at sync time from the full scoreboard, but unlinked players and opponents are never stored.
- **History:** only the last 30 days are stored. Every sync re-fetches those 30 days and, once the fetch succeeds, replaces the player's older matches, so the 30 days count back from the player's last sync. The quiz engine reads every stored match, the same ones the lobby counts. "Whole history" in a question means those 30 days.
- **Deletion:** deleting a user deletes their records and every friend-pair key that names them on other players' records.
- **Riot's policy:** data comes only from players who opted in by linking, and players the game deliberately hides can't be identified or analysed: leave a hidden opponent out of opponent stats ([Riot's VALORANT policies](https://developer.riotgames.com/docs/valorant)).
- **Lookup tables:** match-data-service owns and refreshes each patch a weapon table (weapon ID → name and class, including ability guns) and an agent table (agent ID → role and the ability in each slot, from [valorant-api.com](https://valorant-api.com/v1/agents?isPlayableCharacter=true)).

## Notation

| Alias | Meaning |
| --- | --- |
| `M` | `$.data[i]`, one match |
| `P` | `M.players[j]` for the linked player; join by `puuid`, never display name |
| `T` | `M.teams[k]` where `team_id == P.team_id` |
| `R` | each played round: `M.rounds[r]` where `result != "Surrendered"` |
| `S` | `R.stats[s]` where `player.puuid == P.puuid` |
| `K` | each `M.kills[k]` |
| enemy kill | a `K` whose killer is a match player on the other team from `K.victim`; self-kills and teammate kills are not kills |
| your death | a `K` with `K.victim.puuid == P.puuid` |
| opponent | a match player on the other team |

## Storage rules

- One record per `(user_id, game, match_id)`. PUUIDs are internal join keys and never leave the service.
- Unknown is omitted, never stored as `0` or `"null"`. A count built from events is `0` only when the event data is complete.
- Only complete matches (`M.metadata.is_completed == true`). A missing team or round is unknown, not a loss.
- Played rounds only. Surrender placeholder rounds (`result == "Surrendered"`) are skipped everywhere.
- A division by zero is unknown and omitted (a deathless K/D included), never infinity.
- Keep full precision; round only for display. Numeric flags are `1` or `0`, omitted when unknown.
- Keys ending in `:<parameter>` (for example `kills_with_weapon:<weapon_id>`) are prefixes with one key per value; never build a key from an empty parameter.

## How the scoreboard relates to the event data

These rules make the scoreboard and the events agree in all 840 sample rows. Derived stats follow them.

| Field | Rule |
| --- | --- |
| Kills (`P.stats.kills`) | Enemy kills only. Self-kills (falls, the spike, Clove's ult running out, your own molly) and teammate kills don't count. |
| Deaths (`P.stats.deaths`) | Every death except deaths to the spike (`K.weapon.type == "Bomb"`). Falls and Clove's ult running out count, and so does a second death after a revive. |
| Assists (`P.stats.assists`) | Assists where the victim is on the other team from you, including an enemy who fell or died to the spike after you damaged them. |
| Combat score (`P.stats.score`) | The sum of per-round `S.stats.score`. |
| Headshots (`P.stats.headshots`) | The sum of per-round `S.stats.headshots`. Hits, not headshot kills. |
| Damage dealt and received | Every `S.damage_events` entry except the spike's 999 against yourself (an entry aimed at you, from you, of 999 or more). Self-damage and teammate damage stay in. |
| First blood (`R.first_blood`) | The killer of the round's earliest kill; null only in surrender placeholders. |
| Attack or defense | From `R.winning_team` and `R.winning_team_role`: the winner has that role and the other team the other one. Each team keeps one side for rounds 1–12 and the other for rounds 13–24; in overtime the sides swap every round (true in all 84 sample matches). |
| Halves | The first half is rounds 1–12 (`R.id` 0–11) and the second half rounds 13–24 (`R.id` 12–23). Overtime rounds (`R.id` 24 on) are in neither half. Kills, deaths and assists split by side, or by first half, second half and overtime, add up to the scoreboard. |
| Timing | Plant, defuse and kill times share one round clock; the spike explodes 45 s after the plant. |

## Top-level fields

| Key | Meaning | Source |
| --- | --- | --- |
| `match_id` | Match identifier | `M.metadata.match_id` |
| `user_id` | FriendlyFire linked user | Account linkage, not HenrikDev JSON |
| `game` | Game discriminator | Constant `valorant` |
| `played_at` | Match start time | `M.metadata.started_at` |
| `won` | The player's team won | `T.won`; false also covers a draw, so use `labels.result` |
| `kills` | Match kills | `P.stats.kills` |
| `deaths` | Match deaths | `P.stats.deaths` |
| `assists` | Match assists | `P.stats.assists` |

## Labels (`labels` map)

| Key | Meaning | Source / derivation |
| --- | --- | --- |
| `agent` | Agent name | `P.agent.name` |
| `agent_id` | Agent identifier | `P.agent.id` |
| `role` | Agent role (Duelist, Initiator, Controller, Sentinel) | Agent table, by `P.agent.id` |
| `map` | Map name | `M.metadata.map.name` |
| `map_id` | Map identifier | `M.metadata.map.id` |
| `mode` | Queue display name | `M.metadata.queue.name` |
| `queue_id` | Queue filter identifier | `M.metadata.queue.id` |
| `mode_type` | Ruleset name | `M.metadata.queue.mode_type` |
| `team_id` | Player's team identifier | `P.team_id` |
| `rank` | Competitive tier at this match, not current or peak rank; for opponent stats, not own-rank questions | `P.tier.name`; Competitive only, omitted for tier 0 (unranked, placements, Unrated) |
| `season` | Act, for grouping | `M.metadata.season.short` |
| `season_id` | Season UUID | `M.metadata.season.id` |
| `region` | Match region | `M.metadata.region` |
| `cluster` | Server cluster | `M.metadata.cluster` |
| `platform` | Player platform | `P.platform` |
| `match_platform` | Match platform | `M.metadata.platform` |
| `game_version` | Game build | `M.metadata.game_version` |
| `result` | `win`, `loss` or `draw` | `T.won` true → win; false with equal `T.rounds.won` and `lost` → draw; false with fewer won → loss; otherwise unknown |
| `starting_side` | Your team's side in round 1, `attack` or `defense`, and so for the whole first half | The attack-or-defense rule at `R.id` 0 |

## Direct numbers (`numbers` map)

| Key | Meaning | Source |
| --- | --- | --- |
| `performance_score` | Riot's Performance Score (0–500), the headline stat; it replaced ACS in patch 13.06 and decides the MVP | `P.performance.score` |
| `headshots` | Head hits, not headshot kills | `P.stats.headshots` |
| `bodyshots` | Body hits | `P.stats.bodyshots` |
| `legshots` | Leg hits | `P.stats.legshots` |
| `damage_dealt` | Damage dealt (see the damage rule) | `P.stats.damage.dealt` |
| `damage_received` | Damage received | `P.stats.damage.received` |
| `combat_score` | Total combat score; not ACS or Performance Score | `P.stats.score` |
| `rounds_won` | Rounds won by the player's team | `T.rounds.won` |
| `rounds_lost` | Rounds lost by the player's team | `T.rounds.lost` |
| `rounds_played` | Played rounds | `T.rounds.won + T.rounds.lost`; equals the count of played rounds `R` |
| `match_length_ms` | Match duration | `M.metadata.game_length_in_ms` |
| `rank_tier` | Numeric Competitive tier at match time; for opponent stats, not own-rank questions | `P.tier.id`; Competitive only, omitted for tier 0 |
| `account_level` | Account level at match time | `P.account_level` |
| `team_number` | Numeric team identifier, not a position | `P.team_number` |
| `session_playtime_ms` | Reported session time; not lifetime or match time | `P.session_playtime_in_ms` |
| `ability_casts_grenade`, `ability_casts_ability1`, `ability_casts_ability2`, `ability_casts_ultimate` | Casts per ability slot; the agent table names the ability | `P.ability_casts.grenade`, `.ability1`, `.ability2`, `.ultimate` |
| `credits_spent` | Credits spent | `P.economy.spent.overall` |
| `credits_spent_average` | Average credits spent per round | `P.economy.spent.average` |
| `loadout_value` | Sum of round loadout values | `P.economy.loadout_value.overall` |
| `loadout_value_average` | Average loadout value per round | `P.economy.loadout_value.average` |

## Derived numbers (`numbers` map)

Counts below use played rounds and enemy kills only. The trade window is 3 s everywhere.

### Combat

| Key | Meaning and derivation |
| --- | --- |
| `acs` | `P.stats.score / rounds_played` |
| `adr` | `P.stats.damage.dealt / rounds_played` |
| `headshot_percentage` | `100 * headshots / (headshots + bodyshots + legshots)` |
| `kd_ratio` | `kills / deaths` |
| `kda_ratio` | `(kills + assists) / deaths` |
| `damage_delta` | `damage_dealt - damage_received` |
| `first_bloods` | Rounds where `R.first_blood.puuid == P.puuid` |
| `first_deaths` | Rounds where the earliest enemy kill's victim is you; omit the round when two earliest kills share a time |
| `fastest_first_blood_ms` | Lowest earliest-kill time among your first-blood rounds |
| `max_round_damage` | Highest damage to the other team in one round: sum of your `S.damage_events` aimed at opponents. Self-damage and the spike's 999 never count. |
| `max_round_kills` | Most enemy kills in one round, from `K` (per-round `S.stats.kills` includes self-kills and teammate kills) |
| `rounds_with_kills:<n>` | Rounds with exactly n enemy kills, from `K`; zero-kill rounds included |
| `max_round_combat_score` | Highest `S.stats.score` in one round |
| `kast_pct` | % of rounds with an enemy kill, an assist, no death (spike deaths ignored, falls count) or a traded death |
| `kast_rounds` | Rounds that count towards `kast_pct`. Pooled KAST is total `kast_rounds` ÷ total `rounds_played`; `kast_pct` can only be averaged per match. |
| `trade_kills` | You killed the enemy who killed a teammate in the previous 3 s |
| `traded_deaths` | A teammate killed your killer within 3 s of your death |
| `deathless_rounds` | Rounds with no death event other than the spike. Revives mean this isn't "alive at round end". |
| `longest_deathless_streak` | Most rounds in a row without a death (spike ignored) |
| `longest_kill_streak_rounds` | Most rounds in a row with at least one enemy kill |
| `killed_then_died` | Your enemy kills followed by your death within 3 s |
| `round_winning_kills` | The last enemy kill of a round your team won with `R.result == "Elimination"` |
| `untouched_multikill_rounds` | Rounds with 2+ enemy kills where no other player's damage event hit you |
| `double_kills_under_1s` | Pairs of your consecutive enemy kills in a round under 1 s apart |
| `clutch_attempts` | Rounds where you were your team's last player alive with an enemy left, by kill order (revives blur this) |
| `clutch_wins` | Those rounds that your team won |
| `aces`, `clutches`, `closers` | Rounds where `R.ceremony_player.puuid == P.puuid` with `CeremonyAce`, `CeremonyClutch` or `CeremonyCloser` (the game's own ceremonies) |
| `longest_kill_m` | Longest distance from your position in `K.player_locations` to `K.location`, game units ÷ 100 |
| `avg_kill_time_s` | Average `K.time_in_round_in_ms` of your enemy kills, in seconds |
| `avg_distance_to_team_m` | Average distance, at each of your enemy kills, from you to the nearest living teammate in `K.player_locations` |
| `isolated_deaths` | Your deaths (spike ignored) with no living teammate within 20 m of `K.location`; deaths with no teammate alive don't count |

### Kills and assists by type

| Key | Meaning and derivation |
| --- | --- |
| `weapon_kills` | Enemy kills with a bought gun (`K.weapon.type == "Weapon"` with a name) or the knife (`"Melee"`) |
| `ability_kills` | Enemy kills with `K.weapon.type == "Ability"`, plus ability guns: `"Weapon"` with a null name (Chamber's Headhunter and Tour De Force, Neon's Overdrive) |
| `ability_kills_grenade`, `ability_kills_ability1`, `ability_kills_ability2`, `ability_kills_ultimate` | Ability kills by slot. For `"Ability"` kills `K.weapon.id` is the slot (`GrenadeAbility`, `Ability1`, `Ability2`, `Ultimate`); ability guns take their slot from the weapon table (Headhunter → ability1; Tour De Force, Overdrive → ultimate) |
| `kills_with_weapon:<weapon_id>` | Enemy kills by each bought gun or the knife, by `K.weapon.id`; the name comes from `K.weapon.name` or the weapon table |
| `rifle_kills`, `smg_kills`, `sniper_kills`, `shotgun_kills`, `machine_gun_kills` | Bought-gun kills by class, from the weapon table (the Warden is a rifle) |
| `eco_pistol_kills` | Sidearm kills outside rounds 1 and 13 |
| `picked_up_gun_kills` | Kills with a non-sidearm gun other than your round's `S.economy.weapon` |
| `knife_kills` | Enemy kills with `K.weapon.type == "Melee"` |
| `alt_fire_kills` | Bought-gun kills with `K.secondary_fire_mode` |
| `damage_assists` | Assists where you damaged the victim that round |
| `utility_assists` | Assists with no damage on the victim that round: flashes, reveals, trips and other debuffs. The closest the data gets to utility effects; enemies affected but not killed leave no trace. |

### Rounds, spike and economy

| Key | Meaning and derivation |
| --- | --- |
| `post_plant_kills` | Enemy kills after `R.plant.round_time_in_ms` |
| `outgunned_kills` | Enemy kills where your `S.economy.loadout_value` was below the victim's |
| `pistol_round_kills` | Enemy kills in rounds 1 and 13 (`R.id` 0 and 12) |
| `plants`, `defuses` | Rounds where `R.plant.player.puuid` or `R.defuse.player.puuid` is you |
| `plants_site_<site>` | Your plants by `R.plant.site` (`A`, `B`, `C`); only some maps have a C site |
| `fastest_plant_ms` | Lowest `R.plant.round_time_in_ms` of your plants |
| `closest_defuse_ms_left` | Lowest `45000 - (R.defuse.round_time_in_ms - R.plant.round_time_in_ms)` of your defuses |
| `ninja_defuses` | Your defuses while an attacker was still alive |
| `exit_kills` | Enemy kills after the spike exploded (Detonate rounds, kill time more than 45 s after the plant) |
| `spike_deaths` | Your deaths with `K.weapon.type == "Bomb"` |
| `fall_deaths` | Your deaths with `K.weapon.type == "Fall"`: falling off the map or fatal fall damage; some are credited to an enemy as a kill |
| `penalty_rounds` | Rounds with `S.received_penalty == true` |
| `avg_unspent_credits` | Average `S.economy.remaining` per round |
| `damage_per_1000_credits` | `damage_dealt / credits_spent * 1000` |
| `rounds_without_armor` | Rounds where `S.economy.armor` is null (no armor bought) |
| `rounds_with_loadout:<weapon_id>` | Rounds whose `S.economy.weapon.id` is that gun (for example Operator rounds) |
| `utility_per_round` | `(ability_casts_grenade + ability_casts_ability1 + ability_casts_ability2) / rounds_played`; compare only on the same agent or role |

### By side and half

A side is every round your team attacked, or every round it defended, overtime rounds included. A half is rounds 1–12 or rounds 13–24; overtime rounds are in neither. Each key splits a whole-match stat by the round it happened in, under that stat's rule, so a stat's two sides add up to the match total, and so do its two halves plus overtime. Compare them per round, against the round counts in [Team-shared stats](#team-shared-stats).

| Key | Meaning and derivation |
| --- | --- |
| `attack_kills`, `defense_kills`, `first_half_kills`, `second_half_kills` | Enemy kills, counted like `kills`, by side and by half |
| `attack_deaths`, `defense_deaths`, `first_half_deaths`, `second_half_deaths` | Your deaths, counted like `deaths` (spike deaths ignored), by side and by half |
| `attack_assists`, `defense_assists`, `first_half_assists`, `second_half_assists` | Your assists, counted like `assists` in the round of the kill, by side and by half |
| `attack_kast_rounds`, `defense_kast_rounds`, `first_half_kast_rounds`, `second_half_kast_rounds` | `kast_rounds` by side and by half |
| `attack_damage_dealt`, `defense_damage_dealt` | `damage_dealt` by side, from each round's `S.damage_events` |
| `attack_first_bloods`, `defense_first_bloods`, `attack_first_deaths`, `defense_first_deaths` | `first_bloods` and `first_deaths` by side |

### Against opponents

| Key | Meaning and derivation |
| --- | --- |
| `dominations` | Opponents you dominated: your enemy kills on them minus their kills on you is more than 5 in this match |
| `dominated_by` | Opponents who dominated you: that margin is below −5 |
| `max_duel_margin` | Your best kills-minus-deaths against any one opponent |
| `max_kills_one_opponent` | Most enemy kills on one opponent |
| `max_deaths_to_one_opponent` | Most deaths to one opponent |
| `revenge_kills` | Enemy kills on an opponent who had killed you since your last kill on them |
| `kills_on_enemy_mvp` | Enemy kills on the other team's `T.mvp` |
| `kills_on_higher_ranked` | "Giant killer": enemy kills on opponents with a higher tier than yours (Competitive, both tiers above 0); favours the lowest-ranked friend |
| `kills_on_lower_ranked` | "Playground bully": enemy kills on opponents with a lower tier than yours; favours the highest-ranked friend |
| `deaths_to_lower_ranked` | Deaths to opponents with a lower tier than yours |
| `kills_on_top_ranked_opponent` | Enemy kills on the match's highest-ranked opponent (every opponent sharing the top tier counts); Competitive only |
| `kills_on_bottom_ranked_opponent` | Enemy kills on the match's lowest-ranked opponent; Competitive only |
| `highest_tier_killed`, `lowest_tier_killed` | Highest and lowest tier among the opponents you killed; Competitive only. Friends who played together almost always share these (they tied in 99–100% of the samples' matches), so ask them in the solo scope with `contenders_only`; the two counts above work everywhere. |
| `kills_vs_agent:<agent_id>` | Enemy kills on opponents playing that agent |

### Friend pairs

Computed for every other linked player on your team in the match. `<user_id>` is the friend's FriendlyFire user ID, never a PUUID.

| Key | Meaning and derivation |
| --- | --- |
| `assists_for:<user_id>` | Your assists on that friend's enemy kills |
| `finishes_for:<user_id>` | Your enemy kills on a victim that friend damaged that round |
| `trades_for:<user_id>` | Your enemy kills on that friend's killer within 3 s of their death |

## Match and team comparisons

Computed from the full scoreboard, including unlinked players. If any compared value is unknown, the comparison is omitted.

| Key | Meaning and derivation |
| --- | --- |
| `scoreboard_position` | `1 +` the number of match players with a higher `P.performance.score`; tied players share a position (1, 1, 3). This matches the in-game scoreboard. |
| `team_position` | The same ranking within the player's team |
| `team_placement` | `T.placement`, the team's finish |
| `match_mvp` | `M.metadata.mvp.puuid == P.puuid` (always the top Performance Score) |
| `team_mvp` | `T.mvp.puuid == P.puuid` |
| `topped_match_<stat>` | The player has the highest `<stat>` among all match players; every tied leader gets 1 |
| `topped_team_<stat>` | The same among the player's team |

Supported `<stat>` suffixes: `performance_score`, `kills`, `deaths`, `assists`, `headshots`, `bodyshots`, `legshots`, `damage_dealt`, `damage_received`, `combat_score`, `acs`, `adr`, `first_bloods`, `first_deaths`, `plants`, `defuses`, `headshot_percentage`, `max_round_damage` and `max_round_kills`. "Topped" always means highest, including deaths, not best.

## Team-shared stats

Teammates share these values within a match, so they separate friends only through the matches they didn't play together. Even players who mostly squad up have the odd solo game: ask these in the solo scope with `contenders_only` (friends without solo games sit out), or in the all scope, which mixes solo games in. The samples' 5-stack never played apart, so they couldn't measure how often this works.

| Key | Meaning and derivation |
| --- | --- |
| `rounds_won_attack`, `rounds_won_defense` | Rounds your team won, by side (overtime included) |
| `rounds_played_attack`, `rounds_played_defense` | Played rounds your team attacked or defended (overtime included) |
| `rounds_won_first_half`, `rounds_won_second_half` | Rounds your team won in each half |
| `rounds_played_first_half`, `rounds_played_second_half` | Played rounds in each half. The second half is often short: all 12 rounds in 22 of 84 sample matches, 9.3 on average, and none after a surrender before round 13. |
| `pistol_rounds_won` | Your team won round 1 or 13 |
| `eco_rounds_won` | Your team won a non-pistol round with under 5,000 in total loadout against an opponent's 15,000 or more |
| `flawless_rounds`, `thrifty_rounds`, `team_ace_rounds` | Rounds where `R.ceremony_team` is your team with `CeremonyFlawless`, `CeremonyThrifty` or `CeremonyTeamAce` |
| `max_deficit` | Most rounds your team trailed by at any point, walking played rounds in order. A comeback is a win with `max_deficit` of 6 or more; "biggest comeback" is the largest `max_deficit` in a win. |
| `overtime` | `1` when more than 24 rounds were played |
| `party_size` | Players sharing your `P.party_id` |
| `lobby_avg_tier` | Average `tier.id` of every ranked player; Competitive only |
| `highest_opponent_tier`, `avg_opponent_tier` | Highest and average opponent tier (ranked only); Competitive only |
| `highest_level_opponent` | Highest opponent account level |
| `biggest_opponent_party` | Largest party on the other team |

## Provider fields

| Key | Meaning | Source |
| --- | --- | --- |
| `numbers.performance_damage`, `performance_adjusted_kills`, `performance_adjusted_deaths`, `performance_trades`, `performance_assists`, `performance_utility_usage`, `performance_plants`, `performance_defuses` | Riot's Performance Score inputs, in order of impact. `performance_utility_usage` is normalized per agent, so it's the cross-agent utility measure. Riot publishes no weights; never multiply these back into totals. | `P.performance.breakdown.*` |
| `labels.performance_grade` | Grade: `pass`, `merit` or `distinction` | `P.performance.ratings.grade` |
| `labels.performance_combat_damage`, `_death_impact`, `_kill_impact`, `_trades`; `labels.performance_utility_assists`, `_defuses`, `_plants`, `_utility_usage` | Categorical ratings (`double_down` to `double_up`) | `P.performance.ratings.combat.*`, `P.performance.ratings.utility.*` |
| `numbers.afk_rounds`, `numbers.rounds_in_spawn` | Reported AFK rounds and rounds in spawn | `P.behavior.afk_rounds`, `P.behavior.rounds_in_spawn` |
| `numbers.friendly_fire_incoming`, `numbers.friendly_fire_outgoing` | Unreliable: nonzero in 1 of 840 sample rows, and 0 even for players who killed a teammate | `P.behavior.friendly_fire.*` |
| `labels.card_id`, `labels.title_id`, `labels.preferred_level_border_id`, `labels.bloomline_clip_id`, `labels.bloomline_hype_id`, `labels.bloomline_plank_background_id`, `numbers.bloomline_level` | Cosmetics, not stats | `P.customization.*` |

Null provider values stay unknown.

## Question rules

- **Pooling across matches:** counts are summed; ratios are total ÷ total (pooled K/D = total kills ÷ total deaths; pooled ACS = total combat score ÷ total rounds; pooled headshot % = total head hits ÷ total hits). `performance_score` is averaged per match, every match counting equally. Win rate = wins ÷ completed matches (draws are non-wins).
- **Headline stat:** Performance Score. ACS questions stay available.
- **Solo-scope questions:** most squads still have a few solo games each. Give solo templates `contenders_only` and a small or no `last_n`, so friends with no (or too few) solo games sit out instead of skipping the question.
- **Sides and halves:** compare per round (a side or half stat ÷ the matching `rounds_played_*`), never as totals: sides and second halves vary in length, and overtime ran in 7 of 84 sample matches. Round wins by side or half are team-shared, so ask them in the solo scope. To ask about one starting side, use `starting_side` as the parameter label (the quiz engine's `ParameterLabel` has to list it first).
- **Same-agent and same-role questions:** ability questions use each friend's last 30 days and are asked only when at least 2 friends played the agent that has the ability; they compare only those friends. Show ability names from the agent table, never keys: slot names don't match keys (Phoenix's Hot Hands is `Ability1`, pressed with E). Kill questions for a single ability work only for damage abilities. Role questions use `labels.role`. Cross-agent utility uses `performance_utility_usage`.
- **Rank:** never ask about a player's own rank, which friends already know. Ranks feed opponent questions instead: giant killer, playground bully, kills on the highest- or lowest-ranked opponent. Competitive only; tier 0 is unknown. Riot's policy forbids alternatives to the official ranking and MMR or ELO calculators, so never score or rank players' skill ourselves.
- **Difficulty:** the question generator rates each question easy, medium or hard from the winner's lead over the runner-up (`question_generator.difficulty`), so templates carry no difficulty.

The [sample question bank](https://tamucs-my.sharepoint.com/:w:/r/personal/akhil_tamu_tamu_edu/Documents/FF%20Sample%20Trivia%20Questions.docx?d=wcf956a5805f44fe8ad8cd919daaeca92&csf=1&web=1&e=qwkXPA) can use agent, map and act filters over these records.

## Data quirks

| Quirk | Seen in the samples | Rule |
| --- | --- | --- |
| Surrender placeholder rounds: `result == "Surrendered"`, empty ceremony, null `first_blood`, no kills, but a winner is set | 9 rounds in 1 match (5 more in the earlier fetch) | Skip them; played rounds = won + lost |
| A surrender before round 13 leaves no second half | 1 match (4 played rounds) | `rounds_played_second_half` is 0; pooled rates still count that match's first half |
| Round timer ran out: `result` is an empty string | 17 rounds, all defender wins | Normal played rounds |
| Revives: the same player is killed twice in a round | 152 (62 of them Clove's ult) | `deathless_rounds`, not "survived" |
| Kills after the killer's recorded death | 163 | Allowed |
| Self-kills in `K` | 102 | Not kills; spike deaths aren't scoreboard deaths |
| Teammate kills in `K` | 5 (4 abilities, 1 with no weapon data) | Not kills; their assists don't count |
| A kill with no weapon data (type, ID and name null) | 1 | Unknown weapon |
| Ability guns are type `Weapon` with a null name | 364 kills | Ability kills |
| Tour De Force has an empty weapon ID in patch 13.06 and a real one from 13.08 | 17 and 141 kills | Never key on an empty ID |
| Knife kills are type `Melee` with a null name | 16 | Identify by type |
| Ability kill IDs are slot names; `GrenadeAbility` also has a null name | 276 ability kills; 22 unnamed | Key by slot |
| Spike and fall kills have an empty ID and a null name | 94 (68 spike, 26 fall) | Identify by type |
| The spike killed an enemy, with killer team `Unknown` | 1 | No kill credit |
| Sidearms don't show as the round's loadout weapon | 4 kills | The loadout weapon isn't the kill weapon |
| Earliest kill tied in a round | 3 | Omit `first_deaths` for that round |
| Tier 0 in Competitive (unranked, placements) | 12 rows | Unknown rank |
| Unrated reports tier 0, named "Unrated" | 20 rows in 2 matches | Rank stats are Competitive only |
| Null Performance Score and inputs | `score` 2 rows; breakdown `plants` 396 and `defuses` 590 rows | Unknown |
| No loadout weapon; no armor | 59 and 3,133 of 18,000 player-rounds | Unknown weapon; no armor bought |
| `P.drafted_ability_casts`; per-round `S.ability_casts` | Always null | Not used |

## Not available from this data

- Headshot kills (only hits are recorded) and wallbangs.
- Utility effects: how many enemies a reveal, flash, stun or slow hit. Riot's own match format has per-round effect fields, but they're always null.
- Time alive (no round end time), local time of day (no timezone) and who carried the spike.
- Earned RR, current rank and peak rank. `party_rr_penaltys` is party penalty metadata, not RR earned.
- Exactly what was bought versus picked up.
