"""friendlyfire.quiz.v1.QuizService: every question for a quiz in one call."""

import secrets
from collections.abc import Awaitable, Callable
from typing import Protocol

import grpc
from friendlyfire.matchdata.v1.match_data_service_pb2 import (
    GetPlayerMatchStatsRequest,
    GetPlayerMatchStatsResponse,
)
from friendlyfire.quiz.v1 import quiz_service_pb2_grpc
from friendlyfire.quiz.v1.quiz_service_pb2 import GenerateQuizRequest, GenerateQuizResponse

from quiz_engine.question_generator import QuestionGenerator

MIN_QUESTIONS = 5
MAX_QUESTIONS = 20
# The request sets no `since`: questions use every stored match. Match Data keeps only
# the 30 days before each player's last sync, and Room's lobby check counts the same
# stored matches, so a time filter here would make the two disagree.
# Enough for 30 days of very heavy play; Match Data's default cap is 50.
MAX_MATCHES_PER_PLAYER = 300
MATCH_DATA_TIMEOUT_SECONDS = 5.0


class MatchDataClient(Protocol):
    """The part of MatchDataServiceStub the quiz uses."""

    def GetPlayerMatchStats(
        self, request: GetPlayerMatchStatsRequest, *, timeout: float | None = None
    ) -> Awaitable[GetPlayerMatchStatsResponse]: ...


def random_seed() -> int:
    return secrets.randbits(63)


def _invalid(request: GenerateQuizRequest) -> str:
    """Why the request can't be served, or "" if it can."""
    ids = [player.user_id for player in request.players]
    if len(ids) < 2:
        return "A quiz needs at least 2 players."
    if "" in ids or len(set(ids)) != len(ids):
        return "Every player needs a different, nonempty user_id."
    if not MIN_QUESTIONS <= request.question_count <= MAX_QUESTIONS:
        return f"question_count must be between {MIN_QUESTIONS} and {MAX_QUESTIONS}."
    return ""


def _not_enough(made: int, wanted: int) -> str:
    if made < MIN_QUESTIONS:
        return (
            f"There isn't enough match data for a quiz yet: only {made} different "
            f"questions could be made, and a quiz needs at least {MIN_QUESTIONS}. "
            "Play a few more matches and try again."
        )
    return (
        f"There's only enough match data for {made} different questions, not {wanted}. "
        f"Choose {made} or fewer questions."
    )


class QuizServicer(quiz_service_pb2_grpc.QuizServiceServicer):
    def __init__(
        self,
        match_data: MatchDataClient,
        generator: QuestionGenerator,
        *,
        seed: Callable[[], int] = random_seed,
    ) -> None:
        self._match_data = match_data
        self._generator = generator
        self._seed = seed

    async def GenerateQuiz(
        self,
        request: GenerateQuizRequest,
        context: grpc.aio.ServicerContext[GenerateQuizRequest, GenerateQuizResponse],
    ) -> GenerateQuizResponse:
        problem = _invalid(request)
        if problem:
            await context.abort(grpc.StatusCode.INVALID_ARGUMENT, problem)

        try:
            response = await self._match_data.GetPlayerMatchStats(
                GetPlayerMatchStatsRequest(
                    user_ids=[player.user_id for player in request.players],
                    max_matches_per_player=MAX_MATCHES_PER_PLAYER,
                ),
                timeout=MATCH_DATA_TIMEOUT_SECONDS,
            )
        except grpc.aio.AioRpcError as error:
            await context.abort(
                grpc.StatusCode.UNAVAILABLE,
                f"Couldn't get the players' match data ({error.code().name}). Try again soon.",
            )

        try:
            questions = self._generator.generate(
                request.players,
                response.stats,
                request.question_count,
                request.seed or self._seed(),
                asked_template_ids=request.asked_template_ids,
            )
        except ValueError as error:
            # The request was checked above, so this is bad data from Match Data.
            await context.abort(grpc.StatusCode.INTERNAL, f"Unusable match data: {error}")

        if len(questions) < request.question_count:
            await context.abort(
                grpc.StatusCode.FAILED_PRECONDITION,
                _not_enough(len(questions), request.question_count),
            )
        return GenerateQuizResponse(questions=questions)
