"""Data-only question templates; question_generator.py evaluates them.

Catalogue: docs/stats-catalogue.md. A new question is one more entry here; only a
new kind of calculation needs code in question_generator.py. Templates carry no
difficulty: each generated question is rated easy, medium or hard from how far its
winner leads (question_generator.difficulty).

How a template is evaluated, for every room player who is compared (see below):

1. Rows: the player's matches in the template's game and queues, and in the scope
   (all matches, "shared" ones with 2+ room players, or "solo" ones).
2. Window: the player's newest last_n matches by played_at (fewer than last_n, a
   missing timestamp, or a tie across the boundary skips the question), or every
   stored match when last_n is None. The window is chosen before any filter. With
   windows, each window is its own version of the question (see QuestionTemplate).
3. Filter: with a parameter_label, one question per value of that label (e.g. per
   agent), keeping only the window's matches with that value. A match missing the
   label skips the question rather than silently dropping out.
4. Value, by rule:
   - max_in_match: the player's best single match for stat (highest, or lowest
     when direction is "lowest"); display_label names a label from that match,
     shown as {<display_label>_name}. A match missing the stat makes the value
     unknown, unless skip_missing is set: then matches without it are left out (for
     stats that only exist when something happened, like longest_kill_m in a match
     with no kills), and only a player with no known match is unknown.
   - count: the sum of stat over the matches ("matches" counts them, "won" counts
     wins). No matches after a filter is a known zero.
   - average: stat averaged per match.
   - pooled_ratio: the sum of the numerator stats over the sum of the denominator
     stats. A zero total denominator is unknown; one zero match still counts.
   - pair: friend-pair counts. One question per room player (the {friend}); each
     other player's value is the sum of the "<stat>:<friend's user ID>" number over
     matches where they were on the friend's team.
   - keyed: stat names a keyed family ("numbers.kills_with_weapon"). One question per
     key that a compared player's window holds, shown as {parameter} from the row's
     "name:<key>" label (a key without a name is never asked); each player's value is
     the sum of "<stat>:<key>" over the matches, a match without it counting as 0.
   - range: the player's highest minus their lowest value of stat across the
     matches; {numerator} is the highest and {denominator} the lowest. It needs two
     matches.
5. Compare: the unique highest (or lowest) value wins; a tie skips the question,
   and so does a {value} or {percent} in the question that rounds to the same text
   as the runner-up's (it wouldn't single out the winner). Any player with a
   missing value skips it too, except that with contenders_only players without
   enough matches sit the question out (2+ must remain). "Enough" means at least
   one match in the scope (and at least last_n of them), plus one after the filter,
   and for a pooled_ratio with min_denominator, at least that much denominator in
   those matches (e.g. 5 clutch attempts, so 1 of 1 can't beat 9 of 20; a player with
   none sits out instead of skipping the question). Use it for agent or map
   questions, and for the solo scope: players who mostly squad up still have the
   odd solo game, so pair a solo template with contenders_only and a small or no
   last_n. Pair questions always compare only the
   players who played with the friend.

Every room player is an option in every question, including players who sat it out,
so a question says who it compared ("of anyone here who has played Ascent"). Players
with QuizPlayer.not_enough_matches are never compared: never the answer, a pair
question's friend or the source of a stat. Their matches still count when deciding
whether a match was shared.

Stats: "kills", "deaths", "assists", "won" (needs a labels.result that agrees),
"matches", or "numbers.<key>" from the catalogue. Match Data writes some keys only
when they're above zero (one key of a keyed family, like "rounds_with_kills:3"):
set absent_is_zero so a match without the key counts as 0 instead of missing.

Placeholders: {name} (winner), {value}, {numerator}, {denominator}, {percent}
(value x 100), {scope}, {last_n}, {parameter}, {friend} and {<display_label>_name}.
Whole numbers show as they are; others round half up to 2 decimals (1.28), or 1
for {percent} (29.7). {singular|plural}, e.g. "{value} {time|times}", shows the first
form when {value} shows as 1 and the second otherwise. value_scale multiplies {value}
for display only, e.g. 0.001 to show milliseconds as seconds.
"""

from dataclasses import dataclass
from typing import Literal

Scope = Literal["all", "shared", "solo"]
Rule = Literal["max_in_match", "count", "average", "pooled_ratio", "pair", "keyed", "range"]
Direction = Literal["highest", "lowest"]
ParameterLabel = Literal["", "agent", "map", "role"]

ALL_SCOPES: tuple[Scope, ...] = ("all", "shared", "solo")


@dataclass(frozen=True)
class QuestionTemplate:
    id: str
    question: str
    explanation: str
    # Every catalogue key the template reads, for reviewers.
    stats: tuple[str, ...]
    rule: Rule
    stat: str = ""
    numerator: tuple[str, ...] = ()
    denominator: tuple[str, ...] = ()
    direction: Direction = "highest"
    scopes: tuple[Scope, ...] = ("all",)
    game: str = "valorant"
    queues: tuple[str, ...] = ("competitive", "unrated")
    last_n: int | None = 10
    parameter_label: ParameterLabel = ""
    display_label: str = ""
    contenders_only: bool = False
    # pooled_ratio with contenders_only: the least denominator a player needs to be
    # compared (see Compare above); 0 means no minimum.
    min_denominator: int = 0
    # max_in_match only: leave out matches that don't have the stat (see above).
    skip_missing: bool = False
    # Instead of last_n, which must then be None: one version of the question per window,
    # e.g. (5, 10, None) for the last 5 matches, the last 10 and every stored match, in
    # each scope. {scope} then says how many matches too ("in their last 5 matches with
    # other room players"), and {last_n} can't be used. A quiz asks one version at most.
    windows: tuple[int | None, ...] = ()
    # Count a match without a numbers.<key> stat as 0 instead of missing (see Stats).
    absent_is_zero: bool = False
    # {value} is shown multiplied by this (0.001: milliseconds as seconds).
    value_scale: float = 1


