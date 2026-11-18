"""Data-only question templates transcribed from the sample question bank.

Catalogue: docs/stats-catalogue.md. A new question is another entry here; only a
new kind of calculation needs code in question_generator.py.
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
        question="Among these players, who recorded the most kills in a single match {scope}?",
        explanation="{name} recorded {value} kills in one match {scope}, the unique highest total.",
        stats=("kills", "labels.queue_id"),
        rule="max_in_match",
        stat="kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="highest-acs-v1",
        question="Among these players, who has the highest pooled average combat score {scope}?",
        explanation=(
            "{name} had the highest pooled ACS {scope}: {value} "
            "({numerator} total combat score / {denominator} rounds)."
        ),
        stats=("numbers.combat_score", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.combat_score",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="recent-wins-v1",
        question="Among these players, who won the most games in their last {last_n} "
        "available competitive matches?",
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
        "{value} times, the most in this group. Who is this player?",
        explanation=(
            "{name} opened {value} rounds with the first kill in their last {last_n} matches."
        ),
        stats=("numbers.first_bloods", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.first_bloods",
        scopes=ALL_SCOPES,
        last_n=5,
    ),
    QuestionTemplate(
        id="average-performance-score-v1",
        question="This player averages a Performance Score of {value} per match {scope}, "
        "the highest in this group. Who set the pace?",
        explanation="{name} averaged {numerator} Performance Score in {denominator} matches.",
        stats=("numbers.performance_score", "labels.queue_id"),
        rule="average",
        stat="numbers.performance_score",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="opening-duel-win-rate-v1",
        question="This player wins {percent}% of their opening duels {scope}, "
        "the best record in this group. Who takes the first fight?",
        explanation="{name}: {numerator} first bloods out of {denominator} opening duels.",
        stats=("numbers.first_bloods", "numbers.first_deaths", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.first_bloods",),
        denominator=("numbers.first_bloods", "numbers.first_deaths"),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="max-round-kills-v1",
        question="Who got the most enemy kills in a single round {scope}?",
        explanation="{name} got {value} enemy kills in one round {scope}, the most in the group.",
        stats=("numbers.max_round_kills", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_round_kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="post-plant-kills-v1",
        question="Who gets the most post-plant kills across their last {last_n} matches?",
        explanation="{name} got {value} enemy kills after the spike was planted.",
        stats=("numbers.post_plant_kills", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.post_plant_kills",
        scopes=ALL_SCOPES,
        last_n=5,
    ),
    QuestionTemplate(
        id="ninja-defuses-v1",
        question="Who pulled off the most ninja defuses in their last {last_n} matches?",
        explanation="{name} made {value} defuses while enemy players were still alive.",
        stats=("numbers.ninja_defuses", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.ninja_defuses",
        scopes=ALL_SCOPES,
        last_n=5,
    ),
    QuestionTemplate(
        id="clutch-win-rate-v1",
        question="This player wins {percent}% of their clutches {scope}, "
        "the best record in this group. Who stays cold under pressure?",
        explanation="{name}: {numerator} clutch wins out of {denominator} clutch attempts.",
        stats=("numbers.clutch_wins", "numbers.clutch_attempts", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.clutch_wins",),
        denominator=("numbers.clutch_attempts",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="fall-deaths-v1",
        question="In their last {last_n} matches, this player fell to their death "
        "{value} times, more than anyone else here. Who does this unfortunate stat belong to?",
        explanation="{name} recorded {value} fall deaths in their last {last_n} matches.",
        stats=("numbers.fall_deaths", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.fall_deaths",
        scopes=ALL_SCOPES,
        last_n=5,
    ),
    QuestionTemplate(
        id="pistol-round-kills-v1",
        question="This player averages {value} pistol-round kills per match {scope}, "
        "the most in this group. Who starts rounds with a bang?",
        explanation="{name}: {numerator} pistol-round kills in {denominator} matches.",
        stats=("numbers.pistol_round_kills", "labels.queue_id"),
        rule="average",
        stat="numbers.pistol_round_kills",
        scopes=ALL_SCOPES,
        last_n=10,
    ),
)
