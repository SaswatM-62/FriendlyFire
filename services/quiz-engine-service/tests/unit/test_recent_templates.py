"""Fixture-based checks for the ten approved first-block questions."""

from dataclasses import replace
from fractions import Fraction

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import QuizPlayer
from hypothesis import given
from hypothesis import strategies as st

from quiz_engine.question_generator import QuestionGenerator, _evaluate
from quiz_engine.templates import RECENT_TEMPLATES, TEMPLATES, QuestionTemplate


@pytest.fixture
def roster() -> list[QuizPlayer]:
    return [
        QuizPlayer(user_id="a", display_name="Alice"),
        QuizPlayer(user_id="b", display_name="Bob"),
    ]


def record(uid: str, n: int, *, strong: bool) -> PlayerMatchStats:
    row = PlayerMatchStats(
        user_id=uid,
        match_id=f"{uid}-{n}",
        game="valorant",
        kills=30 if strong else 10,
        deaths=10,
        won=strong,
        labels={
            "queue_id": "competitive",
            "agent": "Jett",
            "map": "Ascent",
            "result": "win" if strong else "loss",
        },
        numbers={
            "rank_tier": 20 if strong else 10,
            "headshots": 8 if strong else 2,
            "bodyshots": 2 if strong else 8,
            "legshots": 0,
            "damage_dealt": 3000 if strong else 1000,
            "rounds_played": 10,
            "max_round_damage": 400 if strong else 200,
        },
    )
    row.played_at.seconds = n + 1
    return row


@pytest.fixture
def stats() -> list[PlayerMatchStats]:
    rows = [record(uid, n, strong=uid == "b") for uid in ("a", "b") for n in range(10)]
    for row in rows:
        if row.user_id == "a" and row.played_at.seconds <= 5:
            row.labels["agent"] = "Sage"
    return rows


def concrete(template: QuestionTemplate) -> QuestionTemplate:
    if template.parameter_label:
        return replace(
            template, parameter_value="Jett" if template.parameter_label == "agent" else "Ascent"
        )
    return template


@pytest.mark.parametrize("template", RECENT_TEMPLATES, ids=lambda t: t.id)
def test_one_correct_player_for_each_template(
    template: QuestionTemplate, roster: list[QuizPlayer], stats: list[PlayerMatchStats]
) -> None:
    questions = QuestionGenerator([concrete(template)]).generate(roster, stats, 1, 42)
    assert len(questions) == 1
    question = questions[0]
    assert question.correct_option_id == ("a" if template.lowest else "b")
    assert sorted(option.option_id for option in question.options) == ["a", "b"]
    assert sum(option.option_id == question.correct_option_id for option in question.options) == 1
    assert "10" in question.text
    assert "{" not in question.text + question.explanation
    assert question.source_match_id in {row.match_id for row in stats}


@pytest.mark.parametrize("template", RECENT_TEMPLATES, ids=lambda t: t.id)
def test_tied_leaders_are_skipped(template: QuestionTemplate, roster: list[QuizPlayer]) -> None:
    rows = [record(uid, n, strong=True) for uid in ("a", "b") for n in range(10)]
    assert QuestionGenerator([concrete(template)]).generate(roster, rows, 1, 42) == []


@pytest.mark.parametrize("template", RECENT_TEMPLATES, ids=lambda t: t.id)
def test_missing_history_is_skipped(
    template: QuestionTemplate, roster: list[QuizPlayer], stats: list[PlayerMatchStats]
) -> None:
    # Nine games cannot establish a last-ten comparison.
    assert QuestionGenerator([concrete(template)]).generate(roster, stats[1:], 1, 42) == []


REQUIRED_MAP_FIELDS = [
    (template, field)
    for template in RECENT_TEMPLATES
    for field in template.stats
    if field.startswith(("labels.", "numbers."))
]


@pytest.mark.parametrize(("template", "field"), REQUIRED_MAP_FIELDS)
def test_missing_required_stat_on_any_player_is_skipped(
    template: QuestionTemplate, field: str, roster: list[QuizPlayer], stats: list[PlayerMatchStats]
) -> None:
    # Pick a Jett game so agent-specific required fields are in scope.
    row = stats[9]
    map_name, key = field.split(".", 1)
    del getattr(row, map_name)[key]
    assert QuestionGenerator([concrete(template)]).generate(roster, stats, 1, 42) == []


