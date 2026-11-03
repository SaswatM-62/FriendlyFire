"""Fakes for our own interfaces. Prefer these over mocking libraries."""

import asyncio
from collections.abc import Awaitable, Callable
from contextlib import AbstractContextManager, nullcontext

from friendlyfire.health.v1.dependency_health_pb2 import ComponentHealth

from scoring.dependency_health import up


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
