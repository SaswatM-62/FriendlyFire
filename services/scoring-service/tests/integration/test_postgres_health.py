"""The Postgres health check against a real Postgres in Docker (Testcontainers)."""

from collections.abc import AsyncIterator, Iterator

import grpc
import pytest
from friendlyfire.health.v1 import dependency_health_pb2_grpc
from friendlyfire.health.v1.dependency_health_pb2 import (
    COMPONENT_STATUS_UP,
    CheckDependenciesRequest,
)
from pydantic import SecretStr
from testcontainers.community.postgres import PostgresContainer

from scoring.bootstrap import Service, build_server
from scoring.settings import Settings

pytestmark = pytest.mark.integration


@pytest.fixture(scope="module")
def postgres() -> Iterator[PostgresContainer]:
    # Same image as compose.yaml.
    with PostgresContainer("postgres:16.15-alpine", driver=None) as container:
        yield container


@pytest.fixture
async def service(postgres: PostgresContainer) -> AsyncIterator[Service]:
    settings = Settings(
        grpc_port=0,
        postgres_host=postgres.get_container_host_ip(),
        postgres_port=int(postgres.get_exposed_port(5432)),
        postgres_db=postgres.dbname,
        postgres_user=postgres.username,
        postgres_password=SecretStr(postgres.password),
    )
    service = await build_server(settings)
    await service.start()
    yield service
    await service.stop(grace=None)


async def test_reports_postgres_up(service: Service) -> None:
    async with grpc.aio.insecure_channel(f"localhost:{service.port}") as channel:
        health = dependency_health_pb2_grpc.DependencyHealthServiceStub(channel)
        # Warm up: on a slow CI runner the very first call can miss the 1-second deadline.
        await health.CheckDependencies(CheckDependenciesRequest())

        response = await health.CheckDependencies(CheckDependenciesRequest())

    postgres_node = response.component.dependencies[0]
    assert postgres_node.name == "postgres"
    assert postgres_node.status == COMPONENT_STATUS_UP
