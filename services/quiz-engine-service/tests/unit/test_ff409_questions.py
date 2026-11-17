"""Fixture coverage for FF-409 question templates."""

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import QuizPlayer

from quiz_engine.question_generator import QuestionGenerator
from quiz_engine.templates import TEMPLATES, QuestionTemplate

PACK6_TEMPLATES = TEMPLATES[3:]


@pytest.fixture
def players() -> list[QuizPlayer]:
    return [
        QuizPlayer(user_id="alice", display_name="Alice"),
        QuizPlayer(user_id="bob", display_name="Bob"),
    ]


def fixture_stats(template: QuestionTemplate, *, tied: bool = False) -> list[PlayerMatchStats]:
    rows = []
    number_key = template.stat.removeprefix("numbers.")
    for match_number in range(10):
        for user_id, value in (("alice", 8), ("bob", 8 if tied else 3)):
            row = PlayerMatchStats(
                user_id=user_id,
                match_id=f"match-{match_number}",
                game="valorant",
                labels={"queue_id": "competitive", "result": "win"},
            )
            row.played_at.seconds = match_number + 1
            row.numbers[number_key] = value
            rows.append(row)
    return rows


@pytest.mark.parametrize("template", PACK6_TEMPLATES, ids=lambda template: template.id)
def test_template_finds_exactly_one_correct_player(
    template: QuestionTemplate, players: list[QuizPlayer]
) -> None:
    questions = QuestionGenerator([template]).generate(
        players, fixture_stats(template), question_count=1, seed=7
    )

    assert len(questions) == 1
    question = questions[0]
    assert question.correct_option_id == "alice"
    assert {option.option_id for option in question.options} == {"alice", "bob"}
    assert sum(option.option_id == question.correct_option_id for option in question.options) == 1


@pytest.mark.parametrize("template", PACK6_TEMPLATES, ids=lambda template: template.id)
def test_template_skips_a_tie(template: QuestionTemplate, players: list[QuizPlayer]) -> None:
    questions = QuestionGenerator([template]).generate(
        players, fixture_stats(template, tied=True), question_count=1, seed=7
    )

    assert questions == []


@pytest.mark.parametrize("template", PACK6_TEMPLATES, ids=lambda template: template.id)
def test_template_skips_missing_stat(template: QuestionTemplate, players: list[QuizPlayer]) -> None:
    stats = fixture_stats(template)
    alice_match = next(row for row in stats if row.user_id == "alice" and row.match_id == "match-1")
    del alice_match.numbers[template.stat.removeprefix("numbers.")]

    questions = QuestionGenerator([template]).generate(players, stats, question_count=1, seed=7)

    assert questions == []


def test_pack_has_ten_distinct_templates_not_in_existing_engine_pack() -> None:
    assert len(PACK6_TEMPLATES) == 10
    assert len({template.id for template in TEMPLATES}) == len(TEMPLATES)
    assert {template.id for template in PACK6_TEMPLATES}.isdisjoint(
        {"most-kills-v1", "highest-acs-v1", "recent-wins-v1"}
    )
