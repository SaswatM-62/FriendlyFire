"""friendlyfire.scoring.v1.ScoringService."""

import base64
import binascii
import json
from collections.abc import Callable
from datetime import datetime
from typing import cast

import grpc
from friendlyfire.scoring.v1 import scoring_service_pb2_grpc
from friendlyfire.scoring.v1.scoring_service_pb2 import (
    END_REASON_UNSPECIFIED,
    AnonymizePlayerRequest,
    AnonymizePlayerResponse,
    EndReason,
    FinalizeQuizRequest,
    FinalizeQuizResponse,
    GetPlayerHistoryRequest,
    GetPlayerHistoryResponse,
    GetQuizResultRequest,
    GetQuizResultResponse,
    QuizSummary,
    RecordRoundRequest,
    RecordRoundResponse,
    RoundPoints,
    ScoreboardEntry,
    StartQuizRequest,
    StartQuizResponse,
)
from google.protobuf.timestamp_pb2 import Timestamp

from scoring.quiz_repository import QuizRecord, Quizzes, RoundResult, Seat, Total
from scoring.score_calculator import ScoreCalculator

DEFAULT_PAGE_SIZE = 20
MAX_PAGE_SIZE = 50


class ScoringServicer(scoring_service_pb2_grpc.ScoringServiceServicer):
    def __init__(
        self, quizzes: Quizzes, calculator: ScoreCalculator, now: Callable[[], datetime]
    ) -> None:
        self._quizzes = quizzes
        self._calculator = calculator
        self._now = now

    async def StartQuiz(
        self,
        request: StartQuizRequest,
        context: grpc.aio.ServicerContext[StartQuizRequest, StartQuizResponse],
    ) -> StartQuizResponse:
        user_ids = [p.user_id for p in request.players]
        if not request.quiz_id or not request.room_code:
            await _invalid(context, "quiz_id and room_code are required")
        if request.total_rounds < 1 or request.seconds_per_question < 1:
            await _invalid(context, "total_rounds and seconds_per_question must be positive")
        if not user_ids or "" in user_ids or len(set(user_ids)) != len(user_ids):
            await _invalid(context, "players need unique, nonempty user IDs")
        quiz = QuizRecord(
            quiz_id=request.quiz_id,
            room_code=request.room_code,
            host_display_name=request.host_display_name,
            total_rounds=request.total_rounds,
            seconds_per_question=request.seconds_per_question,
            started_at=self._now(),
            players=tuple(
                Seat(seat, p.user_id, p.display_name) for seat, p in enumerate(request.players)
            ),
        )
        if not await self._quizzes.create(quiz):
            await context.abort(grpc.StatusCode.ALREADY_EXISTS, "That quiz is already registered.")
        return StartQuizResponse()

    async def RecordRound(
        self,
        request: RecordRoundRequest,
        context: grpc.aio.ServicerContext[RecordRoundRequest, RecordRoundResponse],
    ) -> RecordRoundResponse:
        quiz = await self._quiz(request.quiz_id, context)
        if quiz.finished_at is not None:
            await context.abort(grpc.StatusCode.FAILED_PRECONDITION, "That quiz is over.")
        if not 1 <= request.round_number <= quiz.total_rounds:
            await _invalid(context, f"round_number must be 1 to {quiz.total_rounds}")
        seats = {p.user_id: p.seat for p in quiz.players if p.user_id is not None}
        answers = {a.user_id: a for a in request.answers}
        if unknown := set(answers) - set(seats):
            await _invalid(context, f"{len(unknown)} answer(s) from players not in this quiz")

        limit_ms = quiz.seconds_per_question * 1000
        results = []
        for player in quiz.players:
            answer = answers.get(player.user_id) if player.user_id else None
            answered = answer is not None and answer.answered
            correct = answered and answer is not None and answer.correct
            time_ms = answer.response_time_ms if answered and answer is not None else 0
            points = (
                self._calculator.calculate_points(True, limit_ms - time_ms, limit_ms)
                if correct
                else 0
            )
            results.append(RoundResult(player.seat, answered, correct, time_ms, points))
        await self._quizzes.replace_round(quiz.quiz_id, request.round_number, results)

        updated = await self._quiz(quiz.quiz_id, context)
        return RecordRoundResponse(
            round_points=[
                RoundPoints(user_id=p.user_id or "", points=r.points)
                for p, r in zip(quiz.players, results, strict=True)
            ],
            scoreboard=self._scoreboard(updated),
        )

    async def FinalizeQuiz(
        self,
        request: FinalizeQuizRequest,
        context: grpc.aio.ServicerContext[FinalizeQuizRequest, FinalizeQuizResponse],
    ) -> FinalizeQuizResponse:
        quiz = await self._quiz(request.quiz_id, context)
        if (
            request.end_reason == END_REASON_UNSPECIFIED
            or request.end_reason not in EndReason.values()
        ):
            await _invalid(context, "end_reason is required")
        if not 0 <= request.rounds_played <= quiz.total_rounds:
            await _invalid(context, f"rounds_played must be 0 to {quiz.total_rounds}")
        # Safe to call again: the first call's result stays.
        await self._quizzes.finish(
            quiz.quiz_id, request.rounds_played, EndReason.Name(request.end_reason), self._now()
        )
        finished = await self._quiz(quiz.quiz_id, context)
        return FinalizeQuizResponse(summary=self._summary(finished))

    async def GetQuizResult(
        self,
        request: GetQuizResultRequest,
        context: grpc.aio.ServicerContext[GetQuizResultRequest, GetQuizResultResponse],
    ) -> GetQuizResultResponse:
        quiz = await self._quizzes.get(request.quiz_id) if request.quiz_id else None
        if quiz is None or quiz.finished_at is None:
            await context.abort(grpc.StatusCode.NOT_FOUND, "Quiz not found.")
        assert quiz is not None
        return GetQuizResultResponse(summary=self._summary(quiz))

    async def GetPlayerHistory(
        self,
        request: GetPlayerHistoryRequest,
        context: grpc.aio.ServicerContext[GetPlayerHistoryRequest, GetPlayerHistoryResponse],
    ) -> GetPlayerHistoryResponse:
        if request.page_size < 0:
            await _invalid(context, "page_size can't be negative")
        size = min(request.page_size or DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE)
        after = None
        if request.page_token:
            after = _decode_token(request.page_token)
            if after is None:
                await _invalid(context, "page_token isn't valid")
        if not request.user_id:
            return GetPlayerHistoryResponse()
        quizzes = await self._quizzes.history(request.user_id, size + 1, after)
        page = quizzes[:size]
        next_token = ""
        if len(quizzes) > size:
            last = page[-1]
            assert last.finished_at is not None
            next_token = _encode_token(last.finished_at, last.quiz_id)
        return GetPlayerHistoryResponse(
            quizzes=[self._summary(q) for q in page], next_page_token=next_token
        )

    async def AnonymizePlayer(
        self,
        request: AnonymizePlayerRequest,
        context: grpc.aio.ServicerContext[AnonymizePlayerRequest, AnonymizePlayerResponse],
    ) -> AnonymizePlayerResponse:
        if not request.user_id:
            await _invalid(context, "user_id is required")
        await self._quizzes.anonymize(request.user_id)
        return AnonymizePlayerResponse()

    async def _quiz(self, quiz_id: str, context: grpc.aio.ServicerContext) -> QuizRecord:  # type: ignore[type-arg]
        quiz = await self._quizzes.get(quiz_id) if quiz_id else None
        if quiz is None:
            await context.abort(grpc.StatusCode.NOT_FOUND, "Quiz not found.")
        assert quiz is not None
        return quiz

    def _scoreboard(self, quiz: QuizRecord) -> list[ScoreboardEntry]:
        """Every player, best first; equal points share a rank (1, 1, 3)."""
        totals = {p.seat: quiz.totals.get(p.seat, Total()) for p in quiz.players}
        ranks = self._calculator.calculate_ranks({seat: t.points for seat, t in totals.items()})
        ordered = sorted(quiz.players, key=lambda p: (ranks[p.seat], p.seat))
        return [
            ScoreboardEntry(
                user_id=p.user_id or "",
                display_name=p.display_name,
                total_points=totals[p.seat].points,
                correct_answers=totals[p.seat].correct_answers,
                rank=ranks[p.seat],
            )
            for p in ordered
        ]

    def _summary(self, quiz: QuizRecord) -> QuizSummary:
        summary = QuizSummary(
            quiz_id=quiz.quiz_id,
            started_at=_timestamp(quiz.started_at),
            total_rounds=quiz.total_rounds,
            rounds_played=quiz.rounds_played or 0,
            end_reason=_end_reason(quiz.end_reason),
            final_scoreboard=self._scoreboard(quiz),
            host_display_name=quiz.host_display_name,
            seconds_per_question=quiz.seconds_per_question,
        )
        if quiz.finished_at is not None:
            summary.finished_at.CopyFrom(_timestamp(quiz.finished_at))
        return summary


