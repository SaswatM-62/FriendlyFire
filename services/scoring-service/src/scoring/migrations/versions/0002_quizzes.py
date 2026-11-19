"""Quizzes, their players and every round's answers (FF-413).

A quiz's players are numbered by seat (the order Room sent them in), so their answers
stay linked after AnonymizePlayer clears the user_id (ADR-027).

Revision ID: 0002
Revises: 0001
"""

from alembic import op

revision = "0002"
down_revision = "0001"


def upgrade() -> None:
    op.execute(
        """
        CREATE TABLE quizzes (
            quiz_id              text PRIMARY KEY,
            room_code            text NOT NULL,
            host_display_name    text NOT NULL DEFAULT '',
            total_rounds         integer NOT NULL CHECK (total_rounds > 0),
            seconds_per_question integer NOT NULL CHECK (seconds_per_question > 0),
            started_at           timestamptz NOT NULL,
            finished_at          timestamptz,
            rounds_played        integer,
            end_reason           text
        )
        """
    )
    op.execute(
        """
        CREATE TABLE quiz_players (
            quiz_id      text NOT NULL REFERENCES quizzes ON DELETE CASCADE,
            seat         integer NOT NULL,
            -- NULL once the player deleted their account (AnonymizePlayer).
            user_id      text,
            display_name text NOT NULL,
            PRIMARY KEY (quiz_id, seat),
            UNIQUE (quiz_id, user_id)
        )
        """
    )
    op.execute("CREATE INDEX quiz_players_user_id ON quiz_players (user_id)")
    op.execute(
        """
        CREATE TABLE round_answers (
            quiz_id          text NOT NULL,
            round_number     integer NOT NULL CHECK (round_number > 0),
            seat             integer NOT NULL,
            answered         boolean NOT NULL,
            correct          boolean NOT NULL,
            response_time_ms integer NOT NULL,
            points           integer NOT NULL,
            PRIMARY KEY (quiz_id, round_number, seat),
            FOREIGN KEY (quiz_id, seat) REFERENCES quiz_players ON DELETE CASCADE
        )
        """
    )
    # GetPlayerHistory: a player's finished quizzes, newest first.
    op.execute(
        "CREATE INDEX quizzes_finished ON quizzes (finished_at DESC, quiz_id DESC) "
        "WHERE finished_at IS NOT NULL"
    )


def downgrade() -> None:
    op.execute("DROP TABLE round_answers")
    op.execute("DROP TABLE quiz_players")
    op.execute("DROP TABLE quizzes")
