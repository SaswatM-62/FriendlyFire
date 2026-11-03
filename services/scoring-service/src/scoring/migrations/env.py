"""Alembic's runtime. The connection comes from scoring.migrate, not from alembic.ini."""

from alembic import context
from sqlalchemy.engine import Connection

from scoring.migrate import SCHEMA

connection: Connection = context.config.attributes["connection"]

# Raw SQL migrations only (op.execute): there are no models to autogenerate from.
context.configure(
    connection=connection,
    target_metadata=None,
    version_table_schema=SCHEMA,
)
with context.begin_transaction():
    context.run_migrations()
