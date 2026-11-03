# Player match stats catalogue

Scope: VALORANT Competitive and Unrated only. Accept `metadata.queue.id` equal to `competitive` or `unrated`; do not filter on `mode_type` (`Standard` is not the queue). The HenrikDev response reviewed for this catalogue contained 10 Competitive matches, 100 player records and no Unrated examples. Unrated field availability remains unverified. The separate Riot response was not a source for this catalogue. The sample response files are not retained in the repository.

This is the proposed storage vocabulary, not an implemented ingestion schema: the service is still a stub. Existing `PlayerMatchStats` top-level fields and example map keys are preserved; additional map keys below are proposed here. Every key is case-sensitive. All ordinary rows have comparison scope **None**; explicit match/team comparisons are in their own table.

## Source notation and storage rules

`M` = `$.data[i]`; `P` = `M.players[j]` for the linked player (join by `puuid`, never display name); `T` = `M.teams[k]` where `team_id == P.team_id`; `R` = each `M.rounds[r]`; `S` = `R.stats[s]` where `player.puuid == P.puuid`; `K` = each `M.kills[k]`. These aliases expand to exact JSON paths.

Store one record per `(user_id, game, match_id)`. Use all participants temporarily to calculate comparisons, but persist records only for linked players. PUUIDs are internal join identifiers and must not be sent to the browser. Missing/null is unknown: omit a map entry rather than converting it to zero or the string `null`. Counts derived from events are zero only when the event collection is complete. Do not treat a missing team or round as a loss. Require complete matches (`M.metadata.is_completed == true`) and complete participant/round data for derived stats; otherwise omit affected values. Preserve numeric precision and round only for display.

## Top-level fields

| Exact key | Meaning | Source |
|---|---|---|
| `match_id` | Match identifier | `M.metadata.match_id` |
| `user_id` | FriendlyFire linked user | Internal account linkage, not HenrikDev JSON |
| `game` | Game discriminator | Constant `valorant` |
| `played_at` | Match start timestamp | `M.metadata.started_at` |
| `won` | Whether this player’s team won | `T.won`; false also covers a draw, so use `labels.result` to distinguish |
| `kills` | Match kills | `P.stats.kills` |
| `deaths` | Match deaths | `P.stats.deaths` |
| `assists` | Match assists | `P.stats.assists` |

## Labels (`labels` map)

| Exact key | Meaning | Source / derivation |
|---|---|---|
| `agent` | Agent name | `P.agent.name` |
| `agent_id` | Agent identifier | `P.agent.id` |
| `map` | Map name | `M.metadata.map.name` |
| `map_id` | Map identifier | `M.metadata.map.id` |
| `mode` | Queue display name | `M.metadata.queue.name` |
| `queue_id` | Queue filter identifier | `M.metadata.queue.id` |
| `mode_type` | Ruleset name | `M.metadata.queue.mode_type` |
| `team_id` | Player’s team identifier | `P.team_id` |
| `rank` | Competitive tier at this match; not current or lifetime peak rank | `P.tier.name` |
| `season` | Act identifier used for grouping | `M.metadata.season.short` |
| `season_id` | Season UUID | `M.metadata.season.id` |
| `region` | Match region | `M.metadata.region` |
| `cluster` | Server cluster | `M.metadata.cluster` |
| `platform` | Player platform | `P.platform` |
| `match_platform` | Match platform | `M.metadata.platform` |
| `game_version` | Game build | `M.metadata.game_version` |
| `result` | `win`, `loss` or `draw` | From `T.won` and `T.rounds.won/lost`: true → win; false with equal rounds → draw; false with fewer won → loss; otherwise unknown |

## Direct numbers (`numbers` map)

