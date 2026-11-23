"""Fakes for our own interfaces. Prefer these over mocking libraries."""

import asyncio
from collections.abc import Awaitable, Callable, Sequence

import grpc
from friendlyfire.health.v1.dependency_health_pb2 import ComponentHealth
from friendlyfire.matchdata.v1.match_data_service_pb2 import (
    GetPlayerMatchStatsRequest,
    GetPlayerMatchStatsResponse,
    PlayerMatchStats,
)

from quiz_engine.dependency_health import up


class FakeMatchData:
    """MatchDataService's GetPlayerMatchStats: fixed stats, or a fixed gRPC error."""

    def __init__(
        self,
        stats: Sequence[PlayerMatchStats] = (),
        error: grpc.StatusCode | None = None,
    ) -> None:
        self.stats = list(stats)
        self.error = error
        self.requests: list[GetPlayerMatchStatsRequest] = []
        self.timeouts: list[float | None] = []

    def GetPlayerMatchStats(
        self, request: GetPlayerMatchStatsRequest, *, timeout: float | None = None
    ) -> Awaitable[GetPlayerMatchStatsResponse]:
        """Like the real stub: records the call and returns something to await."""
        self.requests.append(request)
        self.timeouts.append(timeout)
        return self._answer(request)

    async def _answer(self, request: GetPlayerMatchStatsRequest) -> GetPlayerMatchStatsResponse:
        if self.error is not None:
            raise grpc.aio.AioRpcError(
                self.error, grpc.aio.Metadata(), grpc.aio.Metadata(), details="fake failure"
            )
        wanted = set(request.user_ids)
        return GetPlayerMatchStatsResponse(stats=[s for s in self.stats if s.user_id in wanted])


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
