"""Turns match stats into questions.
Pure, deterministic room-only question generation; no I/O or wall clock.

Returns up to question_count questions. The RPC layer owns minimum quiz length,
seed=0 randomization and translating an insufficient result into an RPC error.
Question.question_id is the stable key callers may pass in avoid_question_keys.
"""

import hashlib
import json
import math
import random
from collections import defaultdict
from collections.abc import Collection, Sequence
from dataclasses import dataclass, replace
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


def _recent(rows: list[PlayerMatchStats], n: int) -> list[PlayerMatchStats] | None:
    if (
        n < 1
        or len(rows) < n
        or any(
            not r.HasField("played_at")
            or not -62135596800 <= r.played_at.seconds <= 253402300799
            or not 0 <= r.played_at.nanos < 1_000_000_000
            for r in rows
        )
    ):
        return None
    ordered = sorted(rows, key=lambda r: (_timestamp(r), r.match_id), reverse=True)
    if len(ordered) > n and _timestamp(ordered[n - 1]) == _timestamp(ordered[n]):
        return None
    return ordered[:n]


def _evaluate(template: QuestionTemplate, rows: list[PlayerMatchStats]) -> _Value | None:
    if not rows:
        return None
    if template.last_n is not None:
        recent = _recent(rows, template.last_n)
        if recent is None:
            return None
        rows = recent
    rows = sorted(rows, key=lambda r: r.match_id)
    window_evidence = tuple((r.match_id, str(_timestamp(r))) for r in rows)
    if template.parameter_label:
        if any(not r.labels.get(template.parameter_label) for r in rows):
            return None
        rows = [r for r in rows if r.labels[template.parameter_label] == template.parameter_value]
        if not rows and template.rule != "agent_games":
            return None
    if template.rule == "max_rank":
        rows = [r for r in rows if r.labels.get("queue_id") == "competitive"]
        if not rows:
            return None
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
    elif template.rule in {"max_rank", "max_headshots", "max_round_damage"}:
        key = {
            "max_rank": "rank_tier",
            "max_headshots": "headshots",
            "max_round_damage": "max_round_damage",
        }[template.rule]
        numbers = [_number(r, key) for r in rows]
        if any(v is None or (key == "rank_tier" and v <= 0) for v in numbers):
            return None
        known = [v for v in numbers if v is not None]
        score = max(known)
        numerator, denominator = score, Fraction(1)
        source = rows[known.index(score)].match_id
        evidence = [(r.match_id, str(v)) for r, v in zip(rows, known, strict=True)]
    elif template.rule == "agent_games":
        score = numerator = Fraction(len(rows))
        denominator = Fraction(template.last_n or len(window_evidence))
        source = rows[0].match_id if rows else window_evidence[0][0]
        evidence = [(r.match_id, template.parameter_value) for r in rows]
    elif template.rule == "map_win_rate":
        if any(
            r.labels.get("result") not in {"win", "loss", "draw"}
            or r.won != (r.labels.get("result") == "win")
            for r in rows
        ):
            return None
        numerator, denominator = Fraction(sum(r.won for r in rows)), Fraction(len(rows))
        score = numerator / denominator
        source = rows[0].match_id
        evidence = [(r.match_id, r.labels["result"]) for r in rows]
    elif template.rule in {
        "pooled_kd",
        "agent_kd",
        "pooled_headshots",
        "lowest_adr",
        "highest_adr",
    }:
        numerator, denominator = Fraction(0), Fraction(0)
        for r in rows:
            if template.rule in {"pooled_kd", "agent_kd"}:
                if r.kills < 0 or r.deaths < 0:
                    return None
                top, bottom = Fraction(r.kills), Fraction(r.deaths)
            elif template.rule == "pooled_headshots":
                head, body, leg = (_number(r, k) for k in ("headshots", "bodyshots", "legshots"))
                if head is None or body is None or leg is None:
                    return None
                top, bottom = head, head + body + leg
            else:
                damage, rounds = _number(r, "damage_dealt"), _number(r, "rounds_played")
                if damage is None or rounds is None or rounds <= 0:
                    return None
                top, bottom = damage, rounds
            numerator += top
            denominator += bottom
            evidence.append((r.match_id, str(top), str(bottom)))
        if denominator <= 0:
            return None
        score = numerator / denominator
        source = rows[0].match_id
    else:
        raise ValueError(f"Unsupported rule: {template.rule}")
    if template.last_n is not None:
        evidence = [*window_evidence, *evidence]
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
        avoid_question_keys: Collection[str] = (),
    ) -> list[Question]:
        """Generate at most one question per template and distinct underlying fact.

        Shared means >=2 room players in a match, not a party/queue claim. When
        possible, include both shared-only and solo-only evidence. Incomplete
        comparisons (any room player missing required data) are skipped.
        Input ordering and outsiders never influence output, including IDs.
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
        avoided = set(avoid_question_keys)
        expanded: list[QuestionTemplate] = []
        for template in self._templates:
            if template.parameter_label and not template.parameter_value:
                parameters = sorted(
                    {
                        r.labels[template.parameter_label]
                        for r in unique.values()
                        if r.game == template.game and r.labels.get(template.parameter_label)
                    }
                )
                expanded.extend(replace(template, parameter_value=v) for v in parameters)
            else:
                expanded.append(template)
        for template in expanded:
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
                extreme = min if template.lowest else max
                target = extreme(v.score for v in complete.values())
                winners = [uid for uid, v in complete.items() if v.score == target]
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
                        template.parameter_label,
                        template.parameter_value,
                        template.lowest,
                        [(uid, v.evidence) for uid, v in complete.items()],
                    )
                )
                question_key = f"{template.id}:{fact_key}"
                if question_key in avoided:
                    continue
                context = {
                    "name": next(p.display_name for p in players if p.user_id == winner),
                    "scope": _SCOPE_TEXT[scope],
                    "value": _display(value.score),
                    "numerator": _display(value.numerator),
                    "denominator": _display(value.denominator),
                    "last_n": template.last_n,
                    "parameter": template.parameter_value,
                    "percent": _display(value.score * 100),
                }
                question = Question(
                    question_id=question_key,
                    type=QUESTION_TYPE_PICK_THE_PLAYER,
                    text=template.question.format(**context),
                    explanation=template.explanation.format(**context),
                    correct_option_id=winner,
                    # For aggregates this is representative evidence, not the sole
                    # source. The key fingerprints the full comparison evidence.
                    source_match_id=value.source_match_id,
                    options=[
                        AnswerOption(option_id=p.user_id, label=p.display_name) for p in players
                    ],
                )
                candidates.append(_Candidate(template.id, fact_key, scope, question))

        rng = random.Random(seed)
        rng.shuffle(candidates)
        best = _select(candidates, question_count)
        rng.shuffle(best)
        return [c.question for c in best]


def _select(candidates: list[_Candidate], count: int) -> list[_Candidate]:
    """Match templates to distinct facts without enumerating the entire bank.

    Try each shared/solo pair as mandatory first choices to preserve the engine's
    preference for a mixed quiz, then maximize size with augmenting paths.
    """
    by_template: dict[str, list[_Candidate]] = {}
    for candidate in candidates:
        by_template.setdefault(candidate.template_id, []).append(candidate)
    starts: list[list[_Candidate]] = [[]] + [
        [shared, solo]
        for shared in candidates
        if shared.scope == "shared"
        for solo in candidates
        if solo.scope == "solo"
        and shared.template_id != solo.template_id
        and shared.fact_key != solo.fact_key
    ]
    best: list[_Candidate] = []
    best_rank = (-1, -1)
    for forced in starts:
        if len(forced) > count:
            continue
        fixed = {c.template_id for c in forced}
        matched = {c.fact_key: c for c in forced}

        def assign(
            template_id: str,
            visited: set[str],
            matched: dict[str, _Candidate],
            fixed: set[str],
        ) -> bool:
            for candidate in by_template[template_id]:
                if candidate.fact_key in visited:
                    continue
                visited.add(candidate.fact_key)
                previous = matched.get(candidate.fact_key)
                if previous is None or (
                    previous.template_id not in fixed
                    and assign(previous.template_id, visited, matched, fixed)
                ):
                    matched[candidate.fact_key] = candidate
                    return True
            return False

        for template_id in by_template:
            if template_id not in fixed:
                assign(template_id, set(), matched, fixed)
        selected = forced + [c for c in matched.values() if c.template_id not in fixed]
        selected = selected[:count]
        scopes = {c.scope for c in selected}
        rank = (len(selected), int("shared" in scopes and "solo" in scopes))
        if rank > best_rank:
            best, best_rank = selected, rank
    return best
