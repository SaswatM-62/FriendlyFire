"""Pure, deterministic room-only question generation; no I/O or wall clock.

Returns up to question_count questions. The RPC layer owns minimum quiz length,
seed=0 randomization and translating an insufficient result into an RPC error.
Question.template_id is what callers pass back in asked_template_ids. How a
template is evaluated is described in templates.py.
"""

import hashlib
import json
import math
import random
import re
from collections import defaultdict
from collections.abc import Sequence
from dataclasses import dataclass, replace
from fractions import Fraction
from string import Formatter
from typing import Literal

from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import (
    QUESTION_TYPE_PICK_THE_PLAYER,
    AnswerOption,
    Question,
    QuizPlayer,
)

from quiz_engine.templates import TEMPLATES, Direction, QuestionTemplate, Scope

_SCOPE_TEXT: dict[Scope, str] = {
    "all": "in the available Competitive and Unrated match history",
    "shared": "in available matches played with other room players",
    "solo": "in available matches played without other room players",
}
_SCALARS = ("kills", "deaths", "assists")
_PLACEHOLDERS = (
    "name",
    "scope",
    "value",
    "numerator",
    "denominator",
    "percent",
    "parameter",
    "friend",
    "last_n",
)
# {singular|plural}: the first form when {value} shows as 1, else the second.
_PLURAL = re.compile(r"\{([^{}|]*)\|([^{}|]*)\}")
# Decimals shown for values that aren't whole: {value}, {numerator} and
# {denominator} (e.g. 1.28 kills per death), and {percent} (e.g. 29.7%).
VALUE_PLACES = 2
PERCENT_PLACES = 1

Difficulty = Literal["easy", "medium", "hard"]
LEVELS: tuple[Difficulty, ...] = ("easy", "medium", "hard")
_RANK: dict[Difficulty, int] = {"easy": 0, "medium": 1, "hard": 2}
# Scarce hard questions pick first.
_FILL_ORDER: tuple[Difficulty, ...] = ("hard", "medium", "easy")
EASY_LEAD = Fraction(35, 100)
MEDIUM_LEAD = Fraction(15, 100)
# (longest quiz in the band, easy share, hard share); medium takes the rest.
_MIX_BANDS: tuple[tuple[int, Fraction, Fraction], ...] = (
    (7, Fraction(60, 100), Fraction(0)),
    (12, Fraction(40, 100), Fraction(20, 100)),
)
_LONG_MIX = (Fraction(30, 100), Fraction(30, 100))
# Where each level looks for questions, its own pool first, then the nearest level.
_BORROW: dict[Difficulty, tuple[Difficulty, ...]] = {
    "hard": ("hard", "medium", "easy"),
    "medium": ("medium", "easy", "hard"),
    "easy": ("easy", "medium", "hard"),
}


@dataclass(frozen=True)
class _Value:
    score: Fraction
    numerator: Fraction
    denominator: Fraction
    evidence: tuple[tuple[str, ...], ...]
    source_match_id: str
    # The display_label from the winning match, for max_in_match.
    label: str = ""


@dataclass(frozen=True)
class _Candidate:
    template_id: str
    fact_key: str
    scope: Scope
    difficulty: Difficulty
    question: Question


def difficulty(best: Fraction, runner_up: Fraction, direction: Direction = "highest") -> Difficulty:
    """How hard a question is to answer, from the winner's lead over the runner-up.

    The lead is (best - runner_up) / best, or (runner_up - best) / runner_up when the
    lowest value wins: 35% or more is easy, 15-35% medium and under 15% hard, so a
    hard question is always a near-tie. Values are nonnegative and the winner is
    unique, so the divisor is positive.
    """
    lead = (runner_up - best) / runner_up if direction == "lowest" else (best - runner_up) / best
    if lead >= EASY_LEAD:
        return "easy"
    if lead >= MEDIUM_LEAD:
        return "medium"
    return "hard"