| Exact key | Meaning | Source / derivation |
|---|---|---|
| `headshots` | Head hits, not headshot kills | `P.stats.headshots` |
| `bodyshots` | Body hits | `P.stats.bodyshots` |
| `legshots` | Leg hits | `P.stats.legshots` |
| `damage_dealt` | Total reported damage dealt | `P.stats.damage.dealt` |
| `damage_received` | Total reported damage received | `P.stats.damage.received` |
| `combat_score` | Total combat score; not ACS or HenrikDev performance score | `P.stats.score` |
| `rounds_won` | Rounds won by player’s team | `T.rounds.won` |
| `rounds_lost` | Rounds lost by player’s team | `T.rounds.lost` |
| `rounds_played` | Completed match rounds | `T.rounds.won + T.rounds.lost`; validate against `len(M.rounds)` |
| `match_length_ms` | Match duration in milliseconds | `M.metadata.game_length_in_ms` |
| `rank_tier` | Numeric competitive tier at match time | `P.tier.id` |
| `account_level` | Account level at match time | `P.account_level` |
| `team_number` | Numeric team identifier, not scoreboard rank | `P.team_number` |
| `session_playtime_ms` | Reported session time; not lifetime or match duration | `P.session_playtime_in_ms` |
| `ability_casts_grenade` | Total casts in provider slot grenade; slot names are agent dependent | `P.ability_casts.grenade` |
| `ability_casts_ability1` | Total casts in provider slot ability1; slot names are agent dependent | `P.ability_casts.ability1` |
| `ability_casts_ability2` | Total casts in provider slot ability2; slot names are agent dependent | `P.ability_casts.ability2` |
| `ability_casts_ultimate` | Total casts in provider slot ultimate; slot names are agent dependent | `P.ability_casts.ultimate` |
| `afk_rounds` | Provider-reported AFK rounds | `P.behavior.afk_rounds` |
| `rounds_in_spawn` | Provider-reported rounds in spawn | `P.behavior.rounds_in_spawn` |
| `friendly_fire_incoming` | Provider-reported incoming friendly fire; units not established by sample | `P.behavior.friendly_fire.incoming` |
| `friendly_fire_outgoing` | Provider-reported outgoing friendly fire; units not established by sample | `P.behavior.friendly_fire.outgoing` |
| `credits_spent` | Total credits spent | `P.economy.spent.overall` |
| `credits_spent_average` | Provider average credits spent per round | `P.economy.spent.average` |
| `loadout_value` | Sum of round loadout values | `P.economy.loadout_value.overall` |
| `loadout_value_average` | Provider average loadout value per round | `P.economy.loadout_value.average` |

## Derived numbers (`numbers` map)

| Exact key | Meaning | Source / derivation |
|---|---|---|
| `first_bloods` | Rounds where player made the first kill | Count `R.first_blood.puuid == P.puuid` |
| `first_deaths` | Rounds where player was the first victim | For each `K.round`, take earliest `K.time_in_round_in_ms`; count `K.victim.puuid == P.puuid`. Equal earliest timestamps are ambiguous: omit rather than choose JSON order. |
| `plants` | Spike plants | Count non-null `R.plant.player.puuid == P.puuid` |
| `defuses` | Spike defuses | Count non-null `R.defuse.player.puuid == P.puuid` |
| `acs` | Average combat score per completed round | `P.stats.score / numbers.rounds_played` |
| `adr` | Average damage dealt per completed round | `P.stats.damage.dealt / numbers.rounds_played` |
| `headshot_percentage` | Percentage of reported hits that hit the head, 0–100 | `100 * P.stats.headshots / (P.stats.headshots + P.stats.bodyshots + P.stats.legshots)` |
| `kd_ratio` | Kills per death | `P.stats.kills / P.stats.deaths` |
| `kda_ratio` | Kills plus assists per death | `(P.stats.kills + P.stats.assists) / P.stats.deaths` |
| `damage_delta` | Damage dealt minus damage received | `P.stats.damage.dealt - P.stats.damage.received` |
| `max_round_damage` | Highest damage dealt in one round | For each `S`, sum `S.damage_events[*].damage`, then take max across rounds |
| `max_round_kills` | Most kills in a round | Max `S.stats.kills` across rounds |
| `max_round_combat_score` | Highest total combat score in a round | Max `S.stats.score` across rounds |
| `penalty_rounds` | Rounds with a recorded penalty | Count `S.received_penalty == true` |
| `kills_with_weapon:<weapon_id>` | Kills credited to a particular weapon or ability ID; one key per observed ID | Count `K.killer.puuid == P.puuid` grouped by `K.weapon.id`; display using `K.weapon.name`, classify using `K.weapon.type` |
| `rounds_with_kills:<n>` | Rounds with exactly n kills; n is a nonnegative integer | Count `S.stats.kills == n`; include zero-kill rounds. Five kills do not necessarily mean a five-opponent ace when resurrection occurs. |

