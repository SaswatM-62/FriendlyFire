"""DependencyHealthService: "am I running AND can I reach everything I call?"

Every dependency is checked in parallel, each with the same deadline. A failed or slow
check becomes a DOWN node with a short error; nothing here raises for an unhealthy
dependency.
"""

import asyncio
import time
from collections.abc import Sequence
from typing import Protocol

import grpc
from friendlyfire.health.v1 import dependency_health_pb2_grpc
from friendlyfire.health.v1.dependency_health_pb2 import (
    COMPONENT_STATUS_DEGRADED,
    COMPONENT_STATUS_DOWN,
    COMPONENT_STATUS_UP,
    CheckDependenciesRequest,
    CheckDependenciesResponse,
    ComponentHealth,
)
from psycopg_pool import AsyncConnectionPool, PoolTimeout

DEADLINE_SECONDS = 1.0
# Part of the caller's deadline kept for sending the answer back.
REPLY_MARGIN_SECONDS = 0.2
MIN_DEADLINE_SECONDS = 0.1
_MAX_ERROR_LENGTH = 120
# What to say for the usual "can't connect" details from gRPC.
_UNAVAILABLE_REASONS = [
    ("connection refused", "connection refused"),
    ("no route to host", "no route to host"),
    ("timed out", "connection timed out"),
    ("dns", "unknown host"),
    ("name or service not known", "unknown host"),
    ("domain name not found", "unknown host"),
]


class DependencyCheck(Protocol):
    """One thing this service depends on: a gRPC peer, Postgres or an HTTP API."""

    @property
    def name(self) -> str:
        """The node's name in the health tree, e.g. "postgres"."""
        ...

    async def check(self, deadline: float) -> ComponentHealth:
        """Returns the dependency's node (just a status for a leaf, or the tree a gRPC
        peer reported), or raises if it can't be reached. The checker fills in the name
        and latency and turns exceptions into DOWN nodes."""
        ...


class DependencyHealthChecker:
    """Checks every dependency in parallel and builds this service's node."""

    def __init__(
        self,
        service_name: str,
        checks: Sequence[DependencyCheck],
        deadline: float = DEADLINE_SECONDS,
    ) -> None:
        self._service_name = service_name
        self._checks = list(checks)
        self._deadline = deadline

    async def check(self, caller_time_left: float | None = None) -> ComponentHealth:
        """This service's node, with one child per dependency (in order).

        When the caller only waits caller_time_left seconds, each dependency gets that
        minus a margin for the reply, so a slow dependency deep in the tree shows up as
        DOWN where it is instead of every service above it timing out too.
        """
        deadline = self._deadline
        if caller_time_left is not None:
            budget = max(caller_time_left - REPLY_MARGIN_SECONDS, MIN_DEADLINE_SECONDS)
            deadline = min(deadline, budget)
        nodes = await asyncio.gather(*(self._run(check, deadline) for check in self._checks))
        all_up = all(node.status == COMPONENT_STATUS_UP for node in nodes)
        return ComponentHealth(
            name=self._service_name,
            status=COMPONENT_STATUS_UP if all_up else COMPONENT_STATUS_DEGRADED,
            dependencies=nodes,
        )

    async def _run(self, check: DependencyCheck, deadline: float) -> ComponentHealth:
        start = time.monotonic()
        try:
            async with asyncio.timeout(deadline):
                node = await check.check(deadline)
        except TimeoutError:
            return _down(check.name, start, "deadline exceeded")
        except Exception as error:
            return _down(check.name, start, describe(error))
        node.name = check.name
        node.latency_ms = _millis_since(start)
        return node


class GrpcDependencyCheck:
    """Calls a gRPC peer's CheckDependencies and returns the tree it reports."""

    def __init__(
        self, name: str, health: dependency_health_pb2_grpc.DependencyHealthServiceStub
    ) -> None:
        self._name = name
        self._health = health

    @property
    def name(self) -> str:
        return self._name

    async def check(self, deadline: float) -> ComponentHealth:
        response: CheckDependenciesResponse = await self._health.CheckDependencies(
            CheckDependenciesRequest(), timeout=deadline
        )
        return response.component


class PostgresDependencyCheck:
    """Borrows a connection from the pool and runs SELECT 1."""

    def __init__(self, pool: AsyncConnectionPool) -> None:
        self._pool = pool

    @property
    def name(self) -> str:
        return "postgres"

    async def check(self, deadline: float) -> ComponentHealth:
        # Give up a bit before the deadline, so the error says why ("couldn't get a
        # connection") instead of only "deadline exceeded".
        async with self._pool.connection(timeout=deadline * 0.75) as connection:
            await connection.execute("SELECT 1")
        return up()


class DependencyHealthServicer(dependency_health_pb2_grpc.DependencyHealthServiceServicer):
    """friendlyfire.health.v1.DependencyHealthService."""

    def __init__(self, checker: DependencyHealthChecker) -> None:
        self._checker = checker

    async def CheckDependencies(
        self,
        request: CheckDependenciesRequest,
        context: grpc.aio.ServicerContext[CheckDependenciesRequest, CheckDependenciesResponse],
    ) -> CheckDependenciesResponse:
        # Answer before the caller's own deadline (e.g. room-service waits 1 second).
        return CheckDependenciesResponse(
            component=await self._checker.check(context.time_remaining())
        )


def up() -> ComponentHealth:
    """The node for a leaf dependency that answered."""
    return ComponentHealth(status=COMPONENT_STATUS_UP)


def describe(error: BaseException) -> str:
    """The short error shown in the tree, e.g. "connection refused"."""
    if isinstance(error, grpc.aio.AioRpcError):
        code = error.code()
        details = error.details() or ""
        if code == grpc.StatusCode.DEADLINE_EXCEEDED:
            return "deadline exceeded"
        if code == grpc.StatusCode.UNAVAILABLE:
            lowered = details.lower()
            for needle, reason in _UNAVAILABLE_REASONS:
                if needle in lowered:
                    return reason
            return _shorten(f"unavailable: {details}" if details else "unavailable")
        return code.name.lower().replace("_", " ")
    if isinstance(error, ConnectionRefusedError):
        return "connection refused"
    if isinstance(error, PoolTimeout):
        return "no database connection"
    return _shorten(str(error) or type(error).__name__)


def _down(name: str, start: float, error: str) -> ComponentHealth:
    return ComponentHealth(
        name=name, status=COMPONENT_STATUS_DOWN, latency_ms=_millis_since(start), error=error
    )


def _millis_since(start: float) -> int:
    return int((time.monotonic() - start) * 1000)


def _shorten(message: str) -> str:
    if len(message) <= _MAX_ERROR_LENGTH:
        return message
    return message[: _MAX_ERROR_LENGTH - 3] + "..."