@pytest.mark.parametrize("template", RECENT_TEMPLATES, ids=lambda t: t.id)
@pytest.mark.parametrize("problem", ["missing", "invalid", "boundary"])
def test_unreliable_timestamps_skip_last_ten(
    template: QuestionTemplate,
    problem: str,
    roster: list[QuizPlayer],
    stats: list[PlayerMatchStats],
) -> None:
    if problem == "missing":
        stats[0].ClearField("played_at")
    elif problem == "invalid":
        stats[0].played_at.nanos = -1
    else:
        extra = record("a", -1, strong=True)
        extra.played_at.CopyFrom(stats[0].played_at)
        stats.append(extra)
    assert QuestionGenerator([concrete(template)]).generate(roster, stats, 1, 42) == []


@pytest.mark.parametrize("template", RECENT_TEMPLATES, ids=lambda t: t.id)
def test_older_games_cannot_change_result(
    template: QuestionTemplate, roster: list[QuizPlayer], stats: list[PlayerMatchStats]
) -> None:
    generator = QuestionGenerator([concrete(template)])
    expected = generator.generate(roster, stats, 1, 42)
    older = record("a", -1, strong=True)
    older.kills = 10000
    older.numbers.update(
        {"damage_dealt": 1000000, "headshots": 10000, "rank_tier": 100, "max_round_damage": 1000000}
    )
    assert generator.generate(roster, [*stats, older], 1, 42) == expected


@pytest.mark.parametrize(
    "rule", ["pooled_kd", "agent_kd", "pooled_headshots", "lowest_adr", "highest_adr"]
)
def test_ratio_pools_raw_totals_instead_of_match_ratios(rule: str) -> None:
    template = concrete(next(t for t in RECENT_TEMPLATES if t.rule == rule))
    rows = [record("a", n, strong=False) for n in range(10)]
    for row in rows:
        row.kills, row.deaths = 1, 1
        row.numbers.update(
            {"headshots": 1, "bodyshots": 0, "legshots": 0, "damage_dealt": 1, "rounds_played": 1}
        )
    rows[-1].kills, rows[-1].deaths = 100, 1000
    rows[-1].numbers.update(
        {"headshots": 100, "bodyshots": 900, "damage_dealt": 100, "rounds_played": 1000}
    )
    value = _evaluate(template, rows)
    assert value is not None
    expected = Fraction(109, 1009)
    assert value.score == expected
    assert value.numerator == 109
    assert value.denominator == 1009


@pytest.mark.parametrize(
    "rule", ["pooled_kd", "agent_kd", "pooled_headshots", "lowest_adr", "highest_adr"]
)
def test_zero_denominator_is_unknown(rule: str) -> None:
    template = concrete(next(t for t in RECENT_TEMPLATES if t.rule == rule))
    rows = [record("a", n, strong=True) for n in range(10)]
    for row in rows:
        row.deaths = 0
        row.numbers.update({"headshots": 0, "bodyshots": 0, "legshots": 0, "rounds_played": 0})
    assert _evaluate(template, rows) is None


@pytest.mark.parametrize("bad", [float("nan"), float("inf"), -1])
@pytest.mark.parametrize(
    ("template", "field"), [(t, f) for t, f in REQUIRED_MAP_FIELDS if f.startswith("numbers.")]
)
def test_invalid_numeric_stat_skips_comparison(
    template: QuestionTemplate,
    field: str,
    bad: float,
    roster: list[QuizPlayer],
    stats: list[PlayerMatchStats],
) -> None:
    stats[9].numbers[field.split(".")[1]] = bad
    assert QuestionGenerator([concrete(template)]).generate(roster, stats, 1, 42) == []


def test_filters_apply_after_last_ten_and_do_not_backfill(stats: list[PlayerMatchStats]) -> None:
    for label, rule in (("agent", "agent_kd"), ("map", "map_win_rate")):
        template = concrete(next(t for t in RECENT_TEMPLATES if t.rule == rule))
        rows = [r for r in stats if r.user_id == "b"]
        for row in rows:
            row.labels[label] = "Other"
        old = record("b", -1, strong=True)
        assert _evaluate(template, [*rows, old]) is None


