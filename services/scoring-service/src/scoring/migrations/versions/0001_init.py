"""Empty first migration: proves the setup works before real tables arrive.

Quizzes, rounds and results are added in FF-413.

Revision ID: 0001
Revises:
"""

from alembic import op

revision = "0001"
down_revision = None


def upgrade() -> None:
    op.execute("SELECT 1")


def downgrade() -> None:
    op.execute("SELECT 1")