TEMPLATES = (
    # FF-404, question pack 1.
    QuestionTemplate(
        id="highest-kd-v1",
        question="For every death {scope}, this player gets {value} kills, the best ratio in "
        "this group. Who comes out on top in their fights?",
        explanation="{name}: {numerator} kills and {denominator} deaths.",
        stats=("kills", "deaths", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("deaths",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="headshot-percentage-v1",
        question="In their last {last_n} matches, {percent}% of this player's hits landed on "
        "the head, the best aim in this group. Who keeps clicking heads?",
        explanation="{name}: {numerator} headshots out of {denominator} hits.",
        stats=(
            "numbers.headshots",
            "numbers.bodyshots",
            "numbers.legshots",
            "played_at",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.headshots",),
        denominator=("numbers.headshots", "numbers.bodyshots", "numbers.legshots"),
        last_n=5,
    ),
    QuestionTemplate(
        id="most-headshots-in-a-match-v1",
        question="One of you landed {value} headshots in a single match {scope}, more than "
        "anyone else here has managed. Who was seeing heads that day?",
        explanation="{name}, playing {agent_name}: {value} headshots in one match.",
        stats=("numbers.headshots", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.headshots",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="most-agent-games-v1",
        question="This player picked {parameter} in {value} of their last {last_n} matches, "
        "more than anyone else here. Who is the group's {parameter} main?",
        explanation="{name} played {parameter} in {value} of their last {last_n} matches.",
        stats=("matches", "labels.agent", "played_at", "labels.queue_id"),
        rule="count",
        stat="matches",
        last_n=5,
        parameter_label="agent",
        contenders_only=True,
    ),
    QuestionTemplate(
        id="map-win-rate-v1",
        question="On {parameter}, this player wins {percent}% of their matches, the best "
        "record of anyone here who has played it. Who owns {parameter}?",
        explanation="{name} won {numerator} of {denominator} matches on {parameter}.",
        stats=("won", "matches", "labels.result", "labels.map", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("won",),
        denominator=("matches",),
        last_n=None,
        parameter_label="map",
        contenders_only=True,
    ),
    QuestionTemplate(
        id="agent-kills-per-round-v1",
        question="As {parameter}, this player averages {value} kills per round, the most of "
        "anyone here who has played {parameter}. Who is the deadliest {parameter}?",
        explanation="{name}: {numerator} kills in {denominator} rounds as {parameter}.",
        stats=("kills", "numbers.rounds_played", "labels.agent", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("numbers.rounds_played",),
        last_n=None,
        parameter_label="agent",
        contenders_only=True,
    ),
    QuestionTemplate(
        id="lowest-adr-v1",
        question="This player deals {value} damage per round {scope}, the lowest in this group. "
        "Who's been going easy on the enemy?",
        explanation="{name}: {numerator} damage over {denominator} rounds.",
        stats=("numbers.damage_dealt", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.damage_dealt",),
        denominator=("numbers.rounds_played",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="highest-adr-v1",
        question="Nobody here hits harder: this player deals {value} damage per round "
        "{scope}. Who brings the pain?",
        explanation="{name}: {numerator} damage over {denominator} rounds.",
        stats=("numbers.damage_dealt", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.damage_dealt",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="kast-v1",
        question="Kill, assist, survive or get traded: this player did one of those in "
        "{percent}% of their rounds {scope}, the most consistent in this group. Who always "
        "pulls their weight?",
        explanation="{name} made an impact in {numerator} of {denominator} rounds.",
        stats=("numbers.kast_rounds", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.kast_rounds",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="round-winning-kills-v1",
        question="When a round ends with the last enemy down, this player is often the one "
        "who did it: {value} {time|times} in their last {last_n} matches, the most in this "
        "group. Who closes rounds out?",
        explanation="{name} got the final kill of {value} won {round|rounds} in their last "
        "{last_n} matches.",
        stats=("numbers.round_winning_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.round_winning_kills",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-kills-v1",
        question="This player once racked up {value} kills in a single match {scope}, the most "
        "in this group. Who might this player be?",
        explanation="{name}, with {value} kills in one match.",
        stats=("kills", "labels.queue_id"),
        rule="max_in_match",
        stat="kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # FF-410, question pack 7.
    QuestionTemplate(
        id="most-recent-kills-v1",
        question="Across their last {last_n} matches, this player piled up {value} "
        "{kill|kills}, more than anyone else here. Who's been on a tear lately?",
        explanation="{name}: {value} {kill|kills} in {last_n} matches.",
        stats=("kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="kills",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-assists-v1",
        question="Nobody here has helped out more lately: this player picked up {value} "
        "{assist|assists} in their last {last_n} matches. Who is the ultimate teammate?",
        explanation="{name}: {value} {assist|assists} in {last_n} matches.",
        stats=("assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="assists",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-deaths-v1",
        question="Over their last {last_n} matches, this player died {value} {time|times}, more "
        "than anyone else here. Who's spent the most time spectating?",
        explanation="{name}: {value} {death|deaths} in {last_n} matches.",
        stats=("deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-plants-v1",
        question="Someone here planted the spike {value} {time|times} in their last {last_n} "
        "matches, the most in this group. Who keeps the spike moving?",
        explanation="{name}: {value} {plant|plants} in {last_n} matches.",
        stats=("numbers.plants", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.plants",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-defuses-v1",
        question="This player has defused the spike {value} {time|times} {scope}, more than anyone "
        "else here. Who does the team trust with the defuse?",
        explanation="{name}: {value} {defuse|defuses}.",
        stats=("numbers.defuses", "labels.queue_id"),
        rule="count",
        stat="numbers.defuses",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="highest-recent-kda-v1",
        question="Counting assists as well as kills, this player gets {value} for every death "
        "in their last {last_n} matches, the best in this group. Who makes every life count?",
        explanation="{name}: {numerator} kills and assists against {denominator} deaths.",
        stats=("kills", "assists", "deaths", "played_at", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("kills", "assists"),
        denominator=("deaths",),
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-first-deaths-v1",
        question="Rounds keep starting badly for this player: they were the first one down "
        "{value} {time|times} in their last {last_n} matches, more than anyone else here. "
        "Who needs to stop peeking first?",
        explanation="{name} was the first player killed in {value} {round|rounds} over "
        "{last_n} matches.",
        stats=("numbers.first_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.first_deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-isolated-deaths-v1",
        question="With no teammate within 20 meters, this player went down alone {value} "
        "{time|times} in their last {last_n} matches, the most in this group. Who keeps "
        "wandering off by themselves?",
        explanation="{name}: {value} {death|deaths} with no teammate nearby in {last_n} matches.",
        stats=("numbers.isolated_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.isolated_deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-deathless-rounds-v1",
        question="This player made it through {value} {round|rounds} without dying in their last "
        "{last_n} matches, the most in this group. Who has the best survival instincts?",
        explanation="{name} survived {value} {round|rounds} in {last_n} matches.",
        stats=("numbers.deathless_rounds", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.deathless_rounds",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-sniper-kills-v1",
        question="Behind a scope, this player has picked up {value} {kill|kills} {scope}, more "
        "than anyone else here. Who's the group's sniper?",
        explanation="{name}: {value} {kill|kills} with sniper rifles.",
        stats=("numbers.sniper_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.sniper_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="highest-acs-v1",
        question="This player averages {value} combat score per round {scope}, the highest "
        "in this group. Who is this player?",
        explanation="{name} had the highest pooled ACS {scope}: {value} "
        "({numerator} total combat score / {denominator} rounds).",
        stats=("numbers.combat_score", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.combat_score",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="recent-wins-v1",
        question="This player won {value} of their last {last_n} Competitive matches, more "
        "than anyone else in this group. Who keeps winning?",
        explanation="{name} won {value} of their last {last_n} available competitive matches "
        "(draws count as non-wins).",
        stats=("played_at", "labels.queue_id", "labels.result", "won"),
        rule="count",
        stat="won",
        queues=("competitive",),
        last_n=10,
    ),
    QuestionTemplate(
        id="max-round-damage-v1",
        question="One of you dealt {value} damage to enemies in a single round {scope}, "
        "more than anyone else here has managed. Who pulled that off?",
        explanation="{name}, playing {agent_name}: {value} damage in one round.",
        stats=("numbers.max_round_damage", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_round_damage",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="recent-first-bloods-v1",
        question="Over their last {last_n} matches, this player drew first blood "
        "{value} {time|times}, the most in this group. Who is this player?",
        explanation=(
            "{name} opened {value} {round|rounds} with the first kill in their last {last_n} "
            "matches."
        ),
        stats=("numbers.first_bloods", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.first_bloods",
        last_n=5,
    ),
    QuestionTemplate(
        id="average-performance-score-v1",
        question="This player averages a Performance Score of {value} per match {scope}, the "
        "highest in this group. Who sets the pace?",
        explanation="{name}, averaging {value} over {denominator} matches.",
        stats=("numbers.performance_score", "labels.queue_id"),
        rule="average",
        stat="numbers.performance_score",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="opening-duel-win-rate-v1",
        question="This player wins {percent}% of their opening duels {scope}, "
        "the best record of anyone here with 10 or more. Who takes the first fight?",
        explanation="{name}: {numerator} first bloods out of {denominator} opening duels.",
        stats=("numbers.first_bloods", "numbers.first_deaths", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.first_bloods",),
        # The catalogue leaves first_deaths out of a round whose two earliest kills share
        # a time, but not first_bloods: such a round counts as a duel won, never as one
        # lost. It's rare (3 rounds in the sample data), so the rate is close enough.
        denominator=("numbers.first_bloods", "numbers.first_deaths"),
        scopes=ALL_SCOPES,
        last_n=None,
        contenders_only=True,
        min_denominator=10,
    ),
    QuestionTemplate(
        id="max-round-kills-v1",
        question="One of you got {value} {kill|kills} in a single round {scope}, and nobody "
        "else here has matched it. Who is this player?",
        explanation="{name}, playing {agent_name}: {value} {kill|kills} in one round.",
        stats=("numbers.max_round_kills", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_round_kills",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="post-plant-kills-v1",
        question="Once the spike is down, this player gets to work: {value} post-plant "
        "{kill|kills} in their last {last_n} matches, the most in this group. Who might this "
        "player be?",
        explanation="{name} got {value} {kill|kills} after the plant in their last {last_n} "
        "matches.",
        stats=("numbers.post_plant_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.post_plant_kills",
        last_n=5,
    ),
    QuestionTemplate(
        id="ninja-defuses-v1",
        question="Someone here defused the spike {value} {time|times} with enemies still alive "
        "in the last 30 days. Which of you has that unique distinction?",
        explanation="{name}: {value} ninja {defuse|defuses} in the last 30 days.",
        stats=("numbers.ninja_defuses", "labels.queue_id"),
        rule="count",
        stat="numbers.ninja_defuses",
        # Rare: every stored match (the last 30 days), or most rooms would tie at 0.
        last_n=None,
    ),
    QuestionTemplate(
        id="clutch-win-rate-v1",
        question="Left as the last one standing, this player wins {percent}% of their clutches "
        "{scope}. Nobody here with 5 or more clutch attempts does better. Who stays cold "
        "under pressure?",
        explanation="{name}: {numerator} clutch wins out of {denominator} clutch attempts.",
        stats=("numbers.clutch_wins", "numbers.clutch_attempts", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.clutch_wins",),
        denominator=("numbers.clutch_attempts",),
        scopes=ALL_SCOPES,
        last_n=None,
        # So 1 of 1 can't beat 9 of 20; players with fewer attempts sit it out.
        contenders_only=True,
        min_denominator=5,
    ),
    QuestionTemplate(
        id="fall-deaths-v1",
        question="In the last 30 days, this player fell to their death {value} {time|times}, "
        "more than anyone else here. Who does this unfortunate stat belong to?",
        explanation="{name} recorded {value} fall {death|deaths} in the last 30 days.",
        stats=("numbers.fall_deaths", "labels.queue_id"),
        rule="count",
        stat="numbers.fall_deaths",
        # Rare: every stored match (the last 30 days), or most rooms would tie at 0.
        last_n=None,
    ),
    QuestionTemplate(
        id="pistol-round-kills-v1",
        question="This player averages {value} pistol-round {kill|kills} per match {scope}, the "
        "most in this group. Who starts each half with a bang?",
        explanation="{name}: {numerator} pistol-round kills in {denominator} matches.",
        stats=("numbers.pistol_round_kills", "labels.queue_id"),
        rule="average",
        stat="numbers.pistol_round_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # --- Batch 2: ten new, original questions -------------------------------------------
    # 1. Single-match record: longest kill distance, with the map it happened on.
    QuestionTemplate(
        id="longest-kill-v1",
        question="One of you once landed a kill from {value} meters away {scope}, the longest shot "
        "in this group. Who can hit a target from across the map?",
        explanation="{name}, on {map_name}: a kill from {value} meters out.",
        stats=("numbers.longest_kill_m", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.longest_kill_m",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
        # A match with no kills has no longest kill.
        skip_missing=True,
    ),
    # 2. Totals: assists that came from pure utility (no damage on the victim).
    QuestionTemplate(
        id="utility-assists-v1",
        question="Flashes and utility did the work: this player earned {value} "
        "{assist|assists} without damaging the victim {scope}, the most in this group. "
        "Who sets everyone else up?",
        explanation="{name}: {value} {assist|assists} with no damage dealt.",
        stats=("numbers.utility_assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.utility_assists",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    # 3. Rates: share of kills made against a pricier loadout.
    QuestionTemplate(
        id="underdog-kill-share-v1",
        question="{percent}% of this player's kills came against a better-equipped opponent "
        "{scope}, the highest share in this group. Who wins the fights they shouldn't?",
        explanation="{name}: {numerator} of {denominator} kills against a pricier loadout.",
        stats=("numbers.outgunned_kills", "kills", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.outgunned_kills",),
        denominator=("kills",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # 4. Agent, map and role: non-ultimate ability casts per round, one question per role.
    QuestionTemplate(
        id="role-utility-per-round-v1",
        question="Playing {parameter} {scope}, this player casts {value} non-ultimate "
        "{ability|abilities} per round, the most of anyone here who has played that role. "
        "Who keeps the utility flowing?",
        explanation="{name}: {numerator} ability casts in {denominator} rounds as {parameter}.",
        stats=(
            "numbers.ability_casts_grenade",
            "numbers.ability_casts_ability1",
            "numbers.ability_casts_ability2",
            "numbers.rounds_played",
            "labels.role",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=(
            "numbers.ability_casts_grenade",
            "numbers.ability_casts_ability1",
            "numbers.ability_casts_ability2",
        ),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
        parameter_label="role",
        contenders_only=True,
    ),
    # 5. Friend pairs: who avenges a given friend's deaths the most.
    QuestionTemplate(
        id="trades-for-friend-v1",
        question="Someone here has avenged {friend} {value} {time|times} {scope} by killing their "
        "killer within 3 seconds, more than anyone else who has played on {friend}'s team. "
        "Who has {friend}'s back?",
        explanation="{name}, across {denominator} matches together.",
        stats=("numbers.trades_for:<user_id>", "labels.team_id", "labels.queue_id"),
        rule="pair",
        stat="numbers.trades_for",
        last_n=None,
    ),
    # 6. Opponents: kills on the other team's MVP.
    QuestionTemplate(
        id="kills-on-enemy-mvp-v1",
        question="This player took down the other team's MVP {value} {time|times} {scope}, "
        "the most in this group. Who hunts the star player?",
        explanation="{name} killed the opposing MVP {value} {time|times}.",
        stats=("numbers.kills_on_enemy_mvp", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.kills_on_enemy_mvp",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    # 7. Scoreboard standing: team MVP awards.
    QuestionTemplate(
        id="team-mvps-v1",
        question="This player was their team's MVP {value} {time|times} {scope}, more than "
        "anyone else in this group. Who carries the squad?",
        explanation="{name}: team MVP {value} {time|times}.",
        stats=("numbers.team_mvp", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.team_mvp",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    # 8. Team-shared results: thrifty rounds, solo games only (teammates share the value).
    QuestionTemplate(
        id="solo-thrifty-rounds-v1",
        question="Queueing without the rest of you, this player's team wins {value} thrifty "
        "{round|rounds} per match (rounds won against much bigger buys), the most of anyone here "
        "who plays solo. Who wins on a budget?",
        explanation="{name}: {numerator} thrifty rounds in {denominator} solo matches.",
        stats=("numbers.thrifty_rounds", "labels.queue_id"),
        rule="average",
        stat="numbers.thrifty_rounds",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    # 9. Rounds and economy: rounds that started with no armor (a friendly-ribbing stat).
    QuestionTemplate(
        id="unarmored-rounds-v1",
        question="Across their last {last_n} matches, this player headed into {value} "
        "{round|rounds} with no armor, more than anyone else here. Who's playing on hard mode?",
        explanation="{name} started {value} {round|rounds} unarmored over their last {last_n} "
        "matches.",
        stats=("numbers.rounds_without_armor", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.rounds_without_armor",
        last_n=5,
    ),
    # 10. Single-match record (a blunder): most deaths to one opponent, with the agent played.
    QuestionTemplate(
        id="most-deaths-to-one-opponent-v1",
        question="One of you once died {value} {time|times} to the same opponent in a single match "
        "{scope}, more than anyone else here. Who got farmed that day?",
        explanation="{name}, playing {agent_name}: {value} {death|deaths} to one opponent.",
        stats=("numbers.max_deaths_to_one_opponent", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_deaths_to_one_opponent",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    # FF-405, question pack 2: multi-kills and fights.
    QuestionTemplate(
        id="aces-v1",
        question="Five enemies, one player: someone here has pulled off {value} {ace|aces} "
        "{scope}, more than anyone else. Who wiped out a whole team?",
        explanation="{name}: {value} {ace|aces}.",
        stats=("numbers.aces", "labels.queue_id"),
        rule="count",
        stat="numbers.aces",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="clutches-v1",
        question="Left alone against the odds, this player won {value} {clutch|clutches} {scope}, "
        "the most in this group. Who's the clutch king?",
        explanation="{name}: {value} {clutch|clutches}.",
        stats=("numbers.clutches", "labels.queue_id"),
        rule="count",
        stat="numbers.clutches",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="triple-kill-rounds-v1",
        question="This player got exactly three kills in {value} {round|rounds} {scope}, the most "
        "in this group. Who keeps finding triples?",
        explanation="{name}: {value} three-kill {round|rounds}.",
        stats=("numbers.rounds_with_kills:3", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.rounds_with_kills:3",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
        absent_is_zero=True,
    ),
    QuestionTemplate(
        id="four-kill-rounds-v1",
        question="Four kills in a single round, {value} {time|times} {scope}: nobody here has "
        "done it more. Who was one kill away from an ace?",
        explanation="{name}: {value} four-kill {round|rounds}.",
        stats=("numbers.rounds_with_kills:4", "labels.queue_id"),
        rule="count",
        stat="numbers.rounds_with_kills:4",
        scopes=ALL_SCOPES,
        last_n=None,
        absent_is_zero=True,
    ),
    QuestionTemplate(
        id="kill-less-rounds-v1",
        question="This player went without a kill in {percent}% of their rounds {scope}, more "
        "than anyone else here. Who's been a little too quiet?",
        explanation="{name}: no kill in {numerator} of {denominator} rounds.",
        stats=("numbers.rounds_with_kills:0", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.rounds_with_kills:0",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
        absent_is_zero=True,
    ),
    QuestionTemplate(
        id="double-kills-under-a-second-v1",
        question="Two kills less than a second apart: this player has done it {value} "
        "{time|times} {scope}, the most in this group. Whose reflexes are the fastest?",
        explanation="{name}: {value} {pair|pairs} of kills under a second apart.",
        stats=("numbers.double_kills_under_1s", "labels.queue_id"),
        rule="count",
        stat="numbers.double_kills_under_1s",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="untouched-multikills-v1",
        question="Without taking a single hit, this player got two or more kills in {value} "
        "{round|rounds} {scope}, the most in this group. Who wins fights without a scratch?",
        explanation="{name}: {value} untouched multi-kill {round|rounds}.",
        stats=("numbers.untouched_multikill_rounds", "labels.queue_id"),
        rule="count",
        stat="numbers.untouched_multikill_rounds",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="trade-kills-v1",
        question="When a teammate went down, this player paid it back within 3 seconds {value} "
        "{time|times} in their last {last_n} matches, the most in this group. Who always gets the "
        "trade?",
        explanation="{name}: {value} trade {kill|kills} in {last_n} matches.",
        stats=("numbers.trade_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.trade_kills",
        last_n=5,
    ),
    QuestionTemplate(
        id="traded-deaths-v1",
        question="Someone always had this player's back: {value} of their deaths {was|were} "
        "traded within 3 seconds in their last {last_n} matches, the most in this group. Who has "
        "the best backup?",
        explanation="{name}: {value} traded {death|deaths} in {last_n} matches.",
        stats=("numbers.traded_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.traded_deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="killed-then-died-v1",
        question="A kill, then a death within 3 seconds: it happened to this player {value} "
        "{time|times} in their last {last_n} matches, more than anyone else here. Who can't enjoy "
        "a kill for long?",
        explanation="{name}: {value} {kill|kills} followed by a quick death.",
        stats=("numbers.killed_then_died", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.killed_then_died",
        last_n=5,
    ),
    QuestionTemplate(
        id="revenge-kills-v1",
        question="Payback: this player took down an opponent who had killed them {value} "
        "{time|times} in their last {last_n} matches, the most in this group. Who never forgets?",
        explanation="{name}: {value} revenge {kill|kills} in {last_n} matches.",
        stats=("numbers.revenge_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.revenge_kills",
        last_n=5,
    ),
    QuestionTemplate(
        id="dominations-v1",
        question="This player dominated an opponent, with six or more kills on them than deaths to "
        "them, {value} {time|times} {scope}, the most in this group. Who finds one opponent and "
        "won't let go?",
        explanation="{name}: {value} {domination|dominations}.",
        stats=("numbers.dominations", "labels.queue_id"),
        rule="count",
        stat="numbers.dominations",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="dominated-by-v1",
        question="Dominated: an opponent killed this player at least six more times than they "
        "died back, {value} {time|times} {scope}, more than anyone else here. Who has a nemesis?",
        explanation="{name}: dominated by {value} {opponent|opponents}.",
        stats=("numbers.dominated_by", "labels.queue_id"),
        rule="count",
        stat="numbers.dominated_by",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="max-duel-margin-v1",
        question="One of you once beat a single opponent by {value} {kill|kills} in one match "
        "{scope}, the biggest one-on-one margin in this group. Who owned that matchup?",
        explanation="{name}, playing {agent_name}: up {value} {kill|kills} on one opponent.",
        stats=("numbers.max_duel_margin", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_duel_margin",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="most-kills-on-one-opponent-v1",
        question="Someone here killed the same opponent {value} {time|times} in a single match "
        "{scope}, more than anyone else managed. Who had a favourite target?",
        explanation="{name}, on {map_name}: {value} {kill|kills} on one opponent.",
        stats=("numbers.max_kills_one_opponent", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_kills_one_opponent",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
    ),
    # FF-406, question pack 3: weapons and utility.
    QuestionTemplate(
        id="weapon-kills-v1",
        question="This player has {value} {kill|kills} with the {parameter} {scope}, more than "
        "anyone else here. Who's the group's {parameter} expert?",
        explanation="{name}: {value} {parameter} {kill|kills}.",
        stats=("numbers.kills_with_weapon:<key>", "labels.name:<key>", "labels.queue_id"),
        rule="keyed",
        stat="numbers.kills_with_weapon",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="agent-hunter-v1",
        question="Enemy {parameter} players have fallen to this player {value} {time|times} "
        "{scope}, more than to anyone else here. Who has {parameter}'s number?",
        explanation="{name}: {value} {kill|kills} on enemy {parameter} players.",
        stats=("numbers.kills_vs_agent:<key>", "labels.name:<key>", "labels.queue_id"),
        rule="keyed",
        stat="numbers.kills_vs_agent",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="loadout-rounds-v1",
        question="The {parameter} was in this player's hands in {value} {round|rounds} {scope}, "
        "more than anyone else here. Whose favourite gun is the {parameter}?",
        explanation="{name}: {value} {round|rounds} with the {parameter}.",
        stats=("numbers.rounds_with_loadout:<key>", "labels.name:<key>", "labels.queue_id"),
        rule="keyed",
        stat="numbers.rounds_with_loadout",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="rifle-kill-share-v1",
        question="{percent}% of this player's kills in their last {last_n} matches came from "
        "rifles, the highest share in this group. Who lives on the Vandal and Phantom?",
        explanation="{name}: {numerator} of {denominator} kills with rifles.",
        stats=("numbers.rifle_kills", "kills", "played_at", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.rifle_kills",),
        denominator=("kills",),
        last_n=5,
    ),
    QuestionTemplate(
        id="knife-kills-v1",
        question="Melee only: this player has {value} knife {kill|kills} {scope}, more than "
        "anyone else here. Who brings a knife to a gunfight?",
        explanation="{name}: {value} knife {kill|kills}.",
        stats=("numbers.knife_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.knife_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="smg-kills-v1",
        question="With an SMG in hand, this player has {value} {kill|kills} {scope}, the most in "
        "this group. Who's happiest spraying on the move?",
        explanation="{name}: {value} SMG {kill|kills}.",
        stats=("numbers.smg_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.smg_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="shotgun-kills-v1",
        question="Up close and personal: {value} shotgun {kill|kills} {scope} for this player, "
        "more than anyone else here. Who hides behind corners?",
        explanation="{name}: {value} shotgun {kill|kills}.",
        stats=("numbers.shotgun_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.shotgun_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="machine-gun-kills-v1",
        question="Heavy artillery: this player has {value} machine-gun {kill|kills} {scope}, more "
        "than anyone else here. Who brings out the big guns?",
        explanation="{name}: {value} {kill|kills} with the Ares or Odin.",
        stats=("numbers.machine_gun_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.machine_gun_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="eco-pistol-kills-v1",
        question="Outside the pistol rounds, this player still got {value} sidearm {kill|kills} "
        "{scope}, the most in this group. Who wins fights on an eco?",
        explanation="{name}: {value} sidearm {kill|kills} outside pistol rounds.",
        stats=("numbers.eco_pistol_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.eco_pistol_kills",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="picked-up-gun-kills-v1",
        question="Finders keepers: this player got {value} {kill|kills} with a gun they picked up "
        "{scope}, the most in this group. Who makes the best use of other people's guns?",
        explanation="{name}: {value} {kill|kills} with picked-up guns.",
        stats=("numbers.picked_up_gun_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.picked_up_gun_kills",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="alt-fire-kills-v1",
        question="Right-click specialist: {value} alt-fire {kill|kills} {scope}, the most of "
        "anyone here. Who knows their right mouse button?",
        explanation="{name}: {value} alt-fire {kill|kills}.",
        stats=("numbers.alt_fire_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.alt_fire_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="ability-kills-v1",
        question="Abilities have done the killing for this player {value} {time|times} {scope}, "
        "more than for anyone else here. Whose utility hits hardest?",
        explanation="{name}: {value} ability {kill|kills}.",
        stats=("numbers.ability_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.ability_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="ultimate-kills-v1",
        question="Ultimates have earned this player {value} {kill|kills} {scope}, the most in "
        "this group. Who makes their ult count?",
        explanation="{name}: {value} {kill|kills} with their ultimate.",
        stats=("numbers.ability_kills_ultimate", "labels.queue_id"),
        rule="count",
        stat="numbers.ability_kills_ultimate",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="ultimates-per-match-v1",
        question="This player uses their ultimate {value} {time|times} per match {scope}, the "
        "most in this group. Who's always got ult ready?",
        explanation="{name}: {numerator} ultimates in {denominator} matches.",
        stats=("numbers.ability_casts_ultimate", "labels.queue_id"),
        rule="average",
        stat="numbers.ability_casts_ultimate",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="damage-assists-v1",
        question="This player did the damage before a teammate finished the job {value} "
        "{time|times} in their last {last_n} matches, the most in this group. Who does the dirty "
        "work?",
        explanation="{name}: {value} {assist|assists} after damaging the victim.",
        stats=("numbers.damage_assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.damage_assists",
        last_n=5,
    ),
    QuestionTemplate(
        id="exit-kills-v1",
        question="After the spike had already gone off, this player still got {value} "
        "{kill|kills} {scope}, more than anyone else here. Who keeps fighting after the round is "
        "over?",
        explanation="{name}: {value} {kill|kills} after the explosion.",
        stats=("numbers.exit_kills", "labels.queue_id"),
        rule="count",
        stat="numbers.exit_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # FF-407, question pack 4: spike, economy, damage and timing.
    QuestionTemplate(
        id="fastest-plant-v1",
        question="One of you planted the spike just {value} seconds into a round {scope}, faster "
        "than anyone else here. Who rushed the site?",
        explanation="{name}, on {map_name}: a plant {value} seconds in.",
        stats=("numbers.fastest_plant_ms", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.fastest_plant_ms",
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
        value_scale=0.001,
        skip_missing=True,
    ),
    QuestionTemplate(
        id="closest-defuse-v1",
        question="Someone here finished a defuse with only {value} seconds left on the spike "
        "{scope}, the closest call in this group. Who has such impeccable timing?",
        explanation="{name}, on {map_name}: defused with {value} seconds to spare.",
        stats=("numbers.closest_defuse_ms_left", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.closest_defuse_ms_left",
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
        value_scale=0.001,
        skip_missing=True,
    ),
    QuestionTemplate(
        id="spike-deaths-v1",
        question="This player has been caught in the spike explosion {value} {time|times} "
        "{scope}, more than anyone else here. Who forgot to run?",
        explanation="{name}: {value} spike {death|deaths}.",
        stats=("numbers.spike_deaths", "labels.queue_id"),
        rule="count",
        stat="numbers.spike_deaths",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="fastest-first-blood-v1",
        question="Barely {value} seconds into a round, one of you had already drawn first blood "
        "{scope}, quicker than anyone else here. Who was first through the door?",
        explanation="{name}, playing {agent_name}: first blood after {value} seconds.",
        stats=("numbers.fastest_first_blood_ms", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.fastest_first_blood_ms",
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
        value_scale=0.001,
        skip_missing=True,
    ),
    QuestionTemplate(
        id="average-kill-time-v1",
        question="This player's kills come early: {value} seconds into the round on average in "
        "their last {last_n} matches, the earliest in this group. Who plays fast?",
        explanation="{name}: kills {value} seconds into a round on average.",
        stats=("numbers.avg_kill_time_s", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.avg_kill_time_s",
        direction="lowest",
        last_n=5,
    ),
    QuestionTemplate(
        id="lone-wolf-distance-v1",
        question="When this player gets a kill, their nearest teammate is {value} meters away on "
        "average in their last {last_n} matches, the farthest in this group. Who's the lone wolf?",
        explanation="{name}: {value} meters from the nearest teammate.",
        stats=("numbers.avg_distance_to_team_m", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.avg_distance_to_team_m",
        last_n=5,
    ),
    QuestionTemplate(
        id="credits-spent-v1",
        question="This player spends {value} credits a round on average in their last {last_n} "
        "matches, more than anyone else here. Who's the big spender?",
        explanation="{name}: {value} credits a round.",
        stats=("numbers.credits_spent_average", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.credits_spent_average",
        last_n=5,
    ),
    QuestionTemplate(
        id="unspent-credits-v1",
        question="This player ends the buy with {value} credits still in the bank on average in "
        "their last {last_n} matches, the most in this group. Who's saving for a rainy day?",
        explanation="{name}: {value} credits left over per round.",
        stats=("numbers.avg_unspent_credits", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.avg_unspent_credits",
        last_n=5,
    ),
    QuestionTemplate(
        id="cheapest-loadout-v1",
        question="This player walks into rounds carrying {value} credits of gear on average in "
        "their last {last_n} matches, the least in this group. Who travels light?",
        explanation="{name}: a {value}-credit loadout per round.",
        stats=("numbers.loadout_value_average", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.loadout_value_average",
        direction="lowest",
        last_n=5,
    ),
    QuestionTemplate(
        id="damage-per-credits-v1",
        question="Bang for the buck: this player deals {value} damage for every 1,000 credits "
        "spent in their last {last_n} matches, the best in this group. Who gets the most out of "
        "their money?",
        explanation="{name}: {value} damage per 1,000 credits.",
        stats=("numbers.damage_per_1000_credits", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.damage_per_1000_credits",
        last_n=5,
    ),
    QuestionTemplate(
        id="damage-received-v1",
        question="This player takes {value} damage per round {scope}, more than anyone else here. "
        "Who's the group's damage sponge?",
        explanation="{name}: {numerator} damage taken over {denominator} rounds.",
        stats=("numbers.damage_received", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.damage_received",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="damage-margin-v1",
        question="In a single match {scope}, one of you dealt {value} more damage than they took, "
        "the biggest margin in this group. Who won the damage race?",
        explanation="{name}, playing {agent_name}: {value} more damage dealt than taken.",
        stats=("numbers.damage_delta", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.damage_delta",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
        skip_missing=True,
    ),
    QuestionTemplate(
        id="max-round-combat-score-v1",
        question="A monster round: someone here scored {value} combat score in a single round "
        "{scope}, the best in this group. Whose round was that?",
        explanation="{name}, playing {agent_name}: {value} combat score in one round.",
        stats=("numbers.max_round_combat_score", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_round_combat_score",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    # FF-408, question pack 5: streaks, scoreboard, opponents and records.
    QuestionTemplate(
        id="deathless-streak-v1",
        question="One of you survived {value} rounds in a row without dying in one match {scope}, "
        "the longest streak in this group. Who refused to die?",
        explanation="{name}, on {map_name}: {value} rounds in a row without a death.",
        stats=("numbers.longest_deathless_streak", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.longest_deathless_streak",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
    ),
    QuestionTemplate(
        id="kill-streak-rounds-v1",
        question="A kill in {value} rounds in a row: someone here managed it in one match {scope}, "
        "the longest run in this group. Who was on fire that match?",
        explanation="{name}, playing {agent_name}: a kill in {value} straight rounds.",
        stats=("numbers.longest_kill_streak_rounds", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.longest_kill_streak_rounds",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="match-mvps-v1",
        question="This player was the whole lobby's match MVP {value} {time|times} {scope}, more "
        "than anyone else here. Who's the best player on the server?",
        explanation="{name}: match MVP {value} {time|times}.",
        stats=("numbers.match_mvp", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.match_mvp",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="scoreboard-position-v1",
        question="This player's average spot on the match scoreboard is {value} in their last "
        "{last_n} matches, the closest to the top in this group. Who's rarely far from first "
        "place?",
        explanation="{name}: an average scoreboard position of {value}.",
        stats=("numbers.scoreboard_position", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.scoreboard_position",
        direction="lowest",
        last_n=5,
    ),
    QuestionTemplate(
        id="team-position-v1",
        question="On their own team, this player's average scoreboard spot is {value} in their "
        "last {last_n} matches, the lowest in this group. Who's been quiet on the scoreboard?",
        explanation="{name}: an average team position of {value}.",
        stats=("numbers.team_position", "played_at", "labels.queue_id"),
        rule="average",
        stat="numbers.team_position",
        last_n=5,
    ),
    QuestionTemplate(
        id="giant-killer-v1",
        question="Giant killer: this player took down higher-ranked opponents {value} "
        "{time|times} in their last {last_n} Competitive matches, the most in this group. Who "
        "punches above their weight?",
        explanation="{name}: {value} {kill|kills} on higher-ranked opponents.",
        stats=("numbers.kills_on_higher_ranked", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.kills_on_higher_ranked",
        queues=("competitive",),
        last_n=5,
    ),
    QuestionTemplate(
        id="playground-bully-v1",
        question="This player has taken down lower-ranked opponents {value} {time|times} in their "
        "last {last_n} Competitive matches, the most in this group. Who's the playground bully?",
        explanation="{name}: {value} {kill|kills} on lower-ranked opponents.",
        stats=("numbers.kills_on_lower_ranked", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.kills_on_lower_ranked",
        queues=("competitive",),
        last_n=5,
    ),
    QuestionTemplate(
        id="deaths-to-lower-ranked-v1",
        question="Lower-ranked opponents got the better of this player {value} {time|times} in "
        "their last {last_n} Competitive matches, more than anyone else here. Who's had a few "
        "upsets?",
        explanation="{name}: {value} {death|deaths} to lower-ranked opponents.",
        stats=("numbers.deaths_to_lower_ranked", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.deaths_to_lower_ranked",
        queues=("competitive",),
        last_n=5,
    ),
    QuestionTemplate(
        id="top-ranked-opponent-kills-v1",
        question="The best-ranked opponent in the lobby fell to this player {value} {time|times} "
        "in their last {last_n} Competitive matches, more than anyone else here. Who isn't scared "
        "of the top player?",
        explanation="{name}: {value} {kill|kills} on the lobby's highest-ranked opponent.",
        stats=("numbers.kills_on_top_ranked_opponent", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.kills_on_top_ranked_opponent",
        queues=("competitive",),
        last_n=5,
    ),
    QuestionTemplate(
        id="bottom-ranked-opponent-kills-v1",
        question="This player found the lowest-ranked opponent in the lobby {value} {time|times} "
        "in their last {last_n} Competitive matches, more than anyone else here. Who spots the "
        "weakest link?",
        explanation="{name}: {value} {kill|kills} on the lobby's lowest-ranked opponent.",
        stats=("numbers.kills_on_bottom_ranked_opponent", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.kills_on_bottom_ranked_opponent",
        queues=("competitive",),
        last_n=5,
    ),
    QuestionTemplate(
        id="rank-swing-v1",
        question="Up or down, this player's rank swung by {value} {division|divisions} between "
        "their highest and lowest Competitive match in the last 30 days, the biggest swing in "
        "this group. Whose rank has been on a rollercoaster?",
        explanation="{name}: {value} {division|divisions} between their highest and lowest rank.",
        stats=("numbers.rank_tier", "labels.queue_id"),
        rule="range",
        stat="numbers.rank_tier",
        queues=("competitive",),
        last_n=None,
    ),
    QuestionTemplate(
        id="kill-swing-v1",
        question="Between their best and worst match {scope}, this player's kill count swung by "
        "{value}, the widest gap in this group. Who's the most unpredictable?",
        explanation="{name}: {value} {kill|kills} between their best and worst match.",
        stats=("kills", "labels.queue_id"),
        rule="range",
        stat="kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="performance-swing-v1",
        question="Over their last {last_n} matches, this player's Performance Score swung by "
        "{value} between their best and worst game, more than anyone else here. Who blows hot and "
        "cold?",
        explanation="{name}: a {value}-point gap between their best and worst match.",
        stats=("numbers.performance_score", "played_at", "labels.queue_id"),
        rule="range",
        stat="numbers.performance_score",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-deaths-in-a-match-v1",
        question="This player once died {value} {time|times} in a single match {scope}, more than "
        "anyone else here. Who had the roughest game?",
        explanation="{name}, playing {agent_name}: {value} {death|deaths} in one match.",
        stats=("deaths", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="deaths",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="most-assists-in-a-match-v1",
        question="{value} {assist|assists} in a single match {scope}: nobody here has helped out "
        "more. Who was everyone's wingman that day?",
        explanation="{name}, playing {agent_name}: {value} {assist|assists} in one match.",
        stats=("assists", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="assists",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="most-damage-in-a-match-v1",
        question="Someone here dealt {value} damage in a single match {scope}, the most anyone "
        "here has managed. Who went on a rampage?",
        explanation="{name}, playing {agent_name}: {value} damage in one match.",
        stats=("numbers.damage_dealt", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.damage_dealt",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="best-performance-match-v1",
        question="Their best game {scope} earned this player a Performance Score of {value}, the "
        "highest in this group. Whose game was that?",
        explanation="{name}, on {map_name}: a Performance Score of {value}.",
        stats=("numbers.performance_score", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.performance_score",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
    ),
    QuestionTemplate(
        id="most-first-bloods-in-a-match-v1",
        question="In one match {scope}, this player drew first blood {value} {time|times}, more "
        "than anyone else here has. Who kept opening rounds that day?",
        explanation="{name}, playing {agent_name}: {value} first {blood|bloods} in one match.",
        stats=("numbers.first_bloods", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.first_bloods",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
    QuestionTemplate(
        id="most-plants-in-a-match-v1",
        question="Spike duty: someone here planted it {value} {time|times} in a single match "
        "{scope}, the most in this group. Who carried the spike that day?",
        explanation="{name}, on {map_name}: {value} {plant|plants} in one match.",
        stats=("numbers.plants", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.plants",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
    ),
    # FF-411, question pack 8: team results and friends. Teammates share team results, so these are
    # solo only.
    QuestionTemplate(
        id="solo-flawless-rounds-v1",
        question="Playing without the rest of you, this player's team won {value} {round|rounds} "
        "without losing anyone, the most in this group. Whose solo teams are the most ruthless?",
        explanation="{name}: {value} flawless {round|rounds} in solo matches.",
        stats=("numbers.flawless_rounds", "labels.queue_id"),
        rule="count",
        stat="numbers.flawless_rounds",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-team-ace-rounds-v1",
        question="Without the rest of you, this player's team had all five players get a kill in "
        "the same round {value} {time|times}, the most in this group. Whose solo teams work "
        "together best?",
        explanation="{name}: {value} team-ace {round|rounds}.",
        stats=("numbers.team_ace_rounds", "labels.queue_id"),
        rule="count",
        stat="numbers.team_ace_rounds",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-eco-rounds-won-v1",
        question="In their solo games, this player's team won {value} eco {round|rounds} against a "
        "full buy, more than anyone else here. Whose solo teams pull off the most upsets?",
        explanation="{name}: {value} eco {round|rounds} won.",
        stats=("numbers.eco_rounds_won", "labels.queue_id"),
        rule="count",
        stat="numbers.eco_rounds_won",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-pistol-rounds-v1",
        question="Away from the group, this player's team wins {value} pistol {round|rounds} per "
        "match, the most of anyone here who plays solo. Who's lucky with pistols?",
        explanation="{name}: {numerator} pistol rounds won in {denominator} solo matches.",
        stats=("numbers.pistol_rounds_won", "labels.queue_id"),
        rule="average",
        stat="numbers.pistol_rounds_won",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-round-win-rate-v1",
        question="Playing solo, this player's team wins {percent}% of its rounds, the best of "
        "anyone here who plays solo. Who carries random teammates?",
        explanation="{name}: {numerator} of {denominator} rounds won in solo matches.",
        stats=("numbers.rounds_won", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.rounds_won",),
        denominator=("numbers.rounds_played",),
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-overtime-v1",
        question="Overtime again: {value} of this player's solo matches went past round 24, "
        "more than anyone else here. Who can't finish a match in regulation?",
        explanation="{name}: {value} overtime {match|matches}.",
        stats=("numbers.overtime", "labels.queue_id"),
        rule="count",
        stat="numbers.overtime",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-biggest-deficit-v1",
        question="In a solo match, this player's team once fell {value} {round|rounds} behind, the "
        "biggest hole anyone here has been in. Who's been buried the deepest?",
        explanation="{name}, on {map_name}: down {value} {round|rounds} at one point.",
        stats=("numbers.max_deficit", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_deficit",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
        display_label="map",
    ),
    QuestionTemplate(
        id="assists-for-friend-v1",
        question="Someone here has assisted {friend}'s kills {value} {time|times} {scope}, more "
        "than anyone else on {friend}'s team. Who sets up {friend}?",
        explanation="{name}: {value} {assist|assists} on {friend}'s kills.",
        stats=("numbers.assists_for:<user_id>", "labels.team_id", "labels.queue_id"),
        rule="pair",
        stat="numbers.assists_for",
        last_n=None,
    ),
    QuestionTemplate(
        id="finishes-for-friend-v1",
        question="{friend} did the damage, but this player finished off {value} of those "
        "{opponent|opponents} {scope}, more than anyone else on {friend}'s team. Who steals "
        "{friend}'s kills?",
        explanation="{name}: {value} {kill|kills} on enemies {friend} hit first.",
        stats=("numbers.finishes_for:<user_id>", "labels.team_id", "labels.queue_id"),
        rule="pair",
        stat="numbers.finishes_for",
        last_n=None,
    ),
    QuestionTemplate(
        id="solo-attack-round-win-rate-v1",
        question="Solo on attack, this player's team wins {percent}% of its rounds, the best of "
        "anyone here who plays solo. Whose solo teams break sites?",
        explanation="{name}: {numerator} of {denominator} attack rounds won solo.",
        stats=("numbers.rounds_won_attack", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.rounds_won_attack",),
        denominator=("numbers.rounds_played_attack",),
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-defense-round-win-rate-v1",
        question="Solo on defense, this player's team holds {percent}% of its rounds, the best of "
        "anyone here who plays solo. Whose solo teams lock sites down?",
        explanation="{name}: {numerator} of {denominator} defense rounds won solo.",
        stats=("numbers.rounds_won_defense", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.rounds_won_defense",),
        denominator=("numbers.rounds_played_defense",),
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-first-half-round-win-rate-v1",
        question="First half, solo: this player's team wins {percent}% of those rounds, the best "
        "of anyone here who plays solo. Whose solo teams start fast?",
        explanation="{name}: {numerator} of {denominator} first-half rounds won solo.",
        stats=(
            "numbers.rounds_won_first_half",
            "numbers.rounds_played_first_half",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.rounds_won_first_half",),
        denominator=("numbers.rounds_played_first_half",),
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    QuestionTemplate(
        id="solo-second-half-round-win-rate-v1",
        question="Second half, solo: this player's team takes {percent}% of those rounds, the "
        "best of anyone here who plays solo. Whose solo teams finish the job?",
        explanation="{name}: {numerator} of {denominator} second-half rounds won solo.",
        stats=(
            "numbers.rounds_won_second_half",
            "numbers.rounds_played_second_half",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.rounds_won_second_half",),
        denominator=("numbers.rounds_played_second_half",),
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    # Lowest versions of existing questions.
    QuestionTemplate(
        id="lowest-kd-v1",
        question="Only {value} kills for every death {scope}: the lowest ratio in this group. "
        "Who's been in a rough patch?",
        explanation="{name}: {numerator} kills and {denominator} deaths.",
        stats=("kills", "deaths", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("deaths",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="lowest-kast-v1",
        question="This player killed, assisted, survived or got traded in just {percent}% of "
        "their rounds {scope}, the lowest in this group. Who needs to get more involved?",
        explanation="{name}: an impact in {numerator} of {denominator} rounds.",
        stats=("numbers.kast_rounds", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.kast_rounds",),
        denominator=("numbers.rounds_played",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="lowest-headshot-percentage-v1",
        question="Only {percent}% of this player's hits landed on the head in their last {last_n} "
        "matches, the lowest in this group. Whose crosshair needs a little lift?",
        explanation="{name}: {numerator} headshots out of {denominator} hits.",
        stats=(
            "numbers.headshots",
            "numbers.bodyshots",
            "numbers.legshots",
            "played_at",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.headshots",),
        denominator=("numbers.headshots", "numbers.bodyshots", "numbers.legshots"),
        direction="lowest",
        last_n=5,
    ),
    QuestionTemplate(
        id="lowest-acs-v1",
        question="This player earns just {value} combat score per round {scope}, the lowest in "
        "this group. Who's had a quiet stretch?",
        explanation="{name}: {numerator} combat score over {denominator} rounds.",
        stats=("numbers.combat_score", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.combat_score",),
        denominator=("numbers.rounds_played",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="lowest-performance-score-v1",
        question="This player's Performance Score averages {value} per match {scope}, the lowest "
        "in this group. Who's due a big game?",
        explanation="{name}, averaging {value} over {denominator} matches.",
        stats=("numbers.performance_score", "labels.queue_id"),
        rule="average",
        stat="numbers.performance_score",
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="fewest-recent-deaths-v1",
        question="Only {value} {death|deaths} in their last {last_n} matches: nobody here has "
        "died less. Who's the hardest to pin down?",
        explanation="{name}: {value} {death|deaths} in {last_n} matches.",
        stats=("deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="deaths",
        direction="lowest",
        last_n=5,
    ),
    QuestionTemplate(
        id="fewest-first-deaths-v1",
        question="In their last {last_n} matches, this player was the first one down only {value} "
        "{time|times}, the fewest in this group. Who never dies first?",
        explanation="{name}: first to die {value} {time|times} in {last_n} matches.",
        stats=("numbers.first_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.first_deaths",
        direction="lowest",
        last_n=5,
    ),
    QuestionTemplate(
        id="lowest-damage-received-v1",
        question="Enemies barely touch this player: just {value} damage taken per round {scope}, "
        "the least in this group. Who's the hardest to hit?",
        explanation="{name}: {numerator} damage taken over {denominator} rounds.",
        stats=("numbers.damage_received", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.damage_received",),
        denominator=("numbers.rounds_played",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="lowest-opening-duel-win-rate-v1",
        question="This player wins only {percent}% of their opening duels {scope}, the lowest of "
        "anyone here with 10 or more. Who finds the first fight hardest?",
        explanation="{name}: {numerator} first bloods out of {denominator} opening duels.",
        stats=("numbers.first_bloods", "numbers.first_deaths", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.first_bloods",),
        denominator=("numbers.first_bloods", "numbers.first_deaths"),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
        contenders_only=True,
        min_denominator=10,
    ),
    QuestionTemplate(
        id="lowest-clutch-win-rate-v1",
        question="Last one standing, this player wins only {percent}% of their clutches {scope}. "
        "Nobody here with 5 or more attempts does worse. Who wishes for backup in a clutch?",
        explanation="{name}: {numerator} clutch wins out of {denominator} attempts.",
        stats=("numbers.clutch_wins", "numbers.clutch_attempts", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.clutch_wins",),
        denominator=("numbers.clutch_attempts",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
        contenders_only=True,
        min_denominator=5,
    ),
    QuestionTemplate(
        id="lowest-map-win-rate-v1",
        question="On {parameter}, this player wins only {percent}% of their matches, the worst "
        "record of anyone here who has played it. Who should avoid {parameter}?",
        explanation="{name} won {numerator} of {denominator} matches on {parameter}.",
        stats=("won", "matches", "labels.result", "labels.map", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("won",),
        denominator=("matches",),
        direction="lowest",
        last_n=None,
        parameter_label="map",
        contenders_only=True,
    ),
    # Attack and defense, per round of that side.
    QuestionTemplate(
        id="attack-kills-per-round-v1",
        question="On attack, this player gets {value} kills per round {scope}, the most in this "
        "group. Who leads the charge?",
        explanation="{name}: {numerator} kills in {denominator} attack rounds.",
        stats=("numbers.attack_kills", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_kills",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-kills-per-round-v1",
        question="Holding a site, this player gets {value} kills per round {scope}, the most in "
        "this group. Who's the wall on defense?",
        explanation="{name}: {numerator} kills in {denominator} defense rounds.",
        stats=("numbers.defense_kills", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_kills",),
        denominator=("numbers.rounds_played_defense",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="attack-survival-v1",
        question="This player survives attack rounds better than anyone here: they die in only "
        "{percent}% of them {scope}. Who makes it out alive?",
        explanation="{name}: {numerator} deaths in {denominator} attack rounds.",
        stats=("numbers.attack_deaths", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_deaths",),
        denominator=("numbers.rounds_played_attack",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-survival-v1",
        question="On defense, this player dies in only {percent}% of rounds {scope}, the fewest "
        "in this group. Who's impossible to dig out?",
        explanation="{name}: {numerator} deaths in {denominator} defense rounds.",
        stats=("numbers.defense_deaths", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_deaths",),
        denominator=("numbers.rounds_played_defense",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="attack-adr-v1",
        question="Pushing onto a site, this player deals {value} damage per round {scope}, the "
        "most in this group. Who hits hardest on attack?",
        explanation="{name}: {numerator} damage in {denominator} attack rounds.",
        stats=("numbers.attack_damage_dealt", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_damage_dealt",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-adr-v1",
        question="This player deals {value} damage per round on defense {scope}, the most in this "
        "group. Who punishes every push?",
        explanation="{name}: {numerator} damage in {denominator} defense rounds.",
        stats=("numbers.defense_damage_dealt", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_damage_dealt",),
        denominator=("numbers.rounds_played_defense",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="attack-kast-v1",
        question="Attack after attack, this player makes an impact in {percent}% of rounds "
        "{scope}, the highest in this group. Who never wastes an attack round?",
        explanation="{name}: an impact in {numerator} of {denominator} attack rounds.",
        stats=("numbers.attack_kast_rounds", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_kast_rounds",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-kast-v1",
        question="Defending, this player has an impact in {percent}% of rounds {scope}, more than "
        "anyone else here. Who anchors the defense?",
        explanation="{name}: an impact in {numerator} of {denominator} defense rounds.",
        stats=("numbers.defense_kast_rounds", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_kast_rounds",),
        denominator=("numbers.rounds_played_defense",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="attack-first-bloods-v1",
        question="This player opens {percent}% of attack rounds with the first kill {scope}, the "
        "most in this group. Who's the entry fragger?",
        explanation="{name}: first blood in {numerator} of {denominator} attack rounds.",
        stats=("numbers.attack_first_bloods", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_first_bloods",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-first-bloods-v1",
        question="Holding the line, this player gets the round's first kill in {percent}% of "
        "defense rounds {scope}, more than anyone else here. Who catches the rush first?",
        explanation="{name}: first blood in {numerator} of {denominator} defense rounds.",
        stats=("numbers.defense_first_bloods", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_first_bloods",),
        denominator=("numbers.rounds_played_defense",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="attack-first-deaths-v1",
        question="This player is the first to fall in {percent}% of attack rounds {scope}, more "
        "than anyone else here. Who dry-peeks on attack?",
        explanation="{name}: first death in {numerator} of {denominator} attack rounds.",
        stats=("numbers.attack_first_deaths", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_first_deaths",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-first-deaths-v1",
        question="Too eager on defense: this player goes down first in {percent}% of defense "
        "rounds {scope}, the most in this group. Who keeps peeking too early?",
        explanation="{name}: first death in {numerator} of {denominator} defense rounds.",
        stats=("numbers.defense_first_deaths", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_first_deaths",),
        denominator=("numbers.rounds_played_defense",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="attack-assists-per-round-v1",
        question="When attacking, this player picks up {value} assists per round {scope}, the "
        "most in this group. Who sets up every execute?",
        explanation="{name}: {numerator} assists in {denominator} attack rounds.",
        stats=("numbers.attack_assists", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.attack_assists",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="defense-assists-per-round-v1",
        question="This player chips in {value} assists per round on defense {scope}, more than "
        "anyone else here. Who helps hold every site?",
        explanation="{name}: {numerator} assists in {denominator} defense rounds.",
        stats=("numbers.defense_assists", "numbers.rounds_played_defense", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.defense_assists",),
        denominator=("numbers.rounds_played_defense",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="plants-per-attack-round-v1",
        question="Whatever the site, this player plants the spike in {percent}% of their attack "
        "rounds {scope}, more than anyone else here. Who gets the spike down?",
        explanation="{name}: {numerator} plants in {denominator} attack rounds.",
        stats=("numbers.plants", "numbers.rounds_played_attack", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.plants",),
        denominator=("numbers.rounds_played_attack",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # First and second half, per round of that half (overtime is in neither).
    QuestionTemplate(
        id="first-half-kills-v1",
        question="In the first half of a match, this player gets {value} kills per round {scope}, "
        "the most in this group. Who starts strong?",
        explanation="{name}: {numerator} kills in {denominator} first-half rounds.",
        stats=("numbers.first_half_kills", "numbers.rounds_played_first_half", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.first_half_kills",),
        denominator=("numbers.rounds_played_first_half",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="second-half-kills-v1",
        question="After the side swap, this player gets {value} kills per round {scope}, the most "
        "in this group. Who finishes strong?",
        explanation="{name}: {numerator} kills in {denominator} second-half rounds.",
        stats=("numbers.second_half_kills", "numbers.rounds_played_second_half", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.second_half_kills",),
        denominator=("numbers.rounds_played_second_half",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="first-half-survival-v1",
        question="This player dies in only {percent}% of first-half rounds {scope}, the fewest in "
        "this group. Who's careful early on?",
        explanation="{name}: {numerator} deaths in {denominator} first-half rounds.",
        stats=("numbers.first_half_deaths", "numbers.rounds_played_first_half", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.first_half_deaths",),
        denominator=("numbers.rounds_played_first_half",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="second-half-survival-v1",
        question="In the second half, this player dies in only {percent}% of rounds {scope}, the "
        "fewest in this group. Who tightens up after the swap?",
        explanation="{name}: {numerator} deaths in {denominator} second-half rounds.",
        stats=(
            "numbers.second_half_deaths",
            "numbers.rounds_played_second_half",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.second_half_deaths",),
        denominator=("numbers.rounds_played_second_half",),
        direction="lowest",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="first-half-kast-v1",
        question="This player makes an impact in {percent}% of first-half rounds {scope}, the "
        "highest in this group. Who's switched on from round one?",
        explanation="{name}: an impact in {numerator} of {denominator} first-half rounds.",
        stats=(
            "numbers.first_half_kast_rounds",
            "numbers.rounds_played_first_half",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.first_half_kast_rounds",),
        denominator=("numbers.rounds_played_first_half",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="second-half-kast-v1",
        question="Second half, this player has an impact in {percent}% of rounds {scope}, more "
        "than anyone else here. Who gets better as the match goes on?",
        explanation="{name}: an impact in {numerator} of {denominator} second-half rounds.",
        stats=(
            "numbers.second_half_kast_rounds",
            "numbers.rounds_played_second_half",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.second_half_kast_rounds",),
        denominator=("numbers.rounds_played_second_half",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="first-half-assists-v1",
        question="Early in a match, this player sets up {value} assists per round {scope}, the "
        "most in this group. Who supports from the start?",
        explanation="{name}: {numerator} assists in {denominator} first-half rounds.",
        stats=("numbers.first_half_assists", "numbers.rounds_played_first_half", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.first_half_assists",),
        denominator=("numbers.rounds_played_first_half",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="second-half-assists-v1",
        question="This player picks up {value} assists per round in the second half {scope}, the "
        "most in this group. Who helps close matches out?",
        explanation="{name}: {numerator} assists in {denominator} second-half rounds.",
        stats=(
            "numbers.second_half_assists",
            "numbers.rounds_played_second_half",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=("numbers.second_half_assists",),
        denominator=("numbers.rounds_played_second_half",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # Topped the scoreboard: how often a player had the lobby's or their team's highest value.
    QuestionTemplate(
        id="topped-match-kills-v1",
        question="This player had the most kills in the whole lobby {value} {time|times} {scope}, "
        "more than anyone else here. Who tops the frag chart?",
        explanation="{name}: top fragger in {value} {match|matches}.",
        stats=("numbers.topped_match_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_kills",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-deaths-v1",
        question="Most deaths in the whole lobby: that was this player {value} {time|times} "
        "{scope}, more often than anyone else here. Who keeps topping the wrong list?",
        explanation="{name}: most deaths in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_deaths",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-assists-v1",
        question="This player led the whole lobby in assists {value} {time|times} {scope}, more "
        "than anyone else here. Who's the server's best teammate?",
        explanation="{name}: most assists in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_assists",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-headshots-v1",
        question="Most headshots in the lobby: this player managed it {value} {time|times} "
        "{scope}, more than anyone else here. Who out-aims the whole server?",
        explanation="{name}: most headshots in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_headshots", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_headshots",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-legshots-v1",
        question="This player hit more legs than anyone else in the lobby {value} {time|times} "
        "{scope}, the most in this group. Who aims at the ankles?",
        explanation="{name}: most leg shots in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_legshots", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_legshots",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-damage-v1",
        question="This player dealt the most damage in the lobby {value} {time|times} {scope}, "
        "more than anyone else here. Who leaves everyone low?",
        explanation="{name}: most damage in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_damage_dealt", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_damage_dealt",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-damage-received-v1",
        question="This player took the most damage in the lobby {value} {time|times} {scope}, more "
        "often than anyone else here. Who's the human shield?",
        explanation="{name}: most damage taken in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_damage_received", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_damage_received",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-combat-score-v1",
        question="This player topped the lobby in combat score {value} {time|times} {scope}, more "
        "than anyone else here. Who tops the scoreboard?",
        explanation="{name}: top combat score in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_combat_score", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_combat_score",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-first-bloods-v1",
        question="This player opened more rounds than anyone in the lobby {value} {time|times} "
        "{scope}, the most here. Who starts every fight?",
        explanation="{name}: most first bloods in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_first_bloods", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_first_bloods",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-first-deaths-v1",
        question="First to fall, again: this player led the lobby in first deaths {value} "
        "{time|times} {scope}, more often than anyone else here. Who's always first to go?",
        explanation="{name}: most first deaths in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_first_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_first_deaths",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-plants-v1",
        question="This player planted more than anyone else in the lobby {value} {time|times} "
        "{scope}, the most here. Who carries the spike?",
        explanation="{name}: most plants in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_plants", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_plants",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-defuses-v1",
        question="This player out-defused the whole lobby {value} {time|times} {scope}, more than "
        "anyone else here. Who's on defuse duty?",
        explanation="{name}: most defuses in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_defuses", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_defuses",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-headshot-percentage-v1",
        question="Cleanest aim in the lobby: this player had the best headshot rate {value} "
        "{time|times} {scope}, more than anyone else here. Whose aim is the cleanest?",
        explanation="{name}: best headshot rate in the lobby {value} {time|times}.",
        stats=("numbers.topped_match_headshot_percentage", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_headshot_percentage",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-max-round-damage-v1",
        question="The biggest single round of damage in the lobby belonged to this player {value} "
        "{time|times} {scope}, the most here. Who has the biggest rounds?",
        explanation="{name}: the lobby's biggest damage round {value} {time|times}.",
        stats=("numbers.topped_match_max_round_damage", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_max_round_damage",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-match-max-round-kills-v1",
        question="Best kill round in the lobby: this player had it {value} {time|times} {scope}, "
        "more than anyone else here. Who has the biggest multi-kills?",
        explanation="{name}: the lobby's best kill round {value} {time|times}.",
        stats=("numbers.topped_match_max_round_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_match_max_round_kills",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-kills-v1",
        question="On their own team, this player had the most kills {value} {time|times} {scope}, "
        "more than anyone else here. Who's the team's top fragger?",
        explanation="{name}: their team's top fragger {value} {time|times}.",
        stats=("numbers.topped_team_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_kills",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-deaths-v1",
        question="This player died more than any of their teammates {value} {time|times} {scope}, "
        "the most here. Who's had the toughest time on their team?",
        explanation="{name}: most deaths on their team {value} {time|times}.",
        stats=("numbers.topped_team_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_deaths",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-assists-v1",
        question="Their team's assist leader {value} {time|times} {scope}: nobody here has done "
        "it more. Who makes their teammates look good?",
        explanation="{name}: most assists on their team {value} {time|times}.",
        stats=("numbers.topped_team_assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_assists",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-first-bloods-v1",
        question="This player got the most first bloods on their team {value} {time|times} "
        "{scope}, more than anyone else here. Who's the team's opener?",
        explanation="{name}: most first bloods on their team {value} {time|times}.",
        stats=("numbers.topped_team_first_bloods", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_first_bloods",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-first-deaths-v1",
        question="This player was their team's most frequent first death {value} {time|times} "
        "{scope}, the most here. Who's always first in for the team?",
        explanation="{name}: most first deaths on their team {value} {time|times}.",
        stats=("numbers.topped_team_first_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_first_deaths",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-plants-v1",
        question="Spike carrier: this player planted more than any teammate {value} {time|times} "
        "{scope}, the most here. Who always ends up with the spike?",
        explanation="{name}: most plants on their team {value} {time|times}.",
        stats=("numbers.topped_team_plants", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_plants",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-defuses-v1",
        question="This player defused more than any teammate {value} {time|times} {scope}, more "
        "than anyone else here. Who sticks the defuse for the team?",
        explanation="{name}: most defuses on their team {value} {time|times}.",
        stats=("numbers.topped_team_defuses", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_defuses",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
    QuestionTemplate(
        id="topped-team-damage-received-v1",
        question="This player soaked up more damage than any teammate {value} {time|times} "
        "{scope}, the most here. Who takes the hits for the team?",
        explanation="{name}: most damage taken on their team {value} {time|times}.",
        stats=("numbers.topped_team_damage_received", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.topped_team_damage_received",
        scopes=ALL_SCOPES,
        last_n=None,
        windows=(5, 10, None),
    ),
)
