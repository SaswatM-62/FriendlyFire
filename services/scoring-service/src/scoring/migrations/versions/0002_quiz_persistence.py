"""quiz persistence tables"""

from alembic import op

revision = "0002"
down_revision = "0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    op.execute("CREATE SCHEMA IF NOT EXISTS scoring")
    op.execute("""
        CREATE TABLE scoring.quizzes (
            quiz_id     uuid PRIMARY KEY,
            room_code   text NOT NULL,
            settings    jsonb NOT NULL DEFAULT '{}',
            started_at  timestamptz NOT NULL,
            finished_at timestamptz,
            end_reason  text
        )
    """)
    op.execute("""
        CREATE TABLE scoring.quiz_participants (
            quiz_id         uuid NOT NULL REFERENCES scoring.quizzes (quiz_id),
            player_id       text NOT NULL,
            display_name    text NOT NULL,
            PRIMARY KEY (quiz_id, player_id)
        )
    """)
    op.execute("""
        CREATE TABLE scoring.round_results (
            quiz_id         uuid NOT NULL,
            round_number    integer NOT NULL CHECK (round_number >= 1),
            player_id       text NOT NULL,
            score           integer NOT NULL,
            correct         boolean NOT NULL,
            PRIMARY KEY (quiz_id, round_number, player_id),
            FOREIGN KEY (quiz_id, player_id)
                REFERENCES scoring.quiz_participants (quiz_id, player_id)
        )
    """)
    op.execute("""
        CREATE INDEX quiz_participants_player_idx ON scoring.quiz_participants (player_id, quiz_id)
    """)


def downgrade() -> None:
    op.execute("DROP TABLE scoring.round_results")
    op.execute("DROP TABLE scoring.quiz_participants")
    op.execute("DROP TABLE scoring.quizzes")