async def _invalid(context: grpc.aio.ServicerContext, message: str) -> None:  # type: ignore[type-arg]
    await context.abort(grpc.StatusCode.INVALID_ARGUMENT, message)


def _end_reason(name: str | None) -> EndReason:
    # The generated stubs type Value() as int; it's always an EndReason here.
    return cast(EndReason, EndReason.Value(name)) if name else END_REASON_UNSPECIFIED


def _timestamp(moment: datetime) -> Timestamp:
    stamp = Timestamp()
    stamp.FromDatetime(moment)
    return stamp


def _encode_token(finished_at: datetime, quiz_id: str) -> str:
    raw = json.dumps([finished_at.isoformat(), quiz_id]).encode()
    return base64.urlsafe_b64encode(raw).decode().rstrip("=")


def _decode_token(token: str) -> tuple[datetime, str] | None:
    """The (finished_at, quiz_id) a page ended at, or None if the token isn't one of ours."""
    try:
        raw = base64.urlsafe_b64decode(token + "=" * (-len(token) % 4))
        moment, quiz_id = json.loads(raw)
        parsed = datetime.fromisoformat(moment)
    except (binascii.Error, ValueError, TypeError):
        return None
    if not isinstance(quiz_id, str) or parsed.tzinfo is None:
        return None
    return parsed, quiz_id
