"""Fixtures for the QuizRepository integration tests.

Same approach as test_migrations.py and test_postgres_health.py: a throwaway
Postgres in Docker (Testcontainers), migrated with upgrade_to_head. Nothing
touches your dev database, and the container is deleted when the module ends,
so the tests need no cleanup code.
"""

import uuid
from collections.abc import AsyncIterator, Iterator

import psycopg
import pytest
from psycopg_pool import AsyncConnectionPool
from pydantic import SecretStr
from testcontainers.community.postgres import PostgresContainer

from scoring.migrate import upgrade_to_head
from scoring.quiz_repository import QuizRepository
from scoring.settings import Settings


def _conninfo(settings: Settings) -> str:
    return (
        f"host={settings.postgres_host} port={settings.postgres_port} "
        f"dbname={settings.postgres_db} user={settings.postgres_user} "
        f"password={settings.postgres_password.get_secret_value()}"
    )


@pytest.fixture(scope="module")
def migrated_settings() -> Iterator[Settings]:
    """A fresh Postgres with every migration applied, shared by one test module."""
    # Same image as compose.yaml.
    with PostgresContainer("postgres:16.15-alpine", driver=None) as container:
        settings = Settings(
            postgres_host=container.get_container_host_ip(),
            postgres_port=int(container.get_exposed_port(5432)),
            postgres_db=container.dbname,
            postgres_user=container.username,
            postgres_password=SecretStr(container.password),
        )
        # infra/local/postgres/init creates the schemas in Compose; a bare container has none.
        with psycopg.connect(_conninfo(settings), autocommit=True) as connection:
            connection.execute("CREATE SCHEMA scoring")
        upgrade_to_head(settings)
        yield settings


@pytest.fixture
async def pool(migrated_settings: Settings) -> AsyncIterator[AsyncConnectionPool]:
    pool = AsyncConnectionPool(_conninfo(migrated_settings), open=False)
    await pool.open()
    yield pool
    await pool.close()


@pytest.fixture
async def repo(pool: AsyncConnectionPool) -> QuizRepository:
    return QuizRepository(pool)


@pytest.fixture
def new_quiz(repo: QuizRepository):
    """Create a quiz through the repository and return its id."""

    async def _new_quiz(players=None, room_code="ROOM"):
        quiz_id = uuid.uuid4()
        if players is None:
            players = [("alice", "Alice"), ("bob", "Bob")]
        await repo.create_quiz(
            quiz_id,
            room_code,
            players,
            total_rounds=3,
            seconds_per_question=30,
        )
        return quiz_id

    return _new_quiz


@pytest.fixture
def set_finished(pool: AsyncConnectionPool):
    """Finish a quiz at an exact time, so history tests control the ordering."""

    async def _set_finished(quiz_id, when, reason="COMPLETED"):
        async with pool.connection() as conn:
            await conn.execute(
                "UPDATE scoring.quizzes SET finished_at = %s, end_reason = %s WHERE quiz_id = %s",
                (when, reason, quiz_id),
            )

    return _set_finished
