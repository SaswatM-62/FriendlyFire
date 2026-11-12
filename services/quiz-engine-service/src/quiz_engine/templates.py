"""Value-first question templates; evaluator integration awaits FF-402."""

from dataclasses import dataclass
from typing import Literal

# TODO(ff): the question and explanation templates
#   - e.g. "Who had {kills} kills on {map}?" and "{name} went {kills}/{deaths}/{assists}
#     as {agent}."
#   - only use labels/numbers that every game provides, or check the game first
#   - keep it data (a list of templates), so adding a question type is one entry

Rule = Literal["pooled_ratio", "max_in_match", "count"]


@dataclass(frozen=True)
class QuestionTemplate:
    """One entry per question, with reusable calculation and comparison metadata.

    Select last_n games in queues before applying parameter_label filters.
    Pooled ratios sum raw numerators and denominators across that window; a zero
    total denominator skips the comparison (including zero total deaths for K/D).
    Individual deathless games still contribute if the pooled denominator is positive.
    The synthetic `matches` stat counts games; `won` requires a known result label
    consistent with the scalar. Unknown inputs or tied leaders skip the question.
    Rank displays use display_label from the match attaining the maximum stat.
    These are evaluator requirements, not behavior implemented by this module.
    """

    id: str
    question: str
    explanation: str
    stats: tuple[str, ...]
    rule: Rule
    stat: str = ""
    numerator: tuple[str, ...] = ()
    denominator: tuple[str, ...] = ()
    direction: Literal["highest", "lowest"] = "highest"
    scopes: tuple[Literal["all"], ...] = ("all",)
    game: str = "valorant"
    queues: tuple[str, ...] = ("competitive", "unrated")
    last_n: int = 10
    parameter_label: Literal["", "agent", "map"] = ""
    display_label: str = ""
    zero_denominator: Literal["skip"] = "skip"


