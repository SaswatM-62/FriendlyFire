"""Quizzes, rounds and results in the `scoring` schema."""

from collections.abc import Sequence
from dataclasses import dataclass, field
from datetime import datetime
from typing import Protocol

from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.rows import dict_row
from psycopg_pool import AsyncConnectionPool


@dataclass(frozen=True)
class Seat:
    """One player of a quiz. user_id is None once they deleted their account."""

    seat: int
    user_id: str | None
    display_name: str


@dataclass(frozen=True)
class RoundResult:
    seat: int
    answered: bool
    correct: bool
    response_time_ms: int
    points: int


@dataclass(frozen=True)
class Total:
    points: int = 0
    correct_answers: int = 0


@dataclass(frozen=True)
class QuizRecord:
    quiz_id: str
    room_code: str
    host_display_name: str
    total_rounds: int
    seconds_per_question: int
    started_at: datetime
    players: tuple[Seat, ...]
    finished_at: datetime | None = None
    rounds_played: int | None = None
    end_reason: str | None = None
    # Each seat's points and correct answers over every recorded round.
    totals: dict[int, Total] = field(default_factory=dict)


class Quizzes(Protocol):
    """What the servicer needs from storage (QuizRepository; a fake in tests)."""

    async def create(self, quiz: QuizRecord) -> bool:
        """Saves a new quiz and its players; False if the quiz_id already exists."""
        ...

    async def get(self, quiz_id: str) -> QuizRecord | None: ...

    async def replace_round(
        self, quiz_id: str, round_number: int, results: Sequence[RoundResult]
    ) -> None:
        """Stores one round's results, replacing any earlier ones for that round."""
        ...

    async def finish(
        self, quiz_id: str, rounds_played: int, end_reason: str, finished_at: datetime
    ) -> None:
        """Marks the quiz as over, unless it already is (the first call wins)."""
        ...

    async def history(
        self, user_id: str, limit: int, after: tuple[datetime, str] | None
    ) -> list[QuizRecord]:
        """The player's finished quizzes, newest first, after the given (finished_at,
        quiz_id) position."""
        ...

    async def anonymize(self, user_id: str) -> None:
        """ "Deleted player" in every quiz of this user, and their user_id removed."""
        ...


DELETED_PLAYER = "Deleted player"


