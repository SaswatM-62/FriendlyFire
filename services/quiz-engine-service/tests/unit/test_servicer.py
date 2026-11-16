"""QuizService.GenerateQuiz over gRPC, with a fake Match Data client."""

from collections.abc import AsyncIterator, Awaitable, Callable
from dataclasses import replace

import grpc
import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1 import quiz_service_pb2_grpc
from friendlyfire.quiz.v1.quiz_service_pb2 import (
    GenerateQuizRequest,
    GenerateQuizResponse,
    QuizPlayer,
)

from quiz_engine.bootstrap import Overrides, Service, build_server
from quiz_engine.question_generator import QuestionGenerator
from quiz_engine.servicer import MATCH_DATA_TIMEOUT_SECONDS, MAX_MATCHES_PER_PLAYER
from quiz_engine.settings import Settings
from quiz_engine.templates import TEMPLATES
from tests.fakes import FakeCheck, FakeMatchData

PLAYERS = [
    QuizPlayer(user_id="a", display_name="Alice"),
    QuizPlayer(user_id="b", display_name="Bob"),
]
KILLS = next(t for t in TEMPLATES if t.id == "most-kills-v1")


def games(count: int) -> tuple[QuestionGenerator, list[PlayerMatchStats]]:
    """A generator with one most-kills template per game, and stats Alice wins in each."""
    templates, stats = [], []
    for n in range(count):
        game = f"game-{n}"
        templates.append(replace(KILLS, id=f"kills-{n}", game=game, scopes=("all",)))
        for uid, kills in (("a", 20 + n), ("b", 10)):
            row = PlayerMatchStats(
                user_id=uid,
                match_id=f"{game}-{uid}",
                game=game,
                kills=kills,
                labels={"queue_id": "competitive"},
            )
            stats.append(row)
    return QuestionGenerator(templates), stats


Serve = Callable[..., Awaitable[quiz_service_pb2_grpc.QuizServiceStub]]


@pytest.fixture
async def serve() -> AsyncIterator[Serve]:
    """Starts the real server with the given fakes; returns a QuizService client."""
    services: list[Service] = []
    channels: list[grpc.aio.Channel] = []

    async def start(
        match_data: FakeMatchData,
        generator: QuestionGenerator,
        seed: Callable[[], int] = lambda: 7,
    ) -> quiz_service_pb2_grpc.QuizServiceStub:
        service = await build_server(
            Settings(grpc_port=0),
            Overrides(
                dependency_checks=[FakeCheck.up("match-data-service")],
                match_data=match_data,
                generator=generator,
                seed=seed,
            ),
        )
        await service.start()
        services.append(service)
        channel = grpc.aio.insecure_channel(f"localhost:{service.port}")
        channels.append(channel)
        return quiz_service_pb2_grpc.QuizServiceStub(channel)

    yield start
    for channel in channels:
        await channel.close()
    for service in services:
        await service.stop(grace=None)


def request(count: int = 5, seed: int = 42, **fields: object) -> GenerateQuizRequest:
    return GenerateQuizRequest(players=PLAYERS, question_count=count, seed=seed, **fields)


async def test_makes_every_question_from_every_stored_match(serve: Serve) -> None:
    generator, stats = games(6)
    match_data = FakeMatchData(stats)
    quiz = await serve(match_data, generator)

    response: GenerateQuizResponse = await quiz.GenerateQuiz(request())

    assert len(response.questions) == 5
    assert len({q.template_id for q in response.questions}) == 5
    assert all(q.correct_option_id == "a" for q in response.questions)
    [sent] = match_data.requests
    assert list(sent.user_ids) == ["a", "b"]
    # No time filter: Room's lobby check counts the same stored matches.
    assert not sent.HasField("since")
    assert sent.max_matches_per_player == MAX_MATCHES_PER_PLAYER
    assert match_data.timeouts == [MATCH_DATA_TIMEOUT_SECONDS]


