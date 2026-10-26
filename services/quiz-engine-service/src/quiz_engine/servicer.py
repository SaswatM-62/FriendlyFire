"""friendlyfire.quiz.v1.QuizService. Every RPC is a stub for now."""

import grpc
from friendlyfire.matchdata.v1 import match_data_service_pb2_grpc
from friendlyfire.quiz.v1 import quiz_service_pb2_grpc
from friendlyfire.quiz.v1.quiz_service_pb2 import GenerateQuizRequest, GenerateQuizResponse


class QuizServicer(quiz_service_pb2_grpc.QuizServiceServicer):
    def __init__(self, match_data: match_data_service_pb2_grpc.MatchDataServiceStub) -> None:
        self._match_data = match_data

    async def GenerateQuiz(
        self,
        request: GenerateQuizRequest,
        context: grpc.aio.ServicerContext[GenerateQuizRequest, GenerateQuizResponse],
    ) -> GenerateQuizResponse:
        # TODO(ff): build every question for a quiz in one call
        #   - fetch the players' stats (MatchDataService.GetPlayerMatchStats), using both
        #     matches they played together and each player's solo matches
        #   - make question_count different PICK_THE_PLAYER questions (QuestionGenerator);
        #     the same seed and data give the same questions; seed 0 = pick a random one
        #   - errors: INVALID_ARGUMENT if fewer than 2 players or question_count is outside
        #     5–20; FAILED_PRECONDITION if there isn't enough data (say how many could be made)
        #   - uses: self._match_data, QuestionGenerator
        await context.abort(grpc.StatusCode.UNIMPLEMENTED, "not implemented yet")