class QuizRepository:
    """Quizzes in Postgres. Every method takes a connection from the pool for its query."""

    def __init__(self, pool: AsyncConnectionPool) -> None:
        self._pool = pool

    async def create(self, quiz: QuizRecord) -> bool:
        async with self._pool.connection() as conn:
            try:
                async with conn.transaction():
                    await conn.execute(
                        """
                        INSERT INTO quizzes (quiz_id, room_code, host_display_name,
                            total_rounds, seconds_per_question, started_at)
                        VALUES (%s, %s, %s, %s, %s, %s)
                        """,
                        (
                            quiz.quiz_id,
                            quiz.room_code,
                            quiz.host_display_name,
                            quiz.total_rounds,
                            quiz.seconds_per_question,
                            quiz.started_at,
                        ),
                    )
                    async with conn.cursor() as cur:
                        await cur.executemany(
                            "INSERT INTO quiz_players (quiz_id, seat, user_id, display_name) "
                            "VALUES (%s, %s, %s, %s)",
                            [
                                (quiz.quiz_id, p.seat, p.user_id, p.display_name)
                                for p in quiz.players
                            ],
                        )
            except UniqueViolation:
                return False
        return True

    async def get(self, quiz_id: str) -> QuizRecord | None:
        async with self._pool.connection() as conn:
            records = await self._load(conn, [quiz_id])
        return records[0] if records else None

    async def replace_round(
        self, quiz_id: str, round_number: int, results: Sequence[RoundResult]
    ) -> None:
        async with self._pool.connection() as conn, conn.transaction():
            await conn.execute(
                "DELETE FROM round_answers WHERE quiz_id = %s AND round_number = %s",
                (quiz_id, round_number),
            )
            async with conn.cursor() as cur:
                await cur.executemany(
                    """
                    INSERT INTO round_answers (quiz_id, round_number, seat, answered, correct,
                        response_time_ms, points)
                    VALUES (%s, %s, %s, %s, %s, %s, %s)
                    """,
                    [
                        (
                            quiz_id,
                            round_number,
                            r.seat,
                            r.answered,
                            r.correct,
                            r.response_time_ms,
                            r.points,
                        )
                        for r in results
                    ],
                )

    async def finish(
        self, quiz_id: str, rounds_played: int, end_reason: str, finished_at: datetime
    ) -> None:
        async with self._pool.connection() as conn:
            await conn.execute(
                """
                UPDATE quizzes SET finished_at = %s, rounds_played = %s, end_reason = %s
                WHERE quiz_id = %s AND finished_at IS NULL
                """,
                (finished_at, rounds_played, end_reason, quiz_id),
            )

    async def history(
        self, user_id: str, limit: int, after: tuple[datetime, str] | None
    ) -> list[QuizRecord]:
        async with self._pool.connection() as conn:
            async with conn.cursor() as cur:
                await cur.execute(
                    """
                    SELECT q.quiz_id FROM quizzes q
                    JOIN quiz_players p ON p.quiz_id = q.quiz_id
                    WHERE p.user_id = %(user)s AND q.finished_at IS NOT NULL
                      AND (%(after_time)s::timestamptz IS NULL
                           OR (q.finished_at, q.quiz_id) < (%(after_time)s, %(after_id)s))
                    ORDER BY q.finished_at DESC, q.quiz_id DESC
                    LIMIT %(limit)s
                    """,
                    {
                        "user": user_id,
                        "after_time": after[0] if after else None,
                        "after_id": after[1] if after else None,
                        "limit": limit,
                    },
                )
                ids = [row[0] for row in await cur.fetchall()]
            records = await self._load(conn, ids)
        order = {quiz_id: i for i, quiz_id in enumerate(ids)}
        return sorted(records, key=lambda r: order[r.quiz_id])

    async def anonymize(self, user_id: str) -> None:
        async with self._pool.connection() as conn:
            await conn.execute(
                "UPDATE quiz_players SET user_id = NULL, display_name = %s WHERE user_id = %s",
                (DELETED_PLAYER, user_id),
            )

    @staticmethod
    async def _load(conn: AsyncConnection, quiz_ids: list[str]) -> list[QuizRecord]:
        if not quiz_ids:
            return []
        async with conn.cursor(row_factory=dict_row) as cur:
            await cur.execute("SELECT * FROM quizzes WHERE quiz_id = ANY(%s)", (quiz_ids,))
            quizzes = await cur.fetchall()
            await cur.execute(
                "SELECT quiz_id, seat, user_id, display_name FROM quiz_players "
                "WHERE quiz_id = ANY(%s) ORDER BY quiz_id, seat",
                (quiz_ids,),
            )
            seats = await cur.fetchall()
            await cur.execute(
                """
                SELECT quiz_id, seat, sum(points)::integer AS points,
                       count(*) FILTER (WHERE correct)::integer AS correct_answers
                FROM round_answers WHERE quiz_id = ANY(%s)
                GROUP BY quiz_id, seat
                """,
                (quiz_ids,),
            )
            totals = await cur.fetchall()
        players: dict[str, list[Seat]] = {}
        for row in seats:
            players.setdefault(row["quiz_id"], []).append(
                Seat(row["seat"], row["user_id"], row["display_name"])
            )
        sums: dict[str, dict[int, Total]] = {}
        for row in totals:
            sums.setdefault(row["quiz_id"], {})[row["seat"]] = Total(
                row["points"], row["correct_answers"]
            )
        return [
            QuizRecord(
                quiz_id=q["quiz_id"],
                room_code=q["room_code"],
                host_display_name=q["host_display_name"],
                total_rounds=q["total_rounds"],
                seconds_per_question=q["seconds_per_question"],
                started_at=q["started_at"],
                players=tuple(players.get(q["quiz_id"], [])),
                finished_at=q["finished_at"],
                rounds_played=q["rounds_played"],
                end_reason=q["end_reason"],
                totals=sums.get(q["quiz_id"], {}),
            )
            for q in quizzes
        ]
