"""ScoringService over gRPC, with quizzes kept in memory (FakeQuizzes)."""

from collections.abc import AsyncIterator
from datetime import UTC, datetime, timedelta

import grpc
import pytest
from friendlyfire.scoring.v1 import scoring_service_pb2_grpc
from friendlyfire.scoring.v1.scoring_service_pb2 import (
    END_REASON_COMPLETED,
    END_REASON_VOTED_TO_END,
    AnonymizePlayerRequest,
    FinalizeQuizRequest,
    GetPlayerHistoryRequest,
    GetQuizResultRequest,
    PlayerAnswer,
    QuizParticipant,
    RecordRoundRequest,
    StartQuizRequest,
)

from scoring.bootstrap import Overrides, Service, build_server
from scoring.settings import Settings
from tests.fakes import FakeCheck, FakeQuizzes

START = datetime(2026, 11, 19, 18, 0, tzinfo=UTC)


class Clock:
    def __init__(self) -> None:
        self.now = START

    def __call__(self) -> datetime:
        return self.now


@pytest.fixture
def clock() -> Clock:
    return Clock()


@pytest.fixture
async def scoring(clock: Clock) -> AsyncIterator[scoring_service_pb2_grpc.ScoringServiceStub]:
    service: Service = await build_server(
        Settings(grpc_port=0),
        Overrides(quizzes=FakeQuizzes(), dependency_checks=[FakeCheck.up("postgres")], now=clock),
    )
    await service.start()
    async with grpc.aio.insecure_channel(f"localhost:{service.port}") as channel:
        yield scoring_service_pb2_grpc.ScoringServiceStub(channel)
    await service.stop(grace=None)


def start_request(quiz_id: str = "q1", *players: tuple[str, str]) -> StartQuizRequest:
    return StartQuizRequest(
        quiz_id=quiz_id,
        room_code="K7QM2X",
        host_display_name="Maya",
        total_rounds=5,
        seconds_per_question=20,
        players=[
            QuizParticipant(user_id=uid, display_name=name)
            for uid, name in (players or (("a", "Alice"), ("b", "Bob"), ("c", "Cara")))
        ],
    )


def answer(uid: str, correct: bool, ms: int) -> PlayerAnswer:
    return PlayerAnswer(user_id=uid, answered=True, correct=correct, response_time_ms=ms)


async def code_of(call: object) -> grpc.StatusCode:
    with pytest.raises(grpc.aio.AioRpcError) as error:
        await call  # type: ignore[misc]
    return error.value.code()


async def test_a_round_scores_speed_and_ranks_ties_together(scoring) -> None:  # type: ignore[no-untyped-def]
    await scoring.StartQuiz(start_request())
    # 20-second limit: instant is 1000, halfway (10 s) is 750; wrong or no answer is 0.
    response = await scoring.RecordRound(
        RecordRoundRequest(
            quiz_id="q1",
            round_number=1,
            answers=[answer("a", True, 0), answer("b", True, 10_000), answer("c", False, 2_000)],
        )
    )
    assert {p.user_id: p.points for p in response.round_points} == {"a": 1000, "b": 750, "c": 0}
    board = [(e.user_id, e.total_points, e.correct_answers, e.rank) for e in response.scoreboard]
    assert board == [("a", 1000, 1, 1), ("b", 750, 1, 2), ("c", 0, 0, 3)]

    response = await scoring.RecordRound(
        RecordRoundRequest(
            quiz_id="q1",
            round_number=2,
            answers=[answer("a", True, 10_000), answer("b", True, 0)],
        )
    )
    # 1000 + 750 and 750 + 1000: Alice and Bob share first place; Cara (no answer) is third.
    board = [(e.user_id, e.total_points, e.rank) for e in response.scoreboard]
    assert board == [("a", 1750, 1), ("b", 1750, 1), ("c", 0, 3)]


async def test_recording_a_round_again_replaces_it(scoring) -> None:  # type: ignore[no-untyped-def]
    await scoring.StartQuiz(start_request())
    first = RecordRoundRequest(quiz_id="q1", round_number=1, answers=[answer("a", True, 0)])
    await scoring.RecordRound(first)
    response = await scoring.RecordRound(first)
    assert response.scoreboard[0].total_points == 1000


