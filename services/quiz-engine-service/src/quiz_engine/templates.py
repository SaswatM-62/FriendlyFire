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
   stored match when last_n is None. The window is chosen before any filter.
3. Filter: with a parameter_label, one question per value of that label (e.g. per
   agent), keeping only the window's matches with that value. A match missing the
   label skips the question rather than silently dropping out.
4. Value, by rule:
   - max_in_match: the player's best single match for stat (highest, or lowest
     when direction is "lowest"); display_label names a label from that match,
     shown as {<display_label>_name}.
   - count: the sum of stat over the matches ("matches" counts them, "won" counts
     wins). No matches after a filter is a known zero.
   - average: stat averaged per match.
   - pooled_ratio: the sum of the numerator stats over the sum of the denominator
     stats. A zero total denominator is unknown; one zero match still counts.
   - pair: friend-pair counts. One question per room player (the {friend}); each
     other player's value is the sum of the "<stat>:<friend's user ID>" number over
     matches where they were on the friend's team.
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
"matches", or "numbers.<key>" from the catalogue.

Placeholders: {name} (winner), {value}, {numerator}, {denominator}, {percent}
(value x 100), {scope}, {last_n}, {parameter}, {friend} and {<display_label>_name}.
Whole numbers show as they are; others round half up to 2 decimals (1.28), or 1
for {percent} (29.7). {singular|plural}, e.g. "{value} {time|times}", shows the first
form when {value} shows as 1 and the second otherwise.
"""

from dataclasses import dataclass
from typing import Literal

Scope = Literal["all", "shared", "solo"]
Rule = Literal["max_in_match", "count", "average", "pooled_ratio", "pair"]
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
        question="This player deals {value} damage per round {scope}, the lowest in this "
        "group. Who would rather forget this one?",
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
        question="Over their last {last_n} matches, this player died {value} {time|times}, "
        "more than anyone else here. Who's the culprit?",
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
        question="Behind a scope, this player has taken down {value} {enemy|enemies} {scope}, more "
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
        explanation="{name}: {numerator} combat score over {denominator} rounds.",
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
        explanation="{name}, with {value} wins in {last_n} Competitive matches "
        "(a draw isn't a win).",
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
        question="This player averages a Performance Score of {value} per match {scope}, "
        "the highest in this group. Who set the pace?",
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
        question="This player averages {value} pistol-round {kill|kills} per match {scope}, "
        "the most in this group. Who starts rounds with a bang?",
        explanation="{name}: {numerator} pistol-round kills in {denominator} matches.",
        stats=("numbers.pistol_round_kills", "labels.queue_id"),
        rule="average",
        stat="numbers.pistol_round_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
)
