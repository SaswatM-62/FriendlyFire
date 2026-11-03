import psycopg
import pytest
from sqlalchemy.exc import OperationalError

from scoring.migrate import wait_for_postgres
from tests.fakes import FlakyPostgres


def unreachable() -> OperationalError:
    return OperationalError("SELECT 1", None, psycopg.OperationalError("connection refused"))


def rejected() -> OperationalError:
    return OperationalError("SELECT 1", None, psycopg.errors.InvalidPassword("bad password"))


def no_sleep(_seconds: float) -> None:
    pass


def test_retries_until_postgres_answers() -> None:
    postgres = FlakyPostgres(unreachable(), unreachable())

    wait_for_postgres(postgres, sleep=no_sleep)

    assert postgres.connects == 3


def test_does_not_retry_an_error_from_the_server() -> None:
    postgres = FlakyPostgres(rejected())

    with pytest.raises(OperationalError):
        wait_for_postgres(postgres, sleep=no_sleep)

    assert postgres.connects == 1


def test_gives_up_after_the_last_attempt() -> None:
    postgres = FlakyPostgres(unreachable(), unreachable(), unreachable())

    with pytest.raises(OperationalError):
        wait_for_postgres(postgres, attempts=3, sleep=no_sleep)

    assert postgres.connects == 3


def test_sleeps_between_attempts_only() -> None:
    postgres = FlakyPostgres(unreachable(), unreachable())
    pauses: list[float] = []

    wait_for_postgres(postgres, interval=1.5, sleep=pauses.append)

    assert pauses == [1.5, 1.5]
