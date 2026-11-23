"""QuizRepository against a real Postgres in Docker (Testcontainers)."""

from collections.abc import AsyncIterator, Iterator
from dataclasses import replace
from datetime import UTC, datetime, timedelta

import psycopg
import pytest
from psycopg.conninfo import make_conninfo
from psycopg_pool import AsyncConnectionPool
from pydantic import SecretStr
from testcontainers.community.postgres import PostgresContainer

from scoring.migrate import upgrade_to_head
from scoring.quiz_repository import QuizRecord, QuizRepository, RoundResult, Seat, Total
from scoring.settings import Settings

pytestmark = pytest.mark.integration

START = datetime(2026, 11, 19, 18, 0, tzinfo=UTC)


@pytest.fixture(scope="module")
def postgres() -> Iterator[PostgresContainer]:
    # Same image as compose.yaml.
    with PostgresContainer("postgres:16.15-alpine", driver=None) as container:
        settings = _settings(container)
        with psycopg.connect(_conninfo(settings), autocommit=True) as connection:
            connection.execute("CREATE SCHEMA scoring")
        upgrade_to_head(settings)
        yield container


def _settings(postgres: PostgresContainer) -> Settings:
    return Settings(
        postgres_host=postgres.get_container_host_ip(),
        postgres_port=int(postgres.get_exposed_port(5432)),
        postgres_db=postgres.dbname,
        postgres_user=postgres.username,
        postgres_password=SecretStr(postgres.password),
    )


def _conninfo(settings: Settings, **extra: str) -> str:
    return make_conninfo(
        host=settings.postgres_host,
        port=settings.postgres_port,
        dbname=settings.postgres_db,
        user=settings.postgres_user,
        password=settings.postgres_password.get_secret_value(),
        **extra,
    )


@pytest.fixture
async def repo(postgres: PostgresContainer) -> AsyncIterator[QuizRepository]:
    conninfo = _conninfo(_settings(postgres), options="-c search_path=scoring")
    async with AsyncConnectionPool(conninfo, min_size=1, max_size=2) as pool:
        async with pool.connection() as conn:
            await conn.execute("TRUNCATE quizzes CASCADE")
        yield QuizRepository(pool)


def quiz(quiz_id: str = "q1", *users: str) -> QuizRecord:
    users = users or ("a", "b")
    return QuizRecord(
        quiz_id=quiz_id,
        room_code="K7QM2X",
        host_display_name="Maya",
        total_rounds=5,
        seconds_per_question=20,
        started_at=START,
        players=tuple(Seat(n, uid, uid.upper()) for n, uid in enumerate(users)),
    )


async def test_a_quiz_round_trips_with_its_players(repo: QuizRepository) -> None:
    assert await repo.create(quiz())
    assert not await repo.create(quiz())

    stored = await repo.get("q1")
    assert stored == quiz()
    assert await repo.get("missing") is None


async def test_rounds_add_up_and_a_retried_round_replaces_the_first(repo: QuizRepository) -> None:
    await repo.create(quiz())
    await repo.replace_round(
        "q1", 1, [RoundResult(0, True, True, 0, 1000), RoundResult(1, False, False, 0, 0)]
    )
    await repo.replace_round(
        "q1", 2, [RoundResult(0, True, False, 3000, 0), RoundResult(1, True, True, 10_000, 750)]
    )
    await repo.replace_round(
        "q1", 2, [RoundResult(0, True, False, 3000, 0), RoundResult(1, True, True, 0, 1000)]
    )

    stored = await repo.get("q1")
    assert stored is not None
    assert stored.totals == {0: Total(1000, 1), 1: Total(1000, 1)}


async def test_finish_keeps_the_first_call(repo: QuizRepository) -> None:
    await repo.create(quiz())
    await repo.finish("q1", 3, "END_REASON_VOTED_TO_END", START + timedelta(minutes=5))
    await repo.finish("q1", 5, "END_REASON_COMPLETED", START + timedelta(minutes=9))

    stored = await repo.get("q1")
    assert stored is not None
    assert (stored.rounds_played, stored.end_reason) == (3, "END_REASON_VOTED_TO_END")
    assert stored.finished_at == START + timedelta(minutes=5)


async def test_history_is_finished_quizzes_newest_first_in_pages(repo: QuizRepository) -> None:
    for n in range(4):
        await repo.create(quiz(f"q{n}"))
        await repo.finish(f"q{n}", 5, "END_REASON_COMPLETED", START + timedelta(minutes=n))
    # Same finish time: the quiz ID breaks the tie.
    await repo.create(quiz("q9"))
    await repo.finish("q9", 5, "END_REASON_COMPLETED", START + timedelta(minutes=3))
    await repo.create(quiz("running"))
    await repo.create(quiz("theirs", "b", "c"))
    await repo.finish("theirs", 5, "END_REASON_COMPLETED", START)

    first = await repo.history("a", 3, None)
    assert [q.quiz_id for q in first] == ["q9", "q3", "q2"]
    last = first[-1]
    assert last.finished_at is not None
    rest = await repo.history("a", 3, (last.finished_at, last.quiz_id))
    assert [q.quiz_id for q in rest] == ["q1", "q0"]


async def test_anonymize_clears_the_link_but_keeps_the_answers(repo: QuizRepository) -> None:
    await repo.create(quiz())
    await repo.replace_round("q1", 1, [RoundResult(0, True, True, 0, 1000)])
    await repo.finish("q1", 1, "END_REASON_VOTED_TO_END", START)

    await repo.anonymize("a")
    await repo.anonymize("a")

    stored = await repo.get("q1")
    assert stored is not None
    assert stored.players[0] == Seat(0, None, "Deleted player")
    assert stored.totals[0] == Total(1000, 1)
    assert await repo.history("a", 10, None) == []
    assert [q.quiz_id for q in await repo.history("b", 10, None)] == ["q1"]


async def test_two_deleted_players_fit_in_one_quiz(repo: QuizRepository) -> None:
    # UNIQUE (quiz_id, user_id) allows several NULLs.
    await repo.create(quiz())
    await repo.anonymize("a")
    await repo.anonymize("b")
    stored = await repo.get("q1")
    assert stored is not None
    assert [p.user_id for p in stored.players] == [None, None]
    assert replace(stored, players=()).quiz_id == "q1"
