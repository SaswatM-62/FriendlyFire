"""Property tests for QuestionGenerator: run on many generated inputs, not one example."""

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import QuizPlayer
from hypothesis import given
from hypothesis import strategies as st

from quiz_engine.question_generator import QuestionGenerator


@st.composite
def players_and_stats(draw: st.DrawFn) -> tuple[list[QuizPlayer], list[PlayerMatchStats]]:
    user_ids = draw(st.lists(st.uuids().map(str), min_size=2, max_size=8, unique=True))
    players = [
        QuizPlayer(user_id=uid, display_name=f"Player {n}") for n, uid in enumerate(user_ids)
    ]
    stats = draw(
        st.lists(
            st.builds(
                PlayerMatchStats,
                match_id=st.uuids().map(str),
                user_id=st.sampled_from(user_ids),
                game=st.just("valorant"),
                won=st.booleans(),
                kills=st.integers(0, 60),
                deaths=st.integers(0, 60),
                assists=st.integers(0, 40),
                labels=st.fixed_dictionaries(
                    {
                        "agent": st.sampled_from(["Jett", "Sage", "Omen", "Sova"]),
                        "map": st.sampled_from(["Ascent", "Bind", "Haven", "Split"]),
                    }
                ),
            ),
            min_size=20,
            max_size=100,
        )
    )
    return players, stats


@pytest.mark.skip(reason="TODO(ff): enable when QuestionGenerator exists")
@given(inputs=players_and_stats(), seed=st.integers(min_value=1, max_value=2**31 - 1))
def test_every_question_has_exactly_one_correct_option_among_its_options(
    inputs: tuple[list[QuizPlayer], list[PlayerMatchStats]], seed: int
) -> None:
    players, stats = inputs

    questions = QuestionGenerator().generate(players, stats, question_count=5, seed=seed)  # type: ignore[attr-defined]

    for question in questions:
        option_ids = [option.option_id for option in question.options]
        assert option_ids.count(question.correct_option_id) == 1
