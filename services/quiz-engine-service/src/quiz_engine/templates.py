"""Data-only question templates transcribed from the sample question bank.

Catalogue: docs/stats-catalogue.md. New wording for an existing rule needs only
another entry; a new calculation requires an evaluator in question_generator.py.
"""

from dataclasses import dataclass
from typing import Literal

Scope = Literal["all", "shared", "solo"]
Rule = Literal[
    "max_kills",
    "pooled_acs",
    "recent_wins",
    "max_rank",
    "pooled_kd",
    "pooled_headshots",
    "max_headshots",
    "agent_games",
    "map_win_rate",
    "agent_kd",
    "lowest_adr",
    "highest_adr",
    "max_round_damage",
]


@dataclass(frozen=True)
class QuestionTemplate:
    id: str
    question: str
    explanation: str
    stats: tuple[str, ...]
    rule: Rule
    scopes: tuple[Scope, ...] = ("all", "shared", "solo")
    game: str = "valorant"
    queues: tuple[str, ...] = ("competitive", "unrated")
    last_n: int | None = None
    parameter_label: Literal["", "agent", "map"] = ""
    parameter_value: str = ""
    lowest: bool = False


TEMPLATES: tuple[QuestionTemplate, ...] = (
    QuestionTemplate(
        id="most-kills-v1",
        question="Among these players, who recorded the most kills in a single match {scope}?",
        explanation="{name} recorded {value} kills in one match {scope}, the unique highest total.",
        stats=("kills", "labels.queue_id"),
        rule="max_kills",
    ),
    QuestionTemplate(
        id="highest-acs-v1",
        question="Among these players, who has the highest pooled average combat score {scope}?",
        explanation=(
            "{name} had the highest pooled ACS {scope}: {value} "
            "({numerator} total combat score / {denominator} rounds)."
        ),
        stats=("numbers.combat_score", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_acs",
    ),
    QuestionTemplate(
        id="recent-wins-v1",
        question="Among these players, who won the most games in their last {last_n} "
        "available competitive matches?",
        explanation="{name} won {value} of their last {last_n} available competitive matches "
        "(draws count as non-wins).",
        stats=("played_at", "labels.queue_id", "labels.result", "won"),
        rule="recent_wins",
        scopes=("all",),
        queues=("competitive",),
        last_n=10,
    ),
)


# Approved first-block questions claimed by Akhil (*) in the source document.
# Each uses the latest ten eligible games BEFORE filtering by agent/map/rank.
# Existing max-kills and pooled-ACS questions are deliberately not repeated.
RECENT_TEMPLATES = (
    QuestionTemplate(
        id="highest-recent-rank-v1",
        question="Who reached the highest competitive rank in their last {last_n} games?",
        explanation="{name} reached competitive tier {value} in their last {last_n} games.",
        stats=("numbers.rank_tier", "labels.queue_id"),
        rule="max_rank",
        scopes=("all",),
        last_n=10,
    ),
    QuestionTemplate(
        id="highest-recent-kd-v1",
        question="Who has the highest pooled K/D across their last {last_n} games?",
        explanation="{name} had {numerator} kills / {denominator} deaths: K/D {value}.",
        stats=("kills", "deaths"),
        rule="pooled_kd",
        scopes=("all",),
        last_n=10,
    ),
    QuestionTemplate(
        id="highest-recent-headshot-percentage-v1",
        question="Who has the highest pooled headshot percentage in their last {last_n} games?",
        explanation="{name} had {numerator} head hits / {denominator} total hits ({percent}%).",
        stats=("numbers.headshots", "numbers.bodyshots", "numbers.legshots"),
        rule="pooled_headshots",
        scopes=("all",),
        last_n=10,
    ),
    QuestionTemplate(
        id="most-recent-headshots-v1",
        question="Who recorded the most head hits in one of their last {last_n} games?",
        explanation="{name} recorded {value} head hits in one match (not headshot kills).",
        stats=("numbers.headshots",),
        rule="max_headshots",
        scopes=("all",),
        last_n=10,
    ),
    QuestionTemplate(
        id="most-recent-agent-games-v1",
        question="Who played {parameter} most often in their last {last_n} games?",
        explanation="{name} played {parameter} in {value} of their last {last_n} games.",
        stats=("labels.agent",),
        rule="agent_games",
        scopes=("all",),
        last_n=10,
        parameter_label="agent",
    ),
    QuestionTemplate(
        id="highest-recent-map-win-rate-v1",
        question="Who has the highest win rate on {parameter} within their last {last_n} games?",
        explanation="{name} won {numerator} / {denominator} matches on {parameter} ({percent}%). "
        "Draws count as non-wins.",
        stats=("labels.map", "labels.result", "won"),
        rule="map_win_rate",
        scopes=("all",),
        last_n=10,
        parameter_label="map",
    ),
    QuestionTemplate(
        id="highest-recent-agent-kd-v1",
        question="Who has the highest pooled K/D as {parameter} within their last {last_n} games?",
        explanation="{name} had {numerator} kills / {denominator} deaths as {parameter}: "
        "K/D {value}.",
        stats=("labels.agent", "kills", "deaths"),
        rule="agent_kd",
        scopes=("all",),
        last_n=10,
        parameter_label="agent",
    ),
    QuestionTemplate(
        id="lowest-recent-adr-v1",
        question="Who has the lowest pooled ADR across their last {last_n} games?",
        explanation="{name} dealt {numerator} damage / {denominator} rounds: ADR {value}.",
        stats=("numbers.damage_dealt", "numbers.rounds_played"),
        rule="lowest_adr",
        scopes=("all",),
        last_n=10,
        lowest=True,
    ),
    QuestionTemplate(
        id="highest-recent-adr-v1",
        question="Who has the highest pooled ADR across their last {last_n} games?",
        explanation="{name} dealt {numerator} damage / {denominator} rounds: ADR {value}.",
        stats=("numbers.damage_dealt", "numbers.rounds_played"),
        rule="highest_adr",
        scopes=("all",),
        last_n=10,
    ),
    QuestionTemplate(
        id="highest-recent-round-damage-v1",
        question="Who dealt the most damage in one round within their last {last_n} games?",
        explanation="{name} dealt {value} damage in a single round within those games.",
        stats=("numbers.max_round_damage",),
        rule="max_round_damage",
        scopes=("all",),
        last_n=10,
    ),
)

TEMPLATES += RECENT_TEMPLATES
