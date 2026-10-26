"""The only place objects are wired together."""

from collections.abc import Awaitable, Callable, Sequence
from dataclasses import dataclass, field

import grpc
from friendlyfire.health.v1 import dependency_health_pb2, dependency_health_pb2_grpc
from friendlyfire.matchdata.v1 import match_data_service_pb2_grpc
from friendlyfire.quiz.v1 import quiz_service_pb2, quiz_service_pb2_grpc
from grpc_health.v1 import health, health_pb2, health_pb2_grpc
from grpc_reflection.v1alpha import reflection

from quiz_engine.dependency_health import (
    DependencyCheck,
    DependencyHealthChecker,
    DependencyHealthServicer,
    GrpcDependencyCheck,
)
from quiz_engine.servicer import QuizServicer
from quiz_engine.settings import Settings

SERVICE_NAME = "quiz-engine-service"

# Reconnect to a restarted peer within seconds, not gRPC's default of up to 2 minutes.
CHANNEL_OPTIONS = [
    ("grpc.initial_reconnect_backoff_ms", 500),
    ("grpc.max_reconnect_backoff_ms", 5000),
]


@dataclass
class Overrides:
    """Replacements for tests. Anything left as None is built from the settings."""

    match_data_channel: grpc.aio.Channel | None = None
    dependency_checks: Sequence[DependencyCheck] | None = None


@dataclass
class Service:
    """The wired-up gRPC server and what must be closed when it stops."""

    server: grpc.aio.Server
    port: int
    _closers: list[Callable[[], Awaitable[None]]] = field(default_factory=list)

    async def start(self) -> None:
        await self.server.start()

    async def stop(self, grace: float | None = 5.0) -> None:
        await self.server.stop(grace)
        for close in self._closers:
            await close()


async def build_server(settings: Settings, overrides: Overrides | None = None) -> Service:
    overrides = overrides or Overrides()
    closers: list[Callable[[], Awaitable[None]]] = []

    # One channel per dependency: the feature stub and the health stub share it.
    match_data_channel = overrides.match_data_channel
    if match_data_channel is None:
        match_data_channel = grpc.aio.insecure_channel(
            settings.matchdata_grpc_addr, options=CHANNEL_OPTIONS
        )
        closers.append(match_data_channel.close)
    match_data = match_data_service_pb2_grpc.MatchDataServiceStub(match_data_channel)
    match_data_health = dependency_health_pb2_grpc.DependencyHealthServiceStub(match_data_channel)

    checks = overrides.dependency_checks
    if checks is None:
        checks = [GrpcDependencyCheck("match-data-service", match_data_health)]
    checker = DependencyHealthChecker(SERVICE_NAME, checks)

    server = grpc.aio.server()
    quiz_service_pb2_grpc.add_QuizServiceServicer_to_server(QuizServicer(match_data), server)
    dependency_health_pb2_grpc.add_DependencyHealthServiceServicer_to_server(
        DependencyHealthServicer(checker), server
    )
    health_servicer = health.aio.HealthServicer()
    await health_servicer.set("", health_pb2.HealthCheckResponse.SERVING)
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    reflection.enable_server_reflection(
        [
            quiz_service_pb2.DESCRIPTOR.services_by_name["QuizService"].full_name,
            dependency_health_pb2.DESCRIPTOR.services_by_name["DependencyHealthService"].full_name,
            health.SERVICE_NAME,
            reflection.SERVICE_NAME,
        ],
        server,
    )
    port = server.add_insecure_port(f"[::]:{settings.grpc_port}")
    return Service(server=server, port=port, _closers=closers)
