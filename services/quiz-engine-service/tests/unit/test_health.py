"""grpc.health.v1, the stub RPCs and the dependency health checker."""

import time
from collections.abc import AsyncIterator

import grpc
import pytest
from friendlyfire.health.v1 import dependency_health_pb2_grpc
from friendlyfire.health.v1.dependency_health_pb2 import (
    COMPONENT_STATUS_DEGRADED,
    COMPONENT_STATUS_DOWN,
    COMPONENT_STATUS_UP,
    CheckDependenciesRequest,
)
from friendlyfire.quiz.v1 import quiz_service_pb2_grpc
from friendlyfire.quiz.v1.quiz_service_pb2 import GenerateQuizRequest
from grpc_health.v1 import health_pb2, health_pb2_grpc

from quiz_engine.bootstrap import Overrides, Service, build_server
from quiz_engine.dependency_health import DependencyHealthChecker
from quiz_engine.settings import Settings
from tests.fakes import FakeCheck


@pytest.fixture
async def service() -> AsyncIterator[Service]:
    """The real server on a random port, with a fake match-data-service check."""
    service = await build_server(
        Settings(grpc_port=0),
        Overrides(dependency_checks=[FakeCheck.up("match-data-service")]),
    )
    await service.start()
    yield service
    await service.stop(grace=None)


@pytest.fixture
async def channel(service: Service) -> AsyncIterator[grpc.aio.Channel]:
    async with grpc.aio.insecure_channel(f"localhost:{service.port}") as channel:
        yield channel


async def test_grpc_health_is_serving(channel: grpc.aio.Channel) -> None:
    response = await health_pb2_grpc.HealthStub(channel).Check(health_pb2.HealthCheckRequest())

    assert response.status == health_pb2.HealthCheckResponse.SERVING


async def test_rpcs_are_not_implemented_yet(channel: grpc.aio.Channel) -> None:
    quiz = quiz_service_pb2_grpc.QuizServiceStub(channel)

    with pytest.raises(grpc.aio.AioRpcError) as error:
        await quiz.GenerateQuiz(GenerateQuizRequest())

    assert error.value.code() == grpc.StatusCode.UNIMPLEMENTED
    assert error.value.details() == "not implemented yet"


async def test_check_dependencies_over_grpc(channel: grpc.aio.Channel) -> None:
    health = dependency_health_pb2_grpc.DependencyHealthServiceStub(channel)

    response = await health.CheckDependencies(CheckDependenciesRequest())

    assert response.component.name == "quiz-engine-service"
    assert response.component.status == COMPONENT_STATUS_UP
    assert [node.name for node in response.component.dependencies] == ["match-data-service"]


async def test_up_when_every_dependency_is_up() -> None:
    checker = DependencyHealthChecker("quiz-engine-service", [FakeCheck.up("match-data-service")])

    health = await checker.check()

    assert health.status == COMPONENT_STATUS_UP
    assert health.dependencies[0].status == COMPONENT_STATUS_UP


async def test_degraded_when_one_dependency_is_down() -> None:
    checker = DependencyHealthChecker(
        "quiz-engine-service",
        [FakeCheck.failing("match-data-service", ConnectionRefusedError("Connection refused"))],
    )

    health = await checker.check()

    assert health.status == COMPONENT_STATUS_DEGRADED
    assert health.dependencies[0].status == COMPONENT_STATUS_DOWN
    assert health.dependencies[0].error == "connection refused"


async def test_dependency_that_never_answers_hits_the_deadline() -> None:
    checker = DependencyHealthChecker(
        "quiz-engine-service", [FakeCheck.never_answers("match-data-service")], deadline=1.0
    )

    start = time.monotonic()
    health = await checker.check()

    # Loose on purpose: CI runners can be slow.
    assert time.monotonic() - start < 3
    assert health.status == COMPONENT_STATUS_DEGRADED
    assert health.dependencies[0].status == COMPONENT_STATUS_DOWN
    assert health.dependencies[0].error == "deadline exceeded"


async def test_answers_before_the_caller_gives_up() -> None:
    dependency = FakeCheck.up("match-data-service")
    checker = DependencyHealthChecker("quiz-engine-service", [dependency], deadline=1.0)

    await checker.check(caller_time_left=1.0)

    # The caller waits 1 s, so the dependency gets 1 s minus the reply margin.
    assert dependency.last_deadline == pytest.approx(0.8)


async def test_never_gives_a_dependency_more_than_its_own_deadline() -> None:
    dependency = FakeCheck.up("match-data-service")
    checker = DependencyHealthChecker("quiz-engine-service", [dependency], deadline=1.0)

    await checker.check(caller_time_left=30.0)

    assert dependency.last_deadline == 1.0