All divisions with a zero denominator are unknown and omitted (including a deathless K/D), never infinity. `kills_with_weapon:` and `rounds_with_kills:` are exact key prefixes with a parameter after the colon; they are not literal single keys. Round economy weapons describe loadouts and must not be used to attribute kills. These round/event-derived stats are player facts, not comparisons against other players.

## Whole-match and team comparisons

These need the full scoreboard, including unlinked players; comparing only quiz participants produces a different statistic. Numeric flags are `1` for true and `0` for false, omitted when unknown.

| Exact `numbers` key | Scope | Meaning and source / derivation |
|---|---|---|
| `scoreboard_position` | Whole match | `1 + count(other M.players with higher P.stats.score / rounds_played)`; rank by unrounded ACS descending. Shared ranks for equal ACS (1, 1, 3). This is our defined ranking, not an API-supplied UI position. |
| `team_position` | Team | Same ACS ranking restricted to `M.players[*].team_id == P.team_id`. This is player position within team, not `T.placement`. |
| `team_placement` | Whole match (team result) | `T.placement`; finishing placement of the team, not the individual. |
| `match_mvp` | Whole match | `M.metadata.mvp.puuid == P.puuid`; provider MVP, not inferred from ACS. |
| `team_mvp` | Team | `T.mvp.puuid == P.puuid`; provider team MVP. |
| `topped_match_<stat>` | Whole match | Player equals the maximum eligible `<stat>` across every `M.players[*]`. |
| `topped_team_<stat>` | Team | Player equals the maximum eligible `<stat>` among players with the same `team_id`. |

The exact supported `<stat>` suffixes are `kills`, `deaths`, `assists`, `headshots`, `bodyshots`, `legshots`, `damage_dealt`, `damage_received`, `combat_score`, `acs`, `adr`, `first_bloods`, `first_deaths`, `plants`, `defuses`, `headshot_percentage`, `max_round_damage`, and `max_round_kills`. Thus `topped_match_kills` and `topped_team_plants` are keys. Sources are the corresponding direct/derived rows above, calculated for every participant. “Topped” always means highest, including deaths/damage received; it does not mean best. All tied leaders get 1. If any eligible comparison value is unknown, omit the comparison flag; never compare a partial cohort. These flags are proposed derived fields, not HenrikDev JSON fields.

## Additional observed player fields

| Exact key | Meaning | Source / derivation |
|---|---|---|
| `numbers.performance_score` | Provider performance rating; not combat score or ACS | `P.performance.score` |
| `numbers.performance_damage` | Provider performance component damage; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.damage` |
| `numbers.performance_trades` | Provider performance component trades; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.trades` |
| `numbers.performance_assists` | Provider performance component assists; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.assists` |
| `numbers.performance_utility_usage` | Provider performance component utility_usage; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.utility_usage` |
| `numbers.performance_plants` | Provider performance component plants; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.plants` |
| `numbers.performance_defuses` | Provider performance component defuses; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.defuses` |
| `numbers.performance_adjusted_kills` | Provider performance component adjusted_kills; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.adjusted_kills` |
| `numbers.performance_adjusted_deaths` | Provider performance component adjusted_deaths; weighting/normalization is not established by the saved JSON; not a raw count | `P.performance.breakdown.adjusted_deaths` |
| `labels.performance_grade` | Provider grade; not a match/team position | `P.performance.ratings.grade` |
| `labels.performance_combat_damage` | Provider categorical damage rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.combat.damage` |
| `labels.performance_combat_death_impact` | Provider categorical death_impact rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.combat.death_impact` |
| `labels.performance_combat_kill_impact` | Provider categorical kill_impact rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.combat.kill_impact` |
| `labels.performance_combat_trades` | Provider categorical trades rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.combat.trades` |
| `labels.performance_utility_assists` | Provider categorical assists rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.utility.assists` |
| `labels.performance_utility_defuses` | Provider categorical defuses rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.utility.defuses` |
| `labels.performance_utility_plants` | Provider categorical plants rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.utility.plants` |
| `labels.performance_utility_utility_usage` | Provider categorical utility_usage rating; baseline is unknown, not a verified match/team comparison | `P.performance.ratings.utility.utility_usage` |
| `labels.card_id` | Cosmetic identifier, not a performance stat | `P.customization.card` |
| `labels.title_id` | Cosmetic identifier, not a performance stat | `P.customization.title` |
| `labels.preferred_level_border_id` | Cosmetic identifier, not a performance stat | `P.customization.preferred_level_border` |
| `labels.bloomline_clip_id` | Cosmetic identifier | `P.customization.bloomline.clip_id` |
| `labels.bloomline_hype_id` | Cosmetic identifier | `P.customization.bloomline.hype_id` |
| `labels.bloomline_plank_background_id` | Cosmetic identifier | `P.customization.bloomline.plank_background_id` |
| `numbers.bloomline_level` | Cosmetic progression level | `P.customization.bloomline.level` |