async def test_start_quiz_errors(scoring) -> None:  # type: ignore[no-untyped-def]
    await scoring.StartQuiz(start_request())
    assert await code_of(scoring.StartQuiz(start_request())) == grpc.StatusCode.ALREADY_EXISTS
    duplicate = start_request("q2", ("a", "Alice"), ("a", "Alice again"))
    assert await code_of(scoring.StartQuiz(duplicate)) == grpc.StatusCode.INVALID_ARGUMENT
    no_rounds = start_request("q3")
    no_rounds.total_rounds = 0
    assert await code_of(scoring.StartQuiz(no_rounds)) == grpc.StatusCode.INVALID_ARGUMENT


async def test_record_round_errors(scoring) -> None:  # type: ignore[no-untyped-def]
    unknown = RecordRoundRequest(quiz_id="nope", round_number=1)
    assert await code_of(scoring.RecordRound(unknown)) == grpc.StatusCode.NOT_FOUND
    await scoring.StartQuiz(start_request())
    late = RecordRoundRequest(quiz_id="q1", round_number=6)
    assert await code_of(scoring.RecordRound(late)) == grpc.StatusCode.INVALID_ARGUMENT
    stranger = RecordRoundRequest(quiz_id="q1", round_number=1, answers=[answer("z", True, 0)])
    assert await code_of(scoring.RecordRound(stranger)) == grpc.StatusCode.INVALID_ARGUMENT
    await scoring.FinalizeQuiz(
        FinalizeQuizRequest(quiz_id="q1", rounds_played=0, end_reason=END_REASON_VOTED_TO_END)
    )
    over = RecordRoundRequest(quiz_id="q1", round_number=1)
    assert await code_of(scoring.RecordRound(over)) == grpc.StatusCode.FAILED_PRECONDITION


async def test_finalize_returns_the_summary_and_keeps_the_first_result(scoring, clock) -> None:  # type: ignore[no-untyped-def]
    await scoring.StartQuiz(start_request())
    await scoring.RecordRound(
        RecordRoundRequest(quiz_id="q1", round_number=1, answers=[answer("c", True, 5_000)])
    )
    clock.now = START + timedelta(minutes=4)
    response = await scoring.FinalizeQuiz(
        FinalizeQuizRequest(quiz_id="q1", rounds_played=1, end_reason=END_REASON_VOTED_TO_END)
    )
    summary = response.summary
    assert (summary.host_display_name, summary.seconds_per_question) == ("Maya", 20)
    assert (summary.total_rounds, summary.rounds_played) == (5, 1)
    assert summary.end_reason == END_REASON_VOTED_TO_END
    assert summary.finished_at.ToDatetime(UTC) - summary.started_at.ToDatetime(UTC) == timedelta(
        minutes=4
    )
    assert summary.final_scoreboard[0].user_id == "c"

    clock.now = START + timedelta(minutes=9)
    again = await scoring.FinalizeQuiz(
        FinalizeQuizRequest(quiz_id="q1", rounds_played=5, end_reason=END_REASON_COMPLETED)
    )
    assert again.summary == summary


async def test_finalize_errors(scoring) -> None:  # type: ignore[no-untyped-def]
    missing = FinalizeQuizRequest(quiz_id="nope", end_reason=END_REASON_COMPLETED)
    assert await code_of(scoring.FinalizeQuiz(missing)) == grpc.StatusCode.NOT_FOUND
    await scoring.StartQuiz(start_request())
    no_reason = FinalizeQuizRequest(quiz_id="q1", rounds_played=1)
    assert await code_of(scoring.FinalizeQuiz(no_reason)) == grpc.StatusCode.INVALID_ARGUMENT
    too_many = FinalizeQuizRequest(quiz_id="q1", rounds_played=6, end_reason=END_REASON_COMPLETED)
    assert await code_of(scoring.FinalizeQuiz(too_many)) == grpc.StatusCode.INVALID_ARGUMENT


