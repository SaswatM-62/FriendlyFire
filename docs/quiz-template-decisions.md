# Question bank: last ten games

Source: [FF Sample Trivia Questions](https://docs.google.com/document/d/1WC-Wg2kSI00NNJlmQHwwlH4p028KDYKcDwnkz-hF8m0/edit?tab=t.0).
The author confirmed that the first block is approved. A `*` after a question
means Akhil has claimed it; the document has no claim column or review statuses.

## Agreed scope and calculations

- Each new template uses each player's ten newest available VALORANT Competitive
  or Unrated games, ordered by `played_at`. Require ten games per player. A missing
  or invalid timestamp, or a tie across the tenth-game boundary, skips the question.
- Select the ten games before filtering by agent or map. Do not backfill from older
  games. Rank means the highest observed competitive tier within those ten games;
  Unrated rank values are excluded. This is neither current nor lifetime peak rank.
- Skip a comparison if any quiz player lacks a required value in the applicable
  scope, or if leaders tie. A missing label cannot silently remove a game. No games
  on a map or agent means an unknown ratio; known zero agent usage remains zero.
- K/D, headshot percentage and ADR pool raw totals, rather than averaging match
  ratios. Zero total denominators are unknown. A deathless individual match may
  contribute to a pooled K/D with a positive total denominator.
- Headshots mean head hits, not headshot kills. Map win rate counts draws as
  non-wins and requires `labels.result` to agree with `won`.
- Check duplication by statistic, scope and calculation, rather than wording.
  Highest and lowest ADR ask different comparisons and were separately approved
  questions. Existing maximum-kills, pooled-ACS and recent-wins templates are not
  counted among the ten additions.
- Leave the FF-402 generator and its template contract unchanged. Both existing
  source files match `feat/FF-402-question-engine` (abfc361) exactly. Keep the ten
  question specifications in `recent_templates.py`, outside the runtime registry.
  Akhil will discuss the missing integration support with the owner at the meeting.
  No changes to another contributor's code are part of this proposal.

## Claimed templates

| First-block question | Template ID | Calculation within last ten games |
| --- | --- | --- |
| Highest rank in the past X games | `highest-recent-rank-v1` | Maximum competitive `rank_tier` |
| Overall K/D | `highest-recent-kd-v1` | Total kills / total deaths |
| Highest average headshot percentage | `highest-recent-headshot-percentage-v1` | Total head hits / total hits |
| Most headshots in a single match | `most-recent-headshots-v1` | Maximum match head hits |
| Most games on a specific agent | `most-recent-agent-games-v1` | Count games using that agent |
| Highest win rate on a map | `highest-recent-map-win-rate-v1` | Wins / games on that map |
| Highest K/D with an agent | `highest-recent-agent-kd-v1` | Agent kills / agent deaths |
| Lowest ADR | `lowest-recent-adr-v1` | Minimum total damage / total rounds |
| Highest ADR | `highest-recent-adr-v1` | Maximum total damage / total rounds |
| Highest damage in one round | `highest-recent-round-damage-v1` | Maximum `max_round_damage` |

## Contract limits and completion

Proto3 `kills`, `deaths`, and `won` have no presence information. The generator
cannot distinguish an omitted scalar from a legitimate zero/false; ingestion must
supply them as required by the existing contract. Map entries preserve presence
and are checked explicitly. No proto or generated files are changed.

The ten definitions have specification checks for distinct comparisons, last-ten
scope, required fields and renderable wording. They are not registered with the
existing generator. Runtime fixture checks from the initial implementation were
removed with the generator changes; the earlier 173-test result does not describe
this revised proposal.

## Meeting concerns and remaining work

The existing generator supports only maximum kills, pooled ACS and recent wins.
The new questions need additional calculations, last-ten windows for those
calculations, agent/map parameters and a minimum comparison for lowest ADR. Its
exhaustive candidate selection may also need review before expanding the bank.
The generator owner should agree on the extension interface and implement these
changes before registering the new definitions.

After integration, each question still needs fixture tests proving a unique
correct player, skipped tied leaders and skipped missing required data. Verify
pooled arithmetic, timestamp cutoffs, filter order and deterministic generation
as well. These runtime acceptance tests and merging are still outstanding.

The document now marks these questions as claimed, with definitions prepared and
integration pending. The task is not complete. Akhil will open the PR himself.
