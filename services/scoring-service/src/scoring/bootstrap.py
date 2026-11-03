"""The only place objects are wired together."""

from collections.abc import Awaitable, Callable, Sequence
from dataclasses import dataclass, field

import grpc
from friendlyfire.health.v1 import dependency_health_pb2, dependency_health_pb2_grpc
from friendlyfire.scoring.v1 import scoring_service_pb2, scoring_service_pb2_grpc
from grpc_health.v1 import health, health_pb2, health_pb2_grpc
from grpc_reflection.v1alpha import reflection
from psycopg.conninfo import make_conninfo
from psycopg_pool import AsyncConnectionPool

from scoring.dependency_health import (
    DependencyCheck,
    DependencyHealthChecker,
    DependencyHealthServicer,
    PostgresDependencyCheck,
)
from scoring.quiz_repository import QuizRepository
from scoring.score_calculator import ScoreCalculator
from scoring.servicer import ScoringServicer
from scoring.settings import Settings

SERVICE_NAME = "scoring-service"


@dataclass
class Overrides:
    """Replacements for tests. Anything left as None is built from the settings."""

    pool: AsyncConnectionPool | None = None
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

    pool = overrides.pool
    if pool is None:
        conninfo = make_conninfo(
            host=settings.postgres_host,
            port=settings.postgres_port,
            dbname=settings.postgres_db,
            user=settings.postgres_user,
            password=settings.postgres_password.get_secret_value(),
            options="-c search_path=scoring",
        )
        pool = AsyncConnectionPool(
            conninfo,
            min_size=1,
            max_size=10,
            open=False,
            name="scoring",
            # Give up a failing reconnect after 10 s; the next request then tries again
            # right away. (The default keeps backing off for 5 minutes, so the pool
            # could take minutes to notice Postgres is back.)
            reconnect_timeout=10.0,
        )
        # Fails to start if Postgres is unreachable. Once running, the pool reconnects
        # by itself and the health check reports DOWN meanwhile.
        await pool.open(wait=True)
        closers.append(pool.close)

    checks = overrides.dependency_checks
    if checks is None:
        checks = [PostgresDependencyCheck(pool)]
    checker = DependencyHealthChecker(SERVICE_NAME, checks)

    server = grpc.aio.server()
    scoring_service_pb2_grpc.add_ScoringServiceServicer_to_server(
        ScoringServicer(QuizRepository(), ScoreCalculator()), server
    )
    dependency_health_pb2_grpc.add_DependencyHealthServiceServicer_to_server(
        DependencyHealthServicer(checker), server
    )
    health_servicer = health.aio.HealthServicer()
    await health_servicer.set("", health_pb2.HealthCheckResponse.SERVING)
    health_pb2_grpc.add_HealthServicer_to_server(health_servicer, server)
    reflection.enable_server_reflection(
        [
            scoring_service_pb2.DESCRIPTOR.services_by_name["ScoringService"].full_name,
            dependency_health_pb2.DESCRIPTOR.services_by_name["DependencyHealthService"].full_name,
            health.SERVICE_NAME,
            reflection.SERVICE_NAME,
        ],
        server,
    )
    port = server.add_insecure_port(f"[::]:{settings.grpc_port}")
    return Service(server=server, port=port, _closers=closers)