These optional fields complete the inventory of non-identity scalar fields in `M.players[*]`; retain them only if needed by the question bank. Do not reverse-engineer performance component totals by multiplying rounded rates. Null performance components stay unknown.

## Identity, event detail and exclusions

- `P.puuid` is an internal join key; `P.name` and `P.tag` are display identity snapshots, not quiz stats. `P.party_id` groups parties; it is not a performance label. No public stats-map keys are assigned to these identifiers.
- `P.drafted_ability_casts` is null throughout these Standard Competitive samples; no numeric schema can be inferred. `M.metadata.premier`, `T.premier_roster`, `T.health`, and `T.team_number` are null here; Premier and other queues are out of scope. `M.observers` and `M.coaches` are empty and are not participants.
- `M.metadata.party_rr_penaltys[*].party_id/penalty` is party RR penalty metadata, not earned RR; no match RR gain/loss stat is available here.
- `R.id`, `R.result`, `R.ceremony`, `R.winning_team`, `R.winning_team_role`, `R.ceremony_player`, and `R.ceremony_team` describe rounds. Plant/defuse times (`R.plant.round_time_in_ms`, `R.defuse.round_time_in_ms`), plant site (`R.plant.site`), locations, and kill timestamps are event detail, not single per-match labels. Keep event data separately if questions need it.
- `S.stats.score/kills/headshots/bodyshots/legshots`, `S.damage_events[*].player.puuid/damage/headshots/bodyshots/legshots`, and `S.economy.loadout_value/remaining/weapon/armor` support round questions. `S.was_afk`, `S.stayed_in_spawn`, and `S.received_penalty` are round flags. Round `S.ability_casts.ability_1/ability_2` use underscores unlike match `P.ability_casts.ability1/ability2`; null round casts do not mean no abilities used.
- `K.weapon.id/name/type`, `K.killer`, `K.victim`, `K.assistants`, `K.round`, `K.time_in_round_in_ms`, `K.time_in_match_in_ms`, and `K.secondary_fire_mode` describe kill events. Coordinates and `player_locations[*].view_radians` are telemetry, not per-match stats. Do not label head hits as headshot kills or loadout weapons as kill weapons.

## Question bank availability

The [sample question bank](https://tamucs-my.sharepoint.com/:w:/r/personal/akhil_tamu_tamu_edu/Documents/FF%20Sample%20Trivia%20Questions.docx?d=wcf956a5805f44fe8ad8cd919daaeca92&csf=1&web=1&e=qwkXPA) can use agent/map/act filters and aggregate these per-match records for recent-match kills, damage, ACS, ADR, K/D, headshot percentage, agent usage and win rates. Define the time window and weighting explicitly: pooled K/D = sum kills / sum deaths; pooled ACS/ADR = total score/damage / total rounds; pooled headshot percentage = total head hits / total hits. A mean of match percentages answers a different question. Win rate = wins / completed matches in scope (draws count as non-wins).

Current competitive rank, lifetime peak rank, lifetime hours played and lifetime win rate cannot be established from these ten matches. `tier` is a match snapshot, `session_playtime_in_ms` is not lifetime hours, and “highest rank in the past X games” means highest observed tier in that window. Competitive rank questions must exclude Unrated even if a tier is present. “This Act” totals and time-window records require complete history for that window; saved samples alone are not complete account history.