def test_rank_only_uses_competitive_games(stats: list[PlayerMatchStats]) -> None:
    template = next(t for t in RECENT_TEMPLATES if t.rule == "max_rank")
    rows = [r for r in stats if r.user_id == "a"]
    rows[-1].labels["queue_id"] = "unrated"
    rows[-1].numbers["rank_tier"] = 100
    value = _evaluate(template, rows)
    assert value is not None and value.score == 10


def test_win_rate_draws_and_unknown_results(stats: list[PlayerMatchStats]) -> None:
    template = concrete(next(t for t in RECENT_TEMPLATES if t.rule == "map_win_rate"))
    rows = [r for r in stats if r.user_id == "b"]
    rows[0].won = False
    rows[0].labels["result"] = "draw"
    value = _evaluate(template, rows)
    assert value is not None and value.score == Fraction(9, 10)
    rows[0].labels["result"] = "unknown"
    assert _evaluate(template, rows) is None
    rows[0].labels["result"] = "win"
    assert _evaluate(template, rows) is None


def test_ten_new_templates_are_distinct_from_existing_bank() -> None:
    assert len(RECENT_TEMPLATES) == 10
    assert len(TEMPLATES) == 13
    assert len({t.id for t in TEMPLATES}) == len(TEMPLATES)
    assert len({(t.rule, t.parameter_label, t.lowest) for t in TEMPLATES}) == len(TEMPLATES)


def test_full_bank_is_deterministic_and_does_not_repeat_templates(
    roster: list[QuizPlayer], stats: list[PlayerMatchStats]
) -> None:
    generator = QuestionGenerator(RECENT_TEMPLATES)
    questions = generator.generate(roster, stats, 20, 42)
    assert len(questions) == 10
    assert len({q.question_id.split(":")[0] for q in questions}) == 10
    assert questions == generator.generate(roster[::-1], stats[::-1], 20, 42)
    assert questions != generator.generate(roster, stats, 20, 43)


@given(seed=st.integers(0, 2**31 - 1), kills=st.integers(1, 1000))
def test_generated_kd_answer_matches_independent_oracle(seed: int, kills: int) -> None:
    template = next(t for t in RECENT_TEMPLATES if t.rule == "pooled_kd")
    rows = [record(uid, n, strong=False) for uid in ("a", "b") for n in range(10)]
    for row in rows:
        if row.user_id == "b":
            row.kills = kills
    players = [QuizPlayer(user_id=uid, display_name=uid) for uid in ("a", "b")]
    questions = QuestionGenerator([template]).generate(players, rows, 1, seed)
    if kills == 10:
        assert questions == []
    else:
        assert len(questions) == 1
        assert questions[0].correct_option_id == ("b" if kills > 10 else "a")


def test_unknown_map_and_agent_do_not_turn_into_zero(stats: list[PlayerMatchStats]) -> None:
    for rule in ("agent_games", "agent_kd", "map_win_rate"):
        template = concrete(next(t for t in RECENT_TEMPLATES if t.rule == rule))
        rows = [r for r in stats if r.user_id == "a"]
        value = _evaluate(template, rows)
        assert value is not None
        del rows[0].labels[template.parameter_label]
        assert _evaluate(template, rows) is None
        rows[0].labels[template.parameter_label] = (
            "Jett" if template.parameter_label == "agent" else "Ascent"
        )


def test_zero_agent_usage_is_known_but_no_agent_kd_is_unknown(
    stats: list[PlayerMatchStats],
) -> None:
    rows = [r for r in stats if r.user_id == "b"]
    usage = replace(
        next(t for t in RECENT_TEMPLATES if t.rule == "agent_games"), parameter_value="Sage"
    )
    value = _evaluate(usage, rows)
    assert value is not None and value.score == 0
    kd = replace(next(t for t in RECENT_TEMPLATES if t.rule == "agent_kd"), parameter_value="Sage")
    assert _evaluate(kd, rows) is None


def test_rounding_never_creates_or_breaks_ties() -> None:
    template = next(t for t in RECENT_TEMPLATES if t.rule == "pooled_kd")
    rows = [record(uid, n, strong=False) for uid in ("a", "b") for n in range(10)]
    for row in rows:
        row.kills = 1000000 if row.user_id == "a" else 1000001
        row.deaths = 1000000
    players = [QuizPlayer(user_id=uid, display_name=uid) for uid in ("a", "b")]
    result = QuestionGenerator([template]).generate(players, rows, 1, 0)
    assert len(result) == 1 and result[0].correct_option_id == "b"