async def test_only_a_finished_quiz_has_a_result(scoring) -> None:  # type: ignore[no-untyped-def]
    await scoring.StartQuiz(start_request())
    running = GetQuizResultRequest(quiz_id="q1")
    assert await code_of(scoring.GetQuizResult(running)) == grpc.StatusCode.NOT_FOUND
    await scoring.FinalizeQuiz(
        FinalizeQuizRequest(quiz_id="q1", rounds_played=5, end_reason=END_REASON_COMPLETED)
    )
    result = await scoring.GetQuizResult(running)
    assert result.summary.quiz_id == "q1"


async def play(
    scoring, clock: Clock, quiz_id: str, minutes: int, *players: tuple[str, str]
) -> None:  # type: ignore[no-untyped-def]
    clock.now = START + timedelta(minutes=minutes)
    await scoring.StartQuiz(start_request(quiz_id, *players))
    await scoring.FinalizeQuiz(
        FinalizeQuizRequest(quiz_id=quiz_id, rounds_played=5, end_reason=END_REASON_COMPLETED)
    )


async def test_history_pages_through_finished_quizzes_newest_first(scoring, clock) -> None:  # type: ignore[no-untyped-def]
    for n in range(5):
        await play(scoring, clock, f"q{n}", n, ("a", "Alice"), ("b", "Bob"))
    await play(scoring, clock, "other", 10, ("b", "Bob"), ("c", "Cara"))
    await scoring.StartQuiz(start_request("unfinished"))

    first = await scoring.GetPlayerHistory(GetPlayerHistoryRequest(user_id="a", page_size=2))
    assert [q.quiz_id for q in first.quizzes] == ["q4", "q3"]
    second = await scoring.GetPlayerHistory(
        GetPlayerHistoryRequest(user_id="a", page_size=2, page_token=first.next_page_token)
    )
    assert [q.quiz_id for q in second.quizzes] == ["q2", "q1"]
    last = await scoring.GetPlayerHistory(
        GetPlayerHistoryRequest(user_id="a", page_size=2, page_token=second.next_page_token)
    )
    assert [q.quiz_id for q in last.quizzes] == ["q0"]
    assert last.next_page_token == ""

    everything = await scoring.GetPlayerHistory(GetPlayerHistoryRequest(user_id="a"))
    assert len(everything.quizzes) == 5


async def test_history_page_size_and_token_rules(scoring, clock) -> None:  # type: ignore[no-untyped-def]
    for n in range(3):
        await play(scoring, clock, f"q{n}", n, ("a", "Alice"), ("b", "Bob"))
    huge = await scoring.GetPlayerHistory(GetPlayerHistoryRequest(user_id="a", page_size=500))
    assert len(huge.quizzes) == 3
    negative = GetPlayerHistoryRequest(user_id="a", page_size=-1)
    assert await code_of(scoring.GetPlayerHistory(negative)) == grpc.StatusCode.INVALID_ARGUMENT
    bad_token = GetPlayerHistoryRequest(user_id="a", page_token="not-a-token")
    assert await code_of(scoring.GetPlayerHistory(bad_token)) == grpc.StatusCode.INVALID_ARGUMENT


async def test_anonymize_keeps_scores_but_drops_the_name_and_link(scoring, clock) -> None:  # type: ignore[no-untyped-def]
    clock.now = START
    await scoring.StartQuiz(start_request("q1", ("a", "Alice"), ("b", "Bob")))
    await scoring.RecordRound(
        RecordRoundRequest(quiz_id="q1", round_number=1, answers=[answer("a", True, 0)])
    )
    await scoring.FinalizeQuiz(
        FinalizeQuizRequest(quiz_id="q1", rounds_played=1, end_reason=END_REASON_VOTED_TO_END)
    )

    await scoring.AnonymizePlayer(AnonymizePlayerRequest(user_id="a"))
    await scoring.AnonymizePlayer(AnonymizePlayerRequest(user_id="a"))  # safe to call again

    gone = await scoring.GetPlayerHistory(GetPlayerHistoryRequest(user_id="a"))
    assert list(gone.quizzes) == []
    [quiz] = (await scoring.GetPlayerHistory(GetPlayerHistoryRequest(user_id="b"))).quizzes
    top = quiz.final_scoreboard[0]
    assert (top.user_id, top.display_name, top.total_points) == ("", "Deleted player", 1000)
    await scoring.AnonymizePlayer(AnonymizePlayerRequest(user_id="never-played"))
