"""friendlyfire.scoring.v1.ScoringService. Every RPC is a stub for now."""

import grpc
from friendlyfire.scoring.v1 import scoring_service_pb2_grpc
from friendlyfire.scoring.v1.scoring_service_pb2 import (
    FinalizeQuizRequest,
    FinalizeQuizResponse,
    GetPlayerHistoryRequest,
    GetPlayerHistoryResponse,
    GetQuizResultRequest,
    GetQuizResultResponse,
    RecordRoundRequest,
    RecordRoundResponse,
    StartQuizRequest,
    StartQuizResponse,
)

from scoring.quiz_repository import QuizRepository
from scoring.score_calculator import ScoreCalculator


class ScoringServicer(scoring_service_pb2_grpc.ScoringServiceServicer):
    def __init__(self, quizzes: QuizRepository, calculator: ScoreCalculator) -> None:
        self._quizzes = quizzes
        self._calculator = calculator

    async def StartQuiz(
        self,
        request: StartQuizRequest,
        context: grpc.aio.ServicerContext[StartQuizRequest, StartQuizResponse],
    ) -> StartQuizResponse:
        # TODO(ff): register a quiz before its first question
        #   - save quiz_id, room_code, total_rounds, seconds_per_question and the players
        #     with the display names they used (the history page shows those)
        #   - errors: ALREADY_EXISTS if quiz_id was already registered
        #   - uses: self._quizzes
        await context.abort(grpc.StatusCode.UNIMPLEMENTED, "not implemented yet")

    async def RecordRound(
        self,
        request: RecordRoundRequest,
        context: grpc.aio.ServicerContext[RecordRoundRequest, RecordRoundResponse],
    ) -> RecordRoundResponse:
        # TODO(ff): score one finished round and return the updated scoreboard
        #   - called once per round with every player's answer; safe to retry: the same
        #     quiz_id + round_number replaces the earlier result instead of adding to it
        #   - correct = 500 + 500 * (time left / time limit), rounded; wrong or no answer
        #     = 0; players with equal points share a rank
        #   - errors: none in the contract
        #   - uses: self._quizzes, self._calculator
        await context.abort(grpc.StatusCode.UNIMPLEMENTED, "not implemented yet")

    async def FinalizeQuiz(
        self,
        request: FinalizeQuizRequest,
        context: grpc.aio.ServicerContext[FinalizeQuizRequest, FinalizeQuizResponse],
    ) -> FinalizeQuizResponse:
        # TODO(ff): mark a quiz as over and return the final standings
        #   - store rounds_played, end_reason and finished_at; return the QuizSummary
        #   - errors: none in the contract
        #   - uses: self._quizzes
        await context.abort(grpc.StatusCode.UNIMPLEMENTED, "not implemented yet")

    async def GetQuizResult(
        self,
        request: GetQuizResultRequest,
        context: grpc.aio.ServicerContext[GetQuizResultRequest, GetQuizResultResponse],
    ) -> GetQuizResultResponse:
        # TODO(ff): return one finished quiz with its final scoreboard
        #   - errors: NOT_FOUND if the quiz doesn't exist
        #   - uses: self._quizzes
        await context.abort(grpc.StatusCode.UNIMPLEMENTED, "not implemented yet")

    async def GetPlayerHistory(
        self,
        request: GetPlayerHistoryRequest,
        context: grpc.aio.ServicerContext[GetPlayerHistoryRequest, GetPlayerHistoryResponse],
    ) -> GetPlayerHistoryResponse:
        # TODO(ff): a player's past quizzes, newest first, one page at a time
        #   - page_size 0 means 20, maximum 50; page_token is the last call's
        #     next_page_token; next_page_token is empty on the last page
        #   - errors: none in the contract
        #   - uses: self._quizzes
        await context.abort(grpc.StatusCode.UNIMPLEMENTED, "not implemented yet")
