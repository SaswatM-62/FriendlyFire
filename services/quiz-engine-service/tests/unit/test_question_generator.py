"""Properties and concrete regressions for the FF-402 question engine."""

from dataclasses import replace
from fractions import Fraction

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import QuizPlayer
from hypothesis import given
from hypothesis import strategies as st

from quiz_engine.question_generator import QuestionGenerator
from quiz_engine.templates import TEMPLATES


def players() -> list[QuizPlayer]:
    return [
        QuizPlayer(user_id="a", display_name="Alice"),
        QuizPlayer(user_id="b", display_name="Bob"),
    ]


def row(
    uid: str,
    match: str,
    kills: int = 1,
    score: float = 100,
    rounds: float = 10,
    seconds: int = 1,
    won: bool = False,
) -> PlayerMatchStats:
    result = PlayerMatchStats(
        user_id=uid,
        match_id=match,
        game="valorant",
        kills=kills,
        won=won,
        labels={"queue_id": "competitive", "result": "win" if won else "loss"},
        numbers={"combat_score": score, "rounds_played": rounds},
    )
    result.played_at.seconds = seconds
    return result


def history() -> list[PlayerMatchStats]:
    return [
        row(
            uid,
            f"shared-{n}" if n < 5 else f"{uid}-{n}",
            kills=30 if uid == "a" else 10,
            score=400 if uid == "a" else 100,
            seconds=n + 1,
            won=uid == "a",
        )
        for n in range(10)
        for uid in ("a", "b")
    ]


@st.composite
def players_and_stats(draw: st.DrawFn) -> tuple[list[QuizPlayer], list[PlayerMatchStats]]:
    ids = draw(st.lists(st.uuids().map(str), min_size=2, max_size=5, unique=True))
    roster = [QuizPlayer(user_id=uid, display_name=f"Player {n}") for n, uid in enumerate(ids)]
    stats = []
    for n in range(10):
        for uid in ids:
            stats.append(
                row(
                    uid,
                    f"shared-{n}" if n < 5 else f"{uid}-{n}",
                    kills=draw(st.integers(0, 60)),
                    score=draw(st.integers(0, 10000)),
                    rounds=draw(st.integers(1, 30)),
                    seconds=n + 1,
                    won=draw(st.booleans()),
                )
            )
    return roster, stats


@given(inputs=players_and_stats(), seed=st.integers(min_value=0, max_value=2**31 - 1))
def test_every_question_has_exactly_one_correct_option_among_its_options(
    inputs: tuple[list[QuizPlayer], list[PlayerMatchStats]], seed: int
) -> None:
    roster, stats = inputs
    questions = QuestionGenerator().generate(roster, stats, question_count=5, seed=seed)
    ids = {p.user_id for p in roster}
    template_ids = []
    for question in questions:
        option_ids = [option.option_id for option in question.options]
        assert set(option_ids) == ids
        assert option_ids.count(question.correct_option_id) == 1
        template_id = question.question_id.split(":")[0]
        template_ids.append(template_id)
        # Independent semantic oracle: verify the question, not just its answer ID.
        selected = stats
        if "without other room players" in question.text:
            selected = [r for r in stats if not r.match_id.startswith("shared-")]
        elif "with other room players" in question.text:
            selected = [r for r in stats if r.match_id.startswith("shared-")]
        scores = {}
        for uid in ids:
            own = [r for r in selected if r.user_id == uid]
            if template_id == "most-kills-v1":
                scores[uid] = Fraction(max(r.kills for r in own))
            elif template_id == "highest-acs-v1":
                scores[uid] = sum(Fraction(r.numbers["combat_score"]) for r in own) / sum(
                    Fraction(r.numbers["rounds_played"]) for r in own
                )
            else:
                scores[uid] = Fraction(sum(r.won for r in own))
        winners = [uid for uid, score in scores.items() if score == max(scores.values())]
        assert winners == [question.correct_option_id]
    assert len(template_ids) == len(set(template_ids))
    assert len({q.question_id for q in questions}) == len(questions)
    assert questions == QuestionGenerator().generate(roster[::-1], stats[::-1], 5, seed)