TEMPLATES: tuple[QuestionTemplate, ...] = (
    QuestionTemplate(
        id="highest-recent-rank-v1",
        question="This player hit {rank_name} in their last {last_n} Competitive games, "
        "the highest rank in the group. Who is it?",
        explanation="{name} reached {rank_name}, the highest rank recorded in their last "
        "{last_n} Competitive games. This is a match-time rank, "
        "not their current or lifetime peak.",
        stats=("numbers.rank_tier", "labels.rank", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.rank_tier",
        display_label="rank",
        queues=("competitive",),
    ),
    QuestionTemplate(
        id="highest-recent-kd-v1",
        question="This player got {value} kills per death over their last {last_n} games, "
        "the best in the group. Who is it?",
        explanation="{name} had {numerator} kills / {denominator} deaths: {value} kills per death. "
        "Totals are combined across their last {last_n} games; "
        "zero total deaths skips the question.",
        stats=("kills", "deaths"),
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("deaths",),
    ),
    QuestionTemplate(
        id="highest-recent-headshot-percentage-v1",
        question="This player hit headshots on {percent}% of their hits in their last "
        "{last_n} games, the best in the group. Who is it?",
        explanation="{name} landed {numerator} head hits / {denominator} total hits ({percent}%). "
        "These are hits, not kills; misses are not included. Totals are combined across games.",
        stats=("numbers.headshots", "numbers.bodyshots", "numbers.legshots"),
        rule="pooled_ratio",
        numerator=("numbers.headshots",),
        denominator=("numbers.headshots", "numbers.bodyshots", "numbers.legshots"),
    ),
    QuestionTemplate(
        id="most-recent-headshots-v1",
        question="This player landed {value} headshots in a single game, out of their last "
        "{last_n}, the most in the group. Who is it?",
        explanation="{name} recorded {value} head hits in one of their last {last_n} games. "
        "Headshots count hits, not headshot kills.",
        stats=("numbers.headshots",),
        rule="max_in_match",
        stat="numbers.headshots",
    ),
    QuestionTemplate(
        id="most-recent-agent-games-v1",
        question="This player picked {parameter} in {value} of their last {last_n} games, "
        "more than anyone else. Who is it?",
        explanation="{name} played {parameter} in {value} of their last {last_n} games. "
        "Every game must have a known agent; a known zero uses counts as zero.",
        stats=("labels.agent",),
        rule="count",
        stat="matches",
        parameter_label="agent",
    ),
    QuestionTemplate(
        id="highest-recent-map-win-rate-v1",
        question="This player won {percent}% of their games on {parameter} among their last "
        "{last_n} games, the best in the group. Who is it?",
        explanation="{name} won {numerator} of {denominator} matches on {parameter} "
        "({percent}%). Draws count as losses for this rate. Select the last {last_n} games "
        "before filtering by map; no games on the map skips the question.",
        stats=("labels.map", "labels.result", "won"),
        rule="pooled_ratio",
        numerator=("won",),
        denominator=("matches",),
        parameter_label="map",
    ),
    QuestionTemplate(
        id="highest-recent-agent-kd-v1",
        question="This player got {value} kills per death as {parameter} across their last "
        "{last_n} games, the best in the group. Who is it?",
        explanation="{name} had {numerator} kills / {denominator} deaths as {parameter}: "
        "{value} kills per death. Select the last {last_n} games before filtering by agent; "
        "no games on the agent or zero total deaths skips the question.",
        stats=("labels.agent", "kills", "deaths"),
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("deaths",),
        parameter_label="agent",
    ),
    QuestionTemplate(
        id="lowest-recent-adr-v1",
        question="This player dealt {value} damage per round over their last {last_n} games, "
        "the lowest in the group. Who is it?",
        explanation="{name} dealt {numerator} damage / {denominator} rounds: {value} damage "
        "per round. Combine totals across games rather than averaging match averages.",
        stats=("numbers.damage_dealt", "numbers.rounds_played"),
        rule="pooled_ratio",
        numerator=("numbers.damage_dealt",),
        denominator=("numbers.rounds_played",),
        direction="lowest",
    ),
    QuestionTemplate(
        id="highest-recent-adr-v1",
        question="This player dealt {value} damage per round over their last {last_n} games, "
        "the highest in the group. Who is it?",
        explanation="{name} dealt {numerator} damage / {denominator} rounds: {value} damage "
        "per round. Combine totals across games rather than averaging match averages.",
        stats=("numbers.damage_dealt", "numbers.rounds_played"),
        rule="pooled_ratio",
        numerator=("numbers.damage_dealt",),
        denominator=("numbers.rounds_played",),
    ),
    QuestionTemplate(
        id="highest-recent-round-damage-v1",
        question="This player dealt {value} damage in a single round, out of their last "
        "{last_n} games, the most in the group. Who is it?",
        explanation="{name}'s highest damage in one round was {value} across their last "
        "{last_n} games. This is a single-round maximum, not total match damage.",
        stats=("numbers.max_round_damage",),
        rule="max_in_match",
        stat="numbers.max_round_damage",
    ),
    # FF-410: Question Pack 7
    QuestionTemplate(
        id="most-recent-competitive-games-v1",
        question="This player played {value} Competitive games in their last 10 available games. Who is it?",
        explanation="{name} played {value} Competitive games.",
        stats=("labels.queue_id",),
        rule="count",
        stat="matches",
        queues=("competitive",),
    ),
    QuestionTemplate(
        id="most-recent-competitive-wins-v1",
        question="This player won {value} of their last 10 Competitive games. Who is it?",
        explanation="{name} won {value} Competitive games.",
        stats=("won", "labels.queue_id", "labels.result"),
        rule="count",
        stat="won",
        queues=("competitive",),
    ),
    QuestionTemplate(
        id="most-recent-kills-v1",
        question="This player recorded {value} kills across their last 10 games. Who is it?",
        explanation="{name} recorded {value} kills across their last 10 games.",
        stats=("kills",),
        rule="count",
        stat="kills",
    ),
    QuestionTemplate(
        id="most-recent-assists-v1",
        question="This player recorded {value} assists across their last 10 games. Who is it?",
        explanation="{name} recorded {value} assists across their last 10 games.",
        stats=("assists",),
        rule="count",
        stat="assists",
    ),
    QuestionTemplate(
        id="most-recent-deaths-v1",
        question="This player recorded {value} deaths across their last 10 games. Who is it?",
        explanation="{name} recorded {value} deaths across their last 10 games.",
        stats=("deaths",),
        rule="count",
        stat="deaths",
    ),
    QuestionTemplate(
        id="most-recent-first-bloods-v1",
        question="This player recorded {value} first bloods across their last 10 games. Who is it?",
        explanation="{name} recorded {value} first bloods across their last 10 games.",
        stats=("numbers.first_bloods",),
        rule="count",
        stat="numbers.first_bloods",
    ),
    QuestionTemplate(
        id="most-recent-plants-v1",
        question="This player planted the spike {value} times in their last 10 games. Who is it?",
        explanation="{name} planted the spike {value} times.",
        stats=("numbers.plants",),
        rule="count",
        stat="numbers.plants",
    ),
    QuestionTemplate(
        id="most-recent-defuses-v1",
        question="This player defused the spike {value} times in their last 10 games. Who is it?",
        explanation="{name} defused the spike {value} times.",
        stats=("numbers.defuses",),
        rule="count",
        stat="numbers.defuses",
    ),
    QuestionTemplate(
        id="highest-recent-kda-v1",
        question="This player had a KDA ratio of {value} across their last 10 games. Who is it?",
        explanation="{name} had a KDA ratio of {value}.",
        stats=("kills", "assists", "deaths"),
        rule="pooled_ratio",
        numerator=("kills", "assists"),
        denominator=("deaths",),
    ),
    QuestionTemplate(
        id="most-recent-first-deaths-v1",
        question="This player recorded {value} first deaths across their last 10 games. Who is it?",
        explanation="{name} recorded {value} first deaths.",
        stats=("numbers.first_deaths",),
        rule="count",
        stat="numbers.first_deaths",
    ),
)
