"""Pure, deterministic room-only question generation; no I/O or wall clock.

Returns up to question_count questions. The RPC layer owns minimum quiz length,
seed=0 randomization and translating an insufficient result into an RPC error.
Question.question_id is the stable key callers may pass in avoid_question_keys.
"""

import hashlib
import json
import math
import random
from collections import defaultdict
from collections.abc import Sequence
from dataclasses import dataclass
from fractions import Fraction

from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import (
    QUESTION_TYPE_PICK_THE_PLAYER,
    AnswerOption,
    Question,
    QuizPlayer,
)

from quiz_engine.templates import TEMPLATES, QuestionTemplate, Scope

_SCOPE_TEXT: dict[Scope, str] = {
    "all": "in the available Competitive and Unrated match history",
    "shared": "in available matches played with other room players",
    "solo": "in available matches played without other room players",
}


@dataclass(frozen=True)
class _Value:
    score: Fraction
    numerator: Fraction
    denominator: Fraction
    rows: tuple[PlayerMatchStats, ...]
    evidence: tuple[tuple[str, ...], ...]
    source_match_id: str


@dataclass(frozen=True)
class _Candidate:
    template_id: str
    fact_key: str
    scope: Scope
    question: Question


def _key(value: object) -> str:
    return hashlib.sha256(json.dumps(value, separators=(",", ":")).encode()).hexdigest()


def _number(row: PlayerMatchStats, name: str) -> Fraction | None:
    if name not in row.numbers:
        return None
    value = row.numbers[name]
    if not math.isfinite(value) or value < 0:
        return None
    return Fraction(str(value))


def _timestamp(row: PlayerMatchStats) -> tuple[int, int]:
    return row.played_at.seconds, row.played_at.nanos


def _evaluate(template: QuestionTemplate, rows: list[PlayerMatchStats]) -> _Value | None:
    if not rows:
        return None
    rows = sorted(rows, key=lambda r: r.match_id)
    evidence: list[tuple[str, ...]] = []
    if template.rule == "max_kills":
        # Proto3 scalars have no presence. The upstream contract must supply kills;
        # a legitimate zero must not be mistaken for a missing value.
        if any(row.kills < 0 for row in rows):
            return None
        score = Fraction(max(row.kills for row in rows))
        source = next(row.match_id for row in rows if row.kills == score)
        numerator, denominator = score, Fraction(1)
        evidence = [(r.match_id, str(r.kills)) for r in rows]
    elif template.rule == "pooled_acs":
        numerator, denominator = Fraction(0), Fraction(0)
        for row in rows:
            combat = _number(row, "combat_score")
            rounds = _number(row, "rounds_played")
            if combat is None or rounds is None or rounds <= 0:
                return None
            numerator += combat
            denominator += rounds
            evidence.append((row.match_id, str(combat), str(rounds)))
        score = numerator / denominator
        source = rows[0].match_id
    elif template.rule == "recent_wins":
        n = template.last_n
        if n is None or n < 1 or len(rows) < n:
            return None
        if any(
            not r.HasField("played_at")
            or not -62135596800 <= r.played_at.seconds <= 253402300799
            or not 0 <= r.played_at.nanos < 1_000_000_000
            for r in rows
        ):
            return None
        rows.sort(key=lambda r: (_timestamp(r), r.match_id), reverse=True)
        # A tie at the window boundary makes membership ambiguous.
        if len(rows) > n and _timestamp(rows[n - 1]) == _timestamp(rows[n]):
            return None
        rows = rows[:n]
        for row in rows:
            result = row.labels.get("result")
            if result not in {"win", "loss", "draw"} or row.won != (result == "win"):
                return None
            evidence.append((row.match_id, str(_timestamp(row)), result))
        score = Fraction(sum(row.won for row in rows))
        numerator, denominator = score, Fraction(n)
        source = rows[0].match_id
    else:
        raise ValueError(f"Unsupported rule: {template.rule}")
    return _Value(score, numerator, denominator, tuple(rows), tuple(evidence), source)


def _display(value: Fraction) -> str:
    return str(value.numerator) if value.denominator == 1 else f"{float(value):.6g}"