async def test_same_seed_gives_the_same_quiz_and_seed_zero_picks_one(serve: Serve) -> None:
    generator, stats = games(8)
    picked: list[int] = []

    def seed() -> int:
        picked.append(7)
        return 7

    quiz = await serve(FakeMatchData(stats), generator, seed)

    first = await quiz.GenerateQuiz(request(seed=42))
    again = await quiz.GenerateQuiz(request(seed=42))
    random = await quiz.GenerateQuiz(request(seed=0))

    assert first == again
    assert picked == [7]
    assert random == await quiz.GenerateQuiz(request(seed=7))


async def test_templates_the_room_already_asked_come_last(serve: Serve) -> None:
    generator, stats = games(10)
    quiz = await serve(FakeMatchData(stats), generator)

    first = await quiz.GenerateQuiz(request())
    asked = [q.template_id for q in first.questions]
    second = await quiz.GenerateQuiz(request(asked_template_ids=asked))

    assert not {q.template_id for q in second.questions} & set(asked)


@pytest.mark.parametrize(
    ("players", "count"),
    [
        (PLAYERS[:1], 5),
        ([PLAYERS[0], PLAYERS[0]], 5),
        ([PLAYERS[0], QuizPlayer(display_name="No ID")], 5),
        (PLAYERS, 4),
        (PLAYERS, 21),
    ],
)
async def test_bad_requests_are_rejected_before_fetching_data(
    serve: Serve, players: list[QuizPlayer], count: int
) -> None:
    generator, stats = games(6)
    match_data = FakeMatchData(stats)
    quiz = await serve(match_data, generator)

    with pytest.raises(grpc.aio.AioRpcError) as error:
        await quiz.GenerateQuiz(GenerateQuizRequest(players=players, question_count=count))

    assert error.value.code() == grpc.StatusCode.INVALID_ARGUMENT
    assert match_data.requests == []


async def test_too_little_data_for_any_quiz_says_how_many_could_be_made(serve: Serve) -> None:
    generator, stats = games(3)
    quiz = await serve(FakeMatchData(stats), generator)

    with pytest.raises(grpc.aio.AioRpcError) as error:
        await quiz.GenerateQuiz(request(count=5))

    assert error.value.code() == grpc.StatusCode.FAILED_PRECONDITION
    assert "only 3 different questions" in error.value.details()


async def test_too_few_questions_for_this_length_suggests_a_shorter_quiz(serve: Serve) -> None:
    generator, stats = games(6)
    quiz = await serve(FakeMatchData(stats), generator)

    with pytest.raises(grpc.aio.AioRpcError) as error:
        await quiz.GenerateQuiz(request(count=8))

    assert error.value.code() == grpc.StatusCode.FAILED_PRECONDITION
    assert "only enough match data for 6 different questions, not 8" in error.value.details()


@pytest.mark.parametrize(
    "failure",
    [grpc.StatusCode.UNAVAILABLE, grpc.StatusCode.DEADLINE_EXCEEDED, grpc.StatusCode.UNIMPLEMENTED],
)
async def test_match_data_failure_is_unavailable(serve: Serve, failure: grpc.StatusCode) -> None:
    generator, _ = games(6)
    quiz = await serve(FakeMatchData(error=failure), generator)

    with pytest.raises(grpc.aio.AioRpcError) as error:
        await quiz.GenerateQuiz(request())

    assert error.value.code() == grpc.StatusCode.UNAVAILABLE
    assert failure.name in error.value.details()


async def test_conflicting_match_data_is_an_internal_error(serve: Serve) -> None:
    generator, stats = games(6)
    conflict = PlayerMatchStats()
    conflict.CopyFrom(stats[0])
    conflict.kills += 1
    quiz = await serve(FakeMatchData([*stats, conflict]), generator)

    with pytest.raises(grpc.aio.AioRpcError) as error:
        await quiz.GenerateQuiz(request())

    assert error.value.code() == grpc.StatusCode.INTERNAL
    assert "Conflicting records" in error.value.details()
