"""Applies this service's database migrations (Alembic) to the `scoring` schema."""

import logging
import time
from collections.abc import Callable
from contextlib import AbstractContextManager
from pathlib import Path
from typing import Protocol

from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine
from sqlalchemy.engine import URL
from sqlalchemy.exc import OperationalError

from scoring.settings import Settings

SCHEMA = "scoring"

log = logging.getLogger("scoring-service")


class CanConnect(Protocol):
    """The part of a SQLAlchemy Engine that wait_for_postgres uses."""

    def connect(self) -> AbstractContextManager[object]: ...


def wait_for_postgres(
    engine: CanConnect,
    *,
    attempts: int = 30,
    interval: float = 2.0,
    sleep: Callable[[float], None] = time.sleep,
) -> None:
    """Wait (about a minute by default) until Postgres accepts connections.

    Postgres may still be starting. Only "couldn't reach it" errors are retried: an error
    from the server itself (wrong password, missing database) won't fix itself, so it is
    raised at once.
    """
    for attempt in range(1, attempts + 1):
        try:
            with engine.connect():
                return
        except OperationalError as error:
            # psycopg sets a SQLSTATE only on errors the server sent back.
            server_answered = getattr(error.orig, "sqlstate", None) is not None
            if server_answered or attempt == attempts:
                raise
            log.warning("waiting for postgres (try %d of %d): %s", attempt, attempts, error.orig)
            sleep(interval)


def upgrade_to_head(settings: Settings) -> None:
    """Like `alembic upgrade head`. Waits for Postgres first; raises if it never answers."""
    url = URL.create(
        "postgresql+psycopg",
        username=settings.postgres_user,
        password=settings.postgres_password.get_secret_value(),
        host=settings.postgres_host,
        port=settings.postgres_port,
        database=settings.postgres_db,
    )
    engine = create_engine(
        url, connect_args={"options": f"-c search_path={SCHEMA}", "connect_timeout": 5}
    )
    config = Config()
    config.set_main_option("script_location", str(Path(__file__).parent / "migrations"))
    try:
        wait_for_postgres(engine)
        with engine.begin() as connection:
            config.attributes["connection"] = connection
            command.upgrade(config, "head")
    finally:
        engine.dispose()
