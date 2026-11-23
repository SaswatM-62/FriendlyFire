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
   one match in the scope (and at least last_n of them), plus one after the filter.
   Use it for agent or map questions, and for the solo scope: players who mostly
   squad up still have the odd solo game, so pair a solo template with
   contenders_only and a small or no last_n. Pair questions always compare only the
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
for {percent} (29.7).
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


TEMPLATES = (
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
        question="Across their last {last_n} matches, this player piled up {value} kills, "
        "more than anyone else here. Who's been on a tear lately?",
        explanation="{name}: {value} kills in {last_n} matches.",
        stats=("kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="kills",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-assists-v1",
        question="Nobody here has helped out more lately: this player picked up {value} "
        "assists in their last {last_n} matches. Who is the ultimate teammate?",
        explanation="{name}: {value} assists in {last_n} matches.",
        stats=("assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="assists",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-deaths-v1",
        question="Over their last {last_n} matches, this player died {value} times, more than "
        "anyone else here. Who's the culprit?",
        explanation="{name}: {value} deaths in {last_n} matches.",
        stats=("deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-plants-v1",
        question="Someone here planted the spike {value} times in their last {last_n} matches, "
        "the most in this group. Who keeps the spike moving?",
        explanation="{name}: {value} plants in {last_n} matches.",
        stats=("numbers.plants", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.plants",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-defuses-v1",
        question="This player has defused the spike {value} times {scope}, more than anyone "
        "else here. Who does the team trust with the defuse?",
        explanation="{name}: {value} defuses.",
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
        "{value} times in their last {last_n} matches, more than anyone else here. Who needs "
        "to stop peeking first?",
        explanation="{name} was the first player killed in {value} rounds over {last_n} matches.",
        stats=("numbers.first_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.first_deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-isolated-deaths-v1",
        question="With no teammate within 20 meters, this player went down alone {value} times "
        "in their last {last_n} matches, the most in this group. Who keeps wandering off "
        "by themselves?",
        explanation="{name}: {value} deaths with no teammate nearby in {last_n} matches.",
        stats=("numbers.isolated_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.isolated_deaths",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-recent-deathless-rounds-v1",
        question="This player made it through {value} rounds without dying in their last "
        "{last_n} matches, the most in this group. Who has the best survival instincts?",
        explanation="{name} survived {value} rounds in {last_n} matches.",
        stats=("numbers.deathless_rounds", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.deathless_rounds",
        last_n=5,
    ),
    QuestionTemplate(
        id="most-sniper-kills-v1",
        question="Behind a scope, this player has taken down {value} enemies {scope}, more "
        "than anyone else here. Who's the group's sniper?",
        explanation="{name}: {value} kills with sniper rifles.",
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
)
