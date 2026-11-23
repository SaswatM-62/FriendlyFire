"""Fakes for our own interfaces. Prefer these over mocking libraries."""

import asyncio
from collections.abc import Awaitable, Callable, Sequence
from contextlib import AbstractContextManager, nullcontext
from dataclasses import replace
from datetime import datetime

from friendlyfire.health.v1.dependency_health_pb2 import ComponentHealth

from scoring.dependency_health import up
from scoring.quiz_repository import DELETED_PLAYER, QuizRecord, RoundResult, Seat, Total


class FakeCheck:
    """A dependency check with a fixed outcome."""

    def __init__(self, name: str, outcome: Callable[[], Awaitable[ComponentHealth]]) -> None:
        self._name = name
        self._outcome = outcome
        self.last_deadline: float | None = None

    @property
    def name(self) -> str:
        return self._name

    async def check(self, deadline: float) -> ComponentHealth:
        self.last_deadline = deadline
        return await self._outcome()

    @classmethod
    def up(cls, name: str) -> "FakeCheck":
        async def answer() -> ComponentHealth:
            return up()

        return cls(name, answer)

    @classmethod
    def failing(cls, name: str, error: Exception) -> "FakeCheck":
        async def fail() -> ComponentHealth:
            raise error

        return cls(name, fail)

    @classmethod
    def never_answers(cls, name: str) -> "FakeCheck":
        async def hang() -> ComponentHealth:
            await asyncio.Event().wait()
            return up()

        return cls(name, hang)


class FlakyPostgres:
    """An engine whose connect() raises the given errors one by one, then works."""

    def __init__(self, *errors: Exception) -> None:
        self._errors = list(errors)
        self.connects = 0

    def connect(self) -> AbstractContextManager[object]:
        self.connects += 1
        if self._errors:
            raise self._errors.pop(0)
        return nullcontext()


class FakeQuizzes:
    """Quizzes in memory, behaving like QuizRepository."""

    def __init__(self) -> None:
        self.quizzes: dict[str, QuizRecord] = {}
        self.rounds: dict[tuple[str, int], list[RoundResult]] = {}

    async def create(self, quiz: QuizRecord) -> bool:
        if quiz.quiz_id in self.quizzes:
            return False
        self.quizzes[quiz.quiz_id] = quiz
        return True

    async def get(self, quiz_id: str) -> QuizRecord | None:
        quiz = self.quizzes.get(quiz_id)
        if quiz is None:
            return None
        totals: dict[int, Total] = {}
        for (rounds_quiz, _), results in self.rounds.items():
            if rounds_quiz != quiz_id:
                continue
            for r in results:
                t = totals.get(r.seat, Total())
                totals[r.seat] = Total(t.points + r.points, t.correct_answers + int(r.correct))
        return replace(quiz, totals=totals)

    async def replace_round(
        self, quiz_id: str, round_number: int, results: Sequence[RoundResult]
    ) -> None:
        self.rounds[quiz_id, round_number] = list(results)

    async def finish(
        self, quiz_id: str, rounds_played: int, end_reason: str, finished_at: datetime
    ) -> None:
        quiz = self.quizzes[quiz_id]
        if quiz.finished_at is None:
            self.quizzes[quiz_id] = replace(
                quiz, finished_at=finished_at, rounds_played=rounds_played, end_reason=end_reason
            )

    async def history(
        self, user_id: str, limit: int, after: tuple[datetime, str] | None
    ) -> list[QuizRecord]:
        mine = [
            q
            for q in self.quizzes.values()
            if q.finished_at is not None and any(p.user_id == user_id for p in q.players)
        ]
        mine.sort(key=lambda q: (q.finished_at, q.quiz_id), reverse=True)
        if after is not None:
            mine = [q for q in mine if (q.finished_at, q.quiz_id) < after]
        return [r for q in mine[:limit] if (r := await self.get(q.quiz_id)) is not None]

    async def anonymize(self, user_id: str) -> None:
        for quiz_id, quiz in self.quizzes.items():
            players = tuple(
                Seat(p.seat, None, DELETED_PLAYER) if p.user_id == user_id else p
                for p in quiz.players
            )
            self.quizzes[quiz_id] = replace(quiz, players=players)
