"""Quizzes, rounds and results in the `scoring` schema."""

import uuid
from dataclasses import dataclass
from datetime import datetime
from typing import Any

from psycopg import AsyncConnection
from psycopg.errors import UniqueViolation
from psycopg.types.json import Jsonb
from psycopg_pool import AsyncConnectionPool


@dataclass
class RoundResult:
    """One player's result for one round."""

    round_number: int
    player_id: str
    score: int
    correct: bool


@dataclass
class Quiz:
    """A quiz plus everything stored about it, as returned by get_quiz."""

    quiz_id: uuid.UUID
    room_code: str
    settings: dict[str, int]  # {"total_rounds": ..., "seconds_per_question": ...}
    started_at: datetime
    finished_at: datetime | None  # None while the quiz is still running
    end_reason: str | None  # None while the quiz is still running
    players: list[tuple[str, str]]  # (player_id, display_name)
    results: list[RoundResult]


class QuizAlreadyExists(Exception):
    """Raised by create_quiz when the quiz_id was already registered."""


class QuizRepository:
    """Reads and writes quiz data in Postgres.

    The connection pool is injected so tests and the real app can each pass
    their own. Every method borrows a connection for the duration of one
    `async with` block; everything inside that block is one transaction
    (committed on success, rolled back if an exception escapes).
    """

    def __init__(self, pool: AsyncConnectionPool[AsyncConnection[Any]]) -> None:
        self._pool = pool

    async def create_quiz(
        self,
        quiz_id: uuid.UUID,
        room_code: str,
        players: list[tuple[str, str]],  # (player_id, display_name)
        total_rounds: int,
        seconds_per_question: int,
    ) -> None:
        """Register a quiz and its players.

        The quiz row and the player rows are saved in one transaction, so a
        failure never leaves a quiz without its players.
        Raises QuizAlreadyExists if quiz_id is already in the table.
        """
        try:
            # Only these two values are stored as JSON "settings".
            settings = {
                "total_rounds": total_rounds,
                "seconds_per_question": seconds_per_question,
            }
            async with self._pool.connection() as conn:
                # Jsonb(...) tells psycopg to store the dict in the jsonb column.
                await conn.execute(
                    """
                    INSERT INTO scoring.quizzes (quiz_id, room_code, settings, started_at)
                    VALUES (%s, %s, %s, now())
                    """,
                    (quiz_id, room_code, Jsonb(settings)),
                )
                # One row per player, with the display name they joined with.
                async with conn.cursor() as cur:
                    await cur.executemany(
                        """
                        INSERT INTO scoring.quiz_participants (quiz_id, player_id, display_name)
                        VALUES (%s, %s, %s)
                        """,
                        [(quiz_id, pid, name) for pid, name in players],
                    )
        except UniqueViolation:
            # A duplicate quiz_id breaks the primary key. Translate the database
            # error into our own so callers don't need to know about SQL.
            raise QuizAlreadyExists(quiz_id) from None

    async def get_quiz(self, quiz_id: uuid.UUID) -> Quiz | None:
        """Return the quiz with its players and round results, or None if missing."""
        async with self._pool.connection() as conn:
            # 1. The quiz itself. No row means no such quiz.
            cur = await conn.execute(
                """
                SELECT room_code, settings, started_at, finished_at, end_reason
                FROM scoring.quizzes WHERE quiz_id = %s
                """,
                (quiz_id,),
            )
            quiz_row = await cur.fetchone()
            if quiz_row is None:
                return None

            # 2. Who played, with the display names they used.
            cur = await conn.execute(
                """
                SELECT player_id, display_name
                FROM scoring.quiz_participants WHERE quiz_id = %s
                ORDER BY player_id
                """,
                (quiz_id,),
            )
            player_rows = await cur.fetchall()

            # 3. Every player's result for every round. ORDER BY keeps the
            #    output predictable, which also makes tests easier to write.
            cur = await conn.execute(
                """
                SELECT round_number, player_id, score, correct
                FROM scoring.round_results WHERE quiz_id = %s
                ORDER BY round_number, player_id
                """,
                (quiz_id,),
            )
            result_rows = await cur.fetchall()

        # Assemble the three query results into one object. psycopg already
        # converts jsonb to a dict and timestamptz to a datetime.
        room_code, settings, started_at, finished_at, end_reason = quiz_row
        return Quiz(
            quiz_id=quiz_id,
            room_code=room_code,
            settings=settings,
            started_at=started_at,
            finished_at=finished_at,
            end_reason=end_reason,
            players=[(pid, name) for pid, name in player_rows],
            results=[RoundResult(r, pid, s, c) for r, pid, s, c in result_rows],
        )

    async def save_round(
        self,
        quiz_id: uuid.UUID,
        round_number: int,
        answers: list[tuple[str, int, bool]],  # (player_id, score, correct)
    ) -> None:
        """Save one round's results, replacing any earlier save of that round.

        Safe to retry: delete-then-insert in one transaction means calling this
        twice leaves exactly one result per player, with the latest values.
        """
        # Build one flat 5-value row per player, matching the INSERT columns.
        answer_rows = [
            (quiz_id, round_number, pid, score, correct) for pid, score, correct in answers
        ]
        async with self._pool.connection() as conn:
            # Clear the old results for this round (nothing happens on a first save).
            await conn.execute(
                "DELETE FROM scoring.round_results WHERE quiz_id = %s AND round_number = %s",
                (quiz_id, round_number),
            )
            # Insert the new results. Because the delete and the insert share a
            # transaction, nobody ever sees a half-replaced round.
            async with conn.cursor() as cur:
                await cur.executemany(
                    """
                    INSERT INTO scoring.round_results
                        (quiz_id, round_number, player_id, score, correct)
                    VALUES (%s, %s, %s, %s, %s)
                    """,
                    answer_rows,
                )

    async def list_finished_quizzes(
        self,
        player_id: str,
        limit: int,
        after: tuple[datetime, uuid.UUID] | None = None,
    ) -> tuple[list[tuple[Any, ...]], bool]:
        """One page of a player's finished quizzes, newest first.

        `after` is the "bookmark": the (finished_at, quiz_id) of the last quiz
        on the previous page, or None to start from the newest quiz.
        Returns (rows, has_more), where has_more says another page exists.
        """
        # JOIN links each quiz to its participant rows so we can filter by player.
        # Quizzes that are still running (finished_at is NULL) are skipped.
        sql = """
            SELECT q.quiz_id, q.room_code, q.finished_at
            FROM scoring.quizzes q
            JOIN scoring.quiz_participants p ON p.quiz_id = q.quiz_id
            WHERE p.player_id = %s
              AND q.finished_at IS NOT NULL
        """
        # Values for the %s placeholders, in order. The list grows below
        # whenever we add another placeholder to the SQL.
        params: list[Any] = [player_id]

        # Resume after the bookmark: keep only quizzes that sort *after* it in
        # newest-first order, i.e. older than (finished_at, quiz_id). The two
        # values are compared as a pair: finished_at first, quiz_id only to
        # break a tie between quizzes that finished at the same instant.
        if after is not None:
            sql += " AND (q.finished_at, q.quiz_id) < (%s, %s)"
            params += [after[0], after[1]]

        # Newest first. quiz_id is the tiebreaker that keeps the order stable.
        sql += " ORDER BY q.finished_at DESC, q.quiz_id DESC LIMIT %s"
        # Ask for one extra row: if it comes back, another page exists.
        params.append(limit + 1)

        async with self._pool.connection() as conn:
            cur = await conn.execute(sql, params)
            rows = await cur.fetchall()

        has_more = len(rows) > limit
        return rows[:limit], has_more  # drop the extra row before returning

    async def finish_quiz(self, quiz_id: uuid.UUID, end_reason: str) -> bool:
        """Mark a quiz as over. Returns False if no such quiz exists."""
        async with self._pool.connection() as conn:
            cur = await conn.execute(
                """
                UPDATE scoring.quizzes
                SET finished_at = now(), end_reason = %s
                WHERE quiz_id = %s
                """,
                (end_reason, quiz_id),
            )
            # rowcount is how many rows the UPDATE changed: 1 = found, 0 = missing.
            return cur.rowcount == 1