def test_nonempty_quiz_has_three_templates_and_mixes_shared_and_solo() -> None:
    questions = QuestionGenerator().generate(players(), history(), 5, 42)
    assert len(questions) == 3
    assert all(q.correct_option_id == "a" for q in questions)
    assert any("with other room players" in q.text for q in questions)
    assert any("without other room players" in q.text for q in questions)


def test_outsiders_cannot_change_questions_or_leak_into_text_or_explanations() -> None:
    stats = history()
    original = [r.SerializeToString(deterministic=True) for r in stats]
    outsider = row("OUTSIDER_ID", "a-9", kills=999999, score=987654321)
    outsider.labels.update({"agent": "OUTSIDER_NAME", "map": "SECRET_MAP"})
    generator = QuestionGenerator()
    expected = generator.generate(players(), stats, 3, 7)
    actual = generator.generate(players(), [*stats, outsider], 3, 7)
    assert actual == expected
    assert actual
    for question in actual:
        rendered = question.text + question.explanation
        for secret in ("OUTSIDER_ID", "OUTSIDER_NAME", "SECRET_MAP", "999999", "987654321"):
            assert secret not in rendered
    assert original == [r.SerializeToString(deterministic=True) for r in stats]


def test_question_keys_are_seed_independent_and_can_be_excluded() -> None:
    generator = QuestionGenerator()
    first = generator.generate(players(), history(), 3, 1)
    excluded = {q.question_id for q in first}
    for seed in (0, 1, 42):
        actual = generator.generate(players(), history(), 3, seed, avoid_question_keys=excluded)
        assert not excluded.intersection(q.question_id for q in actual)
    assert generator.generate(players(), history(), 3, 1) == first


def test_excluding_a_key_excludes_identical_evidence_in_another_scope() -> None:
    generator = QuestionGenerator([TEMPLATES[0]])
    stats = [row("a", "a", kills=3), row("b", "b", kills=1)]
    first = generator.generate(players(), stats, 1, 0)
    assert len(first) == 1
    assert (
        generator.generate(players(), stats, 1, 8, avoid_question_keys=[first[0].question_id]) == []
    )


def test_same_fact_with_different_template_wording_is_not_repeated() -> None:
    template = replace(TEMPLATES[0], scopes=("all",))
    generator = QuestionGenerator([template, replace(template, id="paraphrase")])
    assert len(generator.generate(players(), history(), 2, 3)) == 1


def test_ties_and_empty_history_produce_no_questions() -> None:
    generator = QuestionGenerator()
    assert generator.generate(players(), [], 3, 0) == []
    assert generator.generate(players(), [row("a", "x"), row("b", "x")], 3, 0) == []


def test_missing_player_does_not_become_zero() -> None:
    assert QuestionGenerator().generate(players(), [row("a", "x", kills=50)], 3, 0) == []


def test_pooled_acs_uses_round_weighting_not_mean_of_match_acs() -> None:
    stats = [
        row("a", "a1", score=100, rounds=1),
        row("a", "a2", score=0, rounds=99),
        row("b", "b1", score=20, rounds=10),
    ]
    questions = QuestionGenerator([TEMPLATES[1]]).generate(players(), stats, 1, 0)
    assert len(questions) == 1
    assert questions[0].correct_option_id == "b"
    assert "20 total combat score / 10 rounds" in questions[0].explanation


@pytest.mark.parametrize("bad", [None, float("nan"), float("inf"), -1])
def test_missing_or_invalid_combat_score_skips_comparison(bad: float | None) -> None:
    stats = [row("a", "x", score=100), row("b", "x", score=200)]
    if bad is None:
        del stats[1].numbers["combat_score"]
    else:
        stats[1].numbers["combat_score"] = bad
    assert QuestionGenerator([TEMPLATES[1]]).generate(players(), stats, 1, 0) == []


