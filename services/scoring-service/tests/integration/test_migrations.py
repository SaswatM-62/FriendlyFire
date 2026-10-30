"""The migrations against a fresh Postgres in Docker (Testcontainers)."""

from collections.abc import Iterator

import psycopg
import pytest
from pydantic import SecretStr
from testcontainers.community.postgres import PostgresContainer

from scoring.migrate import upgrade_to_head
from scoring.settings import Settings

pytestmark = pytest.mark.integration


@pytest.fixture
def postgres() -> Iterator[PostgresContainer]:
    # Same image as compose.yaml.
    with PostgresContainer("postgres:16.15-alpine", driver=None) as container:
        yield container


def _settings(postgres: PostgresContainer) -> Settings:
    return Settings(
        postgres_host=postgres.get_container_host_ip(),
        postgres_port=int(postgres.get_exposed_port(5432)),
        postgres_db=postgres.dbname,
        postgres_user=postgres.username,
        postgres_password=SecretStr(postgres.password),
    )


def _applied_versions(postgres: PostgresContainer) -> list[str]:
    settings = _settings(postgres)
    with psycopg.connect(
        host=settings.postgres_host,
        port=settings.postgres_port,
        dbname=settings.postgres_db,
        user=settings.postgres_user,
        password=settings.postgres_password.get_secret_value(),
    ) as connection:
        rows = connection.execute("SELECT version_num FROM scoring.alembic_version").fetchall()
    return [row[0] for row in rows]


def _create_schema(postgres: PostgresContainer) -> None:
    # infra/local/postgres/init creates the schemas in Compose; a bare container has none.
    settings = _settings(postgres)
    with psycopg.connect(
        host=settings.postgres_host,
        port=settings.postgres_port,
        dbname=settings.postgres_db,
        user=settings.postgres_user,
        password=settings.postgres_password.get_secret_value(),
        autocommit=True,
    ) as connection:
        connection.execute("CREATE SCHEMA scoring")


def test_applies_first_migration_to_scoring_schema(postgres: PostgresContainer) -> None:
    _create_schema(postgres)

    upgrade_to_head(_settings(postgres))

    assert _applied_versions(postgres) == ["0001"]


def test_running_again_changes_nothing(postgres: PostgresContainer) -> None:
    _create_schema(postgres)
    upgrade_to_head(_settings(postgres))

    upgrade_to_head(_settings(postgres))

    assert _applied_versions(postgres) == ["0001"]