class QuestionGenerator:
    def __init__(self, templates: Sequence[QuestionTemplate] = TEMPLATES) -> None:
        self._templates = tuple(sorted(templates, key=lambda t: t.id))
        if len({t.id for t in self._templates}) != len(self._templates):
            raise ValueError("Template IDs must be unique")

    def generate(
        self,
        players: Sequence[QuizPlayer],
        stats: Sequence[PlayerMatchStats],
        question_count: int,
        seed: int,
        *,
        avoid_question_keys: Sequence[str] = (),
    ) -> list[Question]:
        """Generate at most one question per template and distinct underlying fact.

        Shared means >=2 room players in a match, not a party/queue claim. When
        possible, include both shared-only and solo-only evidence. Incomplete
        comparisons (any room player missing required data) are skipped.
        Input ordering and outsiders never influence output, including IDs.
        Avoided keys are ordered oldest first and reused only to fill a short quiz.
        """
        players = sorted(players, key=lambda p: p.user_id)
        ids = {p.user_id for p in players}
        if len(players) < 2 or len(ids) != len(players) or "" in ids:
            raise ValueError("At least two players with unique nonempty IDs are required")
        if question_count < 0:
            raise ValueError("question_count must be nonnegative")
        if question_count == 0:
            return []

        # Filter outsiders before inspecting values, conflicts, or match membership.
        unique: dict[tuple[str, str, str], PlayerMatchStats] = {}
        for row in stats:
            if row.user_id not in ids:
                continue
            if not row.match_id:
                raise ValueError("Room player stats require nonempty match IDs")
            identity = row.game, row.match_id, row.user_id
            if identity in unique and unique[identity] != row:
                raise ValueError("Conflicting records for the same player and match")
            unique[identity] = row
        members: dict[tuple[str, str], set[str]] = defaultdict(set)
        for row in unique.values():
            members[row.game, row.match_id].add(row.user_id)

        candidates: list[_Candidate] = []
        for template in self._templates:
            for scope in sorted(set(template.scopes)):
                groups: dict[str, list[PlayerMatchStats]] = {uid: [] for uid in ids}
                applicable = [r for r in unique.values() if r.game == template.game]
                # Unknown queue could belong to the selected cohort. Do not silently
                # remove it and claim an exhaustive comparison of available history.
                if any(not r.labels.get("queue_id") for r in applicable):
                    continue
                for row in applicable:
                    shared = len(members[row.game, row.match_id]) >= 2
                    if row.labels.get("queue_id") not in template.queues:
                        continue
                    if (scope == "shared" and not shared) or (scope == "solo" and shared):
                        continue
                    groups[row.user_id].append(row)
                values = {uid: _evaluate(template, groups[uid]) for uid in sorted(ids)}
                if any(v is None for v in values.values()):
                    continue
                complete = {uid: v for uid, v in values.items() if v is not None}
                maximum = max(v.score for v in complete.values())
                winners = [uid for uid, v in complete.items() if v.score == maximum]
                if len(winners) != 1:
                    continue
                winner = winners[0]
                value = complete[winner]
                # Scope is intentionally absent: identical evidence with different
                # wording is the same fact. Include all compared players' evidence.
                fact_key = _key(
                    (
                        template.game,
                        template.rule,
                        template.last_n,
                        [(uid, v.evidence) for uid, v in complete.items()],
                    )
                )
                context = {
                    "name": next(p.display_name for p in players if p.user_id == winner),
                    "scope": _SCOPE_TEXT[scope],
                    "value": _display(value.score),
                    "numerator": _display(value.numerator),
                    "denominator": _display(value.denominator),
                    "last_n": template.last_n,
                }
                text = template.question.format(**context)
                # Repeat identity follows the visible question and answer, not the
                # evidence or explanation, which can change after a match sync.
                question_key = f"{template.id}:{_key((scope, winner, text))}"
                question = Question(
                    question_id=question_key,
                    type=QUESTION_TYPE_PICK_THE_PLAYER,
                    text=text,
                    explanation=template.explanation.format(**context),
                    correct_option_id=winner,
                    # For aggregates this is representative evidence, not the sole
                    # source. fact_key fingerprints the full comparison evidence.
                    source_match_id=value.source_match_id,
                    options=[
                        AnswerOption(option_id=p.user_id, label=p.display_name) for p in players
                    ],
                )
                candidates.append(_Candidate(template.id, fact_key, scope, question))

        rng = random.Random(seed)
        rng.shuffle(candidates)
        # Preserve the first (oldest) occurrence if history contains duplicate keys.
        age: dict[str, int] = {}
        for index, key in enumerate(avoid_question_keys):
            age.setdefault(key, index)
        fresh = [c for c in candidates if c.question.question_id not in age]
        reused = sorted(
            (c for c in candidates if c.question.question_id in age),
            key=lambda c: age[c.question.question_id],
        )
        selected: list[_Candidate] = []
        templates: set[str] = set()
        facts: set[str] = set()
        for pool in (fresh, reused):
            for candidate in pool:
                if len(selected) == question_count:
                    break
                if candidate.template_id in templates or candidate.fact_key in facts:
                    continue
                selected.append(candidate)
                templates.add(candidate.template_id)
                facts.add(candidate.fact_key)

        # Two bounded repair passes, never a Cartesian product. Keep fresh picks
        # ahead of repeats and retain the other scope when swapping one in.
        if len(selected) >= 2:
            for scope in ("shared", "solo"):
                if any(c.scope == scope for c in selected):
                    continue
                scope_counts = {s: sum(c.scope == s for c in selected) for s in ("shared", "solo")}
                repaired = False
                for candidate in fresh + reused:
                    if candidate.scope != scope:
                        continue
                    for index in range(len(selected) - 1, -1, -1):
                        old = selected[index]
                        if (
                            candidate.question.question_id in age
                            and old.question.question_id not in age
                        ):
                            continue
                        if scope_counts.get(old.scope) == 1:
                            continue
                        if (
                            candidate.template_id in templates
                            and candidate.template_id != old.template_id
                        ) or (candidate.fact_key in facts and candidate.fact_key != old.fact_key):
                            continue
                        templates.remove(old.template_id)
                        facts.remove(old.fact_key)
                        templates.add(candidate.template_id)
                        facts.add(candidate.fact_key)
                        selected[index] = candidate
                        repaired = True
                        break
                    if repaired:
                        break
        # Stable sort keeps fresh picks shuffled and reused picks oldest first,
        # including after a scope repair. Do not shuffle the completed quiz again.
        selected.sort(key=lambda c: age.get(c.question.question_id, -1))
        return [c.question for c in selected]