def test_zero_rounds_skips_acs_and_zero_kills_are_valid() -> None:
    stats = [row("a", "x", kills=1, rounds=0), row("b", "x", kills=0)]
    questions = QuestionGenerator().generate(players(), stats, 3, 0)
    assert len(questions) == 1
    assert questions[0].correct_option_id == "a"


def test_recent_wins_filters_queue_before_taking_latest_ten_and_counts_draws() -> None:
    stats = history()
    for r in stats:
        if r.user_id == "a":
            r.won = False
            r.labels["result"] = "draw"
        else:
            r.won = True
            r.labels["result"] = "win"
    extra = row("a", "new-unrated", won=True, seconds=100)
    extra.labels["queue_id"] = "unrated"
    stats.append(extra)
    questions = QuestionGenerator([TEMPLATES[2]]).generate(players(), stats, 1, 0)
    assert len(questions) == 1
    assert questions[0].correct_option_id == "b"
    assert "10 of their last 10" in questions[0].explanation


@pytest.mark.parametrize(
    "case", ["short", "missing-time", "missing-result", "inconsistent", "boundary-tie"]
)
def test_recent_wins_requires_complete_unambiguous_window(case: str) -> None:
    stats = history()
    if case == "short":
        stats.pop()
    elif case == "missing-time":
        stats[0].ClearField("played_at")
    elif case == "missing-result":
        del stats[0].labels["result"]
    elif case == "inconsistent":
        stats[0].won = False
    else:
        stats.append(row("a", "extra", seconds=1))
    assert QuestionGenerator([TEMPLATES[2]]).generate(players(), stats, 1, 0) == []


def test_recent_wins_does_not_include_older_matches() -> None:
    stats = [*history(), row("b", "old", won=True, seconds=0)]
    assert (
        QuestionGenerator([TEMPLATES[2]]).generate(players(), stats, 1, 0)[0].correct_option_id
        == "a"
    )


def test_unknown_queue_is_not_silently_discarded() -> None:
    stats = history()
    del stats[0].labels["queue_id"]
    assert QuestionGenerator().generate(players(), stats, 3, 0) == []


def test_duplicate_rows_do_not_count_twice_and_conflicts_are_rejected() -> None:
    stats = history()
    generator = QuestionGenerator()
    assert generator.generate(players(), stats * 2, 3, 0) == generator.generate(
        players(), stats, 3, 0
    )
    conflict = PlayerMatchStats()
    conflict.CopyFrom(stats[0])
    conflict.kills += 1
    with pytest.raises(ValueError, match="Conflicting"):
        generator.generate(players(), [*stats, conflict], 3, 0)


@pytest.mark.parametrize("roster", [[], players()[:1], players() * 2, [QuizPlayer(), players()[0]]])
def test_invalid_roster_is_rejected(roster: list[QuizPlayer]) -> None:
    with pytest.raises(ValueError, match="unique nonempty"):
        QuestionGenerator().generate(roster, [], 1, 0)


def test_counts_and_duplicate_template_ids() -> None:
    generator = QuestionGenerator()
    assert generator.generate(players(), history(), 0, 0) == []
    assert len(generator.generate(players(), history(), 1, 0)) == 1
    with pytest.raises(ValueError, match="nonnegative"):
        generator.generate(players(), history(), -1, 0)
    with pytest.raises(ValueError, match="Template IDs"):
        QuestionGenerator([TEMPLATES[0], TEMPLATES[0]])


def test_questions_have_no_weekdays_or_wall_clock_dependency() -> None:
    questions = QuestionGenerator().generate(players(), history(), 3, 0)
    assert questions
    for q in questions:
        for weekday in (
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday",
        ):
            assert weekday not in q.text + q.explanation
