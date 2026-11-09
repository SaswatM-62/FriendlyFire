"""Data-only question templates transcribed from the sample question bank.

Catalogue: docs/stats-catalogue.md. New wording for an existing rule needs only
another entry; a new calculation requires an evaluator in question_generator.py.
"""

from dataclasses import dataclass
from typing import Literal

Scope = Literal["all", "shared", "solo"]
Rule = Literal["max_kills", "pooled_acs", "recent_wins"]


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


TEMPLATES = (
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
