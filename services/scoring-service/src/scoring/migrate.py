"""Applies this service's database migrations (Alembic) to the `scoring` schema."""

from pathlib import Path

from alembic import command
from alembic.config import Config
from sqlalchemy import create_engine
from sqlalchemy.engine import URL

from scoring.settings import Settings

SCHEMA = "scoring"


def upgrade_to_head(settings: Settings) -> None:
    """Like `alembic upgrade head`. Raises if Postgres is unreachable."""
    url = URL.create(
        "postgresql+psycopg",
        username=settings.postgres_user,
        password=settings.postgres_password.get_secret_value(),
        host=settings.postgres_host,
        port=settings.postgres_port,
        database=settings.postgres_db,
    )
    engine = create_engine(url, connect_args={"options": f"-c search_path={SCHEMA}"})
    config = Config()
    config.set_main_option("script_location", str(Path(__file__).parent / "migrations"))
    try:
        with engine.begin() as connection:
            config.attributes["connection"] = connection
            command.upgrade(config, "head")
    finally:
        engine.dispose()