def quiz_mix(question_count: int) -> dict[Difficulty, int]:
    """How many questions of each difficulty a quiz of this length asks for.

    5-7 questions: 60% easy, 40% medium. 8-12: 40% easy, 40% medium, 20% hard.
    13-20: 30% easy, 40% medium, 30% hard. Easy rounds up, hard rounds down and
    medium takes the rest. Shorter and longer quizzes use the nearest band.
    """
    easy_share, hard_share = next(
        ((easy, hard) for longest, easy, hard in _MIX_BANDS if question_count <= longest),
        _LONG_MIX,
    )
    easy = math.ceil(question_count * easy_share)
    hard = math.floor(question_count * hard_share)
    return {"easy": easy, "medium": question_count - easy - hard, "hard": hard}


def arrange(levels: Sequence[Difficulty]) -> list[int]:
    """Quiz order for questions of these difficulties, as indexes into levels.

    The hardest questions go in the middle and the quiz eases off towards both
    ends (10 questions: E E M M H H M M E E). Questions of one difficulty keep
    their order.
    """
    count = len(levels)
    middle = (count - 1) // 2
    # Slots from the middle outwards: middle, right, left, right, left, ...
    slots = [middle + (step + 1) // 2 if step % 2 else middle - step // 2 for step in range(count)]
    hardest_first = sorted(levels, key=lambda level: _RANK[level], reverse=True)
    level_at: list[Difficulty] = list(levels)
    for slot, level in zip(slots, hardest_first, strict=True):
        level_at[slot] = level
    waiting = {level: [i for i, own in enumerate(levels) if own == level] for level in LEVELS}
    return [waiting[level].pop(0) for level in level_at]


def _key(value: object) -> str:
    return hashlib.sha256(json.dumps(value, separators=(",", ":")).encode()).hexdigest()


def _number(row: PlayerMatchStats, name: str) -> Fraction | None:
    if name not in row.numbers:
        return None
    value = row.numbers[name]
    if not math.isfinite(value) or value < 0:
        return None
    return Fraction(str(value))


def _stat(row: PlayerMatchStats, name: str) -> Fraction | None:
    """One stat of one match, or None when it's missing or invalid."""
    if name in _SCALARS:
        # Proto3 scalars have no presence. The upstream contract must supply them;
        # a legitimate zero must not be mistaken for a missing value.
        value: int = getattr(row, name)
        return Fraction(value) if value >= 0 else None
    if name == "won":
        result = row.labels.get("result")
        if result not in {"win", "loss", "draw"} or row.won != (result == "win"):
            return None
        return Fraction(int(row.won))
    if name == "matches":
        return Fraction(1)
    return _number(row, name.removeprefix("numbers."))


def _valid_stat(name: str) -> bool:
    return name in (*_SCALARS, "won", "matches") or (
        name.startswith("numbers.") and len(name) > len("numbers.")
    )


def _timestamp(row: PlayerMatchStats) -> tuple[int, int]:
    return row.played_at.seconds, row.played_at.nanos


def _valid_time(row: PlayerMatchStats) -> bool:
    return (
        row.HasField("played_at")
        and -62135596800 <= row.played_at.seconds <= 253402300799
        and 0 <= row.played_at.nanos < 1_000_000_000
    )


def _window(
    template: QuestionTemplate, rows: list[PlayerMatchStats]
) -> list[PlayerMatchStats] | None:
    """The matches a template looks at for one player, or None if that's unclear."""
    if template.last_n is None:
        return sorted(rows, key=lambda r: r.match_id)
    n = template.last_n
    if len(rows) < n or not all(_valid_time(r) for r in rows):
        return None
    newest = sorted(rows, key=lambda r: (_timestamp(r), r.match_id), reverse=True)
    # A tie at the window boundary makes membership ambiguous.
    if len(newest) > n and _timestamp(newest[n - 1]) == _timestamp(newest[n]):
        return None
    return newest[:n]


def _filtered(
    template: QuestionTemplate, window: list[PlayerMatchStats], parameter: str
) -> list[PlayerMatchStats] | None:
    if not template.parameter_label:
        return window
    values = [row.labels.get(template.parameter_label, "") for row in window]
    # A missing label can't silently remove a match.
    if any(not value for value in values):
        return None
    return [row for row, value in zip(window, values, strict=True) if value == parameter]


def _too_few(template: QuestionTemplate, rows: list[PlayerMatchStats]) -> bool:
    """Less denominator than min_denominator (e.g. too few clutch attempts) to compare.

    Missing data isn't too few: _measure then makes the value unknown."""
    if not template.min_denominator:
        return False
    total = Fraction(0)
    for row in rows:
        for name in template.denominator:
            part = _stat(row, name)
            if part is None:
                return False
            total += part
    return total < template.min_denominator


def _measure(template: QuestionTemplate, rows: list[PlayerMatchStats]) -> _Value | None:
    """One player's value for a template over these matches."""
    if template.rule == "pooled_ratio":
        numerator, denominator = Fraction(0), Fraction(0)
        evidence: list[tuple[str, ...]] = []
        for row in rows:
            tops = [_stat(row, name) for name in template.numerator]
            bottoms = [_stat(row, name) for name in template.denominator]
            parts = [*tops, *bottoms]
            if any(part is None for part in parts):
                return None
            numerator += sum(t for t in tops if t is not None)
            denominator += sum(b for b in bottoms if b is not None)
            evidence.append((row.match_id, *(str(part) for part in parts)))
        if denominator == 0:
            return None
        source = rows[0].match_id
        return _Value(numerator / denominator, numerator, denominator, tuple(evidence), source)
    values = [_stat(row, template.stat) for row in rows]
    if template.skip_missing:
        # Only max_in_match (_check): a match without the stat can't be the best one.
        rows = [row for row, v in zip(rows, values, strict=True) if v is not None]
        values = [v for v in values if v is not None]
        if not rows:
            return None
    known = [v for v in values if v is not None]
    if len(known) != len(values):
        return None
    evidence = [(row.match_id, str(v)) for row, v in zip(rows, known, strict=True)]
    source = rows[0].match_id if rows else ""
    if template.rule in ("count", "pair"):
        total = sum(known, Fraction(0))
        return _Value(total, total, Fraction(len(rows)), tuple(evidence), source)
    if not rows:
        return None
    if template.rule == "average":
        total = sum(known, Fraction(0))
        return _Value(total / len(rows), total, Fraction(len(rows)), tuple(evidence), source)
    if template.rule == "max_in_match":
        best = min(known) if template.direction == "lowest" else max(known)
        winner = rows[known.index(best)]
        label = ""
        if template.display_label:
            label = winner.labels.get(template.display_label, "")
            if not label:
                return None
        return _Value(best, best, Fraction(1), tuple(evidence), winner.match_id, label)
    raise ValueError(f"Unsupported rule: {template.rule}")


def _display(value: Fraction, places: int = VALUE_PLACES) -> str:
    """A whole number as it is; anything else rounded half up to `places` decimals.

    Values are nonnegative (_number and _stat reject negatives).
    """
    if value.denominator == 1:
        return str(value.numerator)
    scale = 10**places
    whole, part = divmod(math.floor(value * scale + Fraction(1, 2)), scale)
    return f"{whole}.{part:0{places}d}"


def _percent(value: Fraction) -> str:
    return _display(value * 100, PERCENT_PLACES)


def _plural(text: str, value: str) -> str:
    """Picks each {singular|plural} form for the displayed {value}."""
    return _PLURAL.sub(lambda m: m[1] if value == "1" else m[2], text)


def _check(template: QuestionTemplate) -> None:
    """Rejects a template the generator can't evaluate or render."""
    problem = ""
    stats = [template.stat] if template.stat else []
    if template.rule == "pooled_ratio":
        if not template.numerator or not template.denominator or template.stat:
            problem = "pooled_ratio needs numerator and denominator stats, and no stat"
        stats = [*template.numerator, *template.denominator]
    elif not template.stat or template.numerator or template.denominator:
        problem = f"{template.rule} needs a stat, and no numerator or denominator"
    if template.rule == "pair" and (
        not template.stat.startswith("numbers.") or template.parameter_label
    ):
        problem = "pair needs a numbers.<key> stat and no parameter_label"
    if template.display_label and template.rule != "max_in_match":
        problem = "display_label only works with max_in_match"
    if template.min_denominator < 0 or (
        template.min_denominator
        and (template.rule != "pooled_ratio" or not template.contenders_only)
    ):
        problem = "min_denominator needs a pooled_ratio with contenders_only"
    if template.skip_missing and template.rule != "max_in_match":
        problem = "skip_missing only works with max_in_match"
    if template.last_n is not None and template.last_n < 1:
        problem = "last_n must be at least 1"
    if not template.scopes:
        problem = "scopes can't be empty"
    if not problem and not all(_valid_stat(name) for name in stats):
        problem = 'stats must be "kills", "deaths", "assists", "won", "matches" or "numbers.<key>"'
    allowed = {*_PLACEHOLDERS, f"{template.display_label}_name"}
    for text in (template.question, template.explanation):
        text = _PLURAL.sub(r"\2", text)
        unknown = {name for _, name, _, _ in Formatter().parse(text) if name} - allowed
        if unknown and not problem:
            problem = f"unknown placeholder {{{sorted(unknown)[0]}}}"
    if problem:
        raise ValueError(f"Template {template.id}: {problem}")


class QuestionGenerator:
    def __init__(self, templates: Sequence[QuestionTemplate] = TEMPLATES) -> None:
        self._templates = tuple(sorted(templates, key=lambda t: t.id))
        if len({t.id for t in self._templates}) != len(self._templates):
            raise ValueError("Template IDs must be unique")
        for template in self._templates:
            _check(template)

    def generate(
        self,
        players: Sequence[QuizPlayer],
        stats: Sequence[PlayerMatchStats],
        question_count: int,
        seed: int,
        *,
        asked_template_ids: Sequence[str] = (),
    ) -> list[Question]:
        """Generate at most one question per template and distinct underlying fact.

        Shared means >=2 room players in a match, not a party/queue claim. When
        possible, include both shared-only and solo-only evidence. Incomplete
        comparisons (a room player missing required data) are skipped.
        Input ordering and outsiders never influence output, including IDs.

        asked_template_ids: Question.template_id of the questions already asked in the
        room, ordered by when each template was last asked, oldest first. A repeat means
        the same template, whatever the scope, parameter or answer.

        Each question is easy, medium or hard (difficulty()), and quiz_mix() says how
        many of each the quiz wants. Unused templates fill every level first; a level
        whose own pool runs short borrows from the nearest level. Only then are asked
        templates reused, the one asked longest ago first, in the same way. The quiz
        puts its hardest questions in the middle (arrange()).

        Every room player is an option in every question. Players with
        not_enough_matches are never compared, so they are never the answer, a pair
        question's friend or the source of a stat, and their missing data can't skip
        a question. Their matches still count when deciding whether a match was shared.
        """
        players = sorted(players, key=lambda p: p.user_id)
        ids = {p.user_id for p in players}
        if len(players) < 2 or len(ids) != len(players) or "" in ids:
            raise ValueError("At least two players with unique nonempty IDs are required")
        if question_count < 0:
            raise ValueError("question_count must be nonnegative")
        compared = {p.user_id for p in players if not p.not_enough_matches}
        if question_count == 0 or len(compared) < 2:
            return []

        # Filter outsiders before inspecting values, conflicts, or match membership.
        # Players who aren't compared stay in: their matches decide what was shared.
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
        names = {p.user_id: p.display_name for p in players}

        candidates: list[_Candidate] = []
        for template in self._templates:
            applicable = [
                r for r in unique.values() if r.game == template.game and r.user_id in compared
            ]
            # Unknown queue could belong to the selected cohort. Do not silently
            # remove it and claim an exhaustive comparison of available history.
            if any(not r.labels.get("queue_id") for r in applicable):
                continue
            for scope in sorted(set(template.scopes)):
                groups: dict[str, list[PlayerMatchStats]] = {uid: [] for uid in compared}
                for row in applicable:
                    shared = len(members[row.game, row.match_id]) >= 2
                    if row.labels.get("queue_id") not in template.queues:
                        continue
                    if (scope == "shared" and not shared) or (scope == "solo" and shared):
                        continue
                    groups[row.user_id].append(row)
                for parameter, participants in self._comparisons(template, groups, unique):
                    candidate = self._candidate(
                        template, scope, parameter, participants, players, names
                    )
                    if candidate is not None:
                        candidates.append(candidate)
        return self._select(candidates, question_count, seed, asked_template_ids)

    @staticmethod
    def _comparisons(
        template: QuestionTemplate,
        groups: dict[str, list[PlayerMatchStats]],
        unique: dict[tuple[str, str, str], PlayerMatchStats],
    ) -> list[tuple[str, dict[str, _Value]]]:
        """(parameter, each compared player's value) for every question in one scope."""
        windows: dict[str, list[PlayerMatchStats]] = {}
        for uid in sorted(groups):
            rows = groups[uid]
            too_few = not rows or (template.last_n is not None and len(rows) < template.last_n)
            if template.contenders_only and too_few:
                # Not enough matches in this scope (e.g. a squad player's few solo
                # games): this player sits the question out.
                continue
            window = _window(template, rows) if rows else None
            if window is not None:
                windows[uid] = window
            elif template.rule != "pair":
                # Unclear data, or too few matches: the comparison would be incomplete.
                return []
        if template.rule == "pair":
            return [
                (friend, pair)
                for friend in sorted(groups)
                if (pair := _pair_values(template, friend, windows, unique)) is not None
            ]
        parameters = [""]
        if template.parameter_label:
            parameters = sorted(
                {
                    label
                    for window in windows.values()
                    for row in window
                    if (label := row.labels.get(template.parameter_label, ""))
                }
            )
        result = []
        for parameter in parameters:
            values: dict[str, _Value] = {}
            for uid, window in windows.items():
                matches = _filtered(template, window, parameter)
                if matches is None:
                    values = {}
                    break
                if template.contenders_only and (not matches or _too_few(template, matches)):
                    continue
                measured = _measure(template, matches)
                if measured is None:
                    values = {}
                    break
                values[uid] = measured
            if len(values) >= 2:
                result.append((parameter, values))
        return result

    @staticmethod
    def _candidate(
        template: QuestionTemplate,
        scope: Scope,
        parameter: str,
        values: dict[str, _Value],
        players: list[QuizPlayer],
        names: dict[str, str],
    ) -> _Candidate | None:
        lowest = template.direction == "lowest"
        scores = {uid: v.score for uid, v in values.items()}
        best = min(scores.values()) if lowest else max(scores.values())
        winners = [uid for uid, score in scores.items() if score == best]
        if len(winners) != 1:
            return None
        winner = winners[0]
        value = values[winner]
        others = [score for uid, score in scores.items() if uid != winner]
        runner_up = min(others) if lowest else max(others)
        # A number in the question must single out the winner: if rounding shows the
        # runner-up's value the same way, the question is a tie as far as players see.
        in_question = {name for _, name, _, _ in Formatter().parse(template.question) if name}
        if ("value" in in_question and _display(best) == _display(runner_up)) or (
            "percent" in in_question and _percent(best) == _percent(runner_up)
        ):
            return None
        # A pair question's parameter is the friend's user ID; show their name.
        shown = names[parameter] if template.rule == "pair" else parameter
        # Scope is intentionally absent: identical evidence with different
        # wording is the same fact. Include all compared players' evidence.
        fact_key = _key(
            (
                template.game,
                template.rule,
                template.stat,
                template.numerator,
                template.denominator,
                template.direction,
                template.last_n,
                template.parameter_label,
                parameter,
                [(uid, v.evidence) for uid, v in sorted(values.items())],
            )
        )
        context = {
            "name": names[winner],
            "scope": _SCOPE_TEXT[scope],
            "value": _display(value.score),
            "numerator": _display(value.numerator),
            "denominator": _display(value.denominator),
            "percent": _percent(value.score),
            "parameter": shown,
            "friend": shown if template.rule == "pair" else "",
            "last_n": template.last_n,
            f"{template.display_label}_name": value.label,
        }
        shown_value = _display(value.score)
        text = _plural(template.question, shown_value).format(**context)
        question = Question(
            # Follows the template and the visible question and answer, so it stays
            # the same after a match sync.
            question_id=_key((template.id, scope, winner, text)),
            template_id=template.id,
            type=QUESTION_TYPE_PICK_THE_PLAYER,
            text=text,
            explanation=_plural(template.explanation, shown_value).format(**context),
            correct_option_id=winner,
            # For aggregates this is representative evidence, not the sole
            # source. fact_key fingerprints the full comparison evidence.
            source_match_id=value.source_match_id,
            # Every room player, including those this question didn't compare.
            options=[AnswerOption(option_id=p.user_id, label=p.display_name) for p in players],
        )
        rating = difficulty(best, runner_up, template.direction)
        return _Candidate(template.id, fact_key, scope, rating, question)

    @staticmethod
    def _select(
        candidates: list[_Candidate],
        question_count: int,
        seed: int,
        asked_template_ids: Sequence[str],
    ) -> list[Question]:
        rng = random.Random(seed)
        rng.shuffle(candidates)
        # A template's age is when it was last asked: its last place in the list.
        age: dict[str, int] = {}
        for index, template_id in enumerate(asked_template_ids):
            age[template_id] = index

        def repeat(candidate: _Candidate) -> bool:
            return candidate.template_id in age

        fresh = [c for c in candidates if not repeat(c)]
        # Stable: within one template, the shuffle still picks the scope.
        reused = sorted((c for c in candidates if repeat(c)), key=lambda c: age[c.template_id])
        need = quiz_mix(question_count)
        selected: list[_Candidate] = []
        templates: set[str] = set()
        facts: set[str] = set()
        # Every level takes from its own pool, then from the nearest level, then the
        # farthest; scarce hard questions pick first. Unused templates fill all of that
        # before any asked template is reused.
        for pool in (fresh, reused):
            for step in range(len(LEVELS)):
                for level in _FILL_ORDER:
                    source = _BORROW[level][step]
                    for candidate in pool:
                        if need[level] == 0:
                            break
                        if candidate.difficulty != source:
                            continue
                        if candidate.template_id in templates or candidate.fact_key in facts:
                            continue
                        selected.append(candidate)
                        templates.add(candidate.template_id)
                        facts.add(candidate.fact_key)
                        need[level] -= 1

        # Two bounded repair passes, never a Cartesian product. Retain the other scope
        # when swapping one in, and the difficulty mix. A reused template only swaps to
        # another scope of itself: fresh picks stay ahead of repeats, and the templates
        # asked longest ago stay in (swapping in a newer one could skip the same old
        # template every quiz).
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
                        if candidate.difficulty != old.difficulty:
                            continue
                        if repeat(candidate) and candidate.template_id != old.template_id:
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
        # Stable sort: fresh picks first in shuffled order, then reused ones oldest first,
        # including after a scope repair. arrange() keeps that order within each
        # difficulty. Do not shuffle the completed quiz again.
        selected.sort(key=lambda c: age.get(c.template_id, -1))
        return [selected[i].question for i in arrange([c.difficulty for c in selected])]


def _pair_values(
    template: QuestionTemplate,
    friend: str,
    windows: dict[str, list[PlayerMatchStats]],
    unique: dict[tuple[str, str, str], PlayerMatchStats],
) -> dict[str, _Value] | None:
    """Each player's pair count for one friend, over matches on the friend's team."""
    key = f"{template.stat}:{friend}"
    values: dict[str, _Value] = {}
    for uid, window in windows.items():
        if uid == friend:
            continue
        together = []
        for row in window:
            theirs = unique.get((row.game, row.match_id, friend))
            if theirs is None:
                continue
            mine_team, their_team = row.labels.get("team_id"), theirs.labels.get("team_id")
            if not mine_team or not their_team:
                return None
            if mine_team == their_team:
                together.append(row)
        if not together:
            continue
        value = _measure(replace(template, stat=key), together)
        if value is None:
            return None
        values[uid] = value
    return values if len(values) >= 2 else None
