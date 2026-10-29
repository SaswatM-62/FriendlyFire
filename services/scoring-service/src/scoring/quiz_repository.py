"""Quizzes, rounds and results in the `scoring` schema."""


class QuizRepository:
    # TODO(ff): store quizzes, rounds and results in the `scoring` schema
    #   - quizzes (room code, settings, started/finished, end reason), participants
    #     with their display names, and round results keyed by (quiz_id, round_number)
    #     so RecordRound can be retried
    #   - a player's finished quizzes, newest first, paged (GetPlayerHistory)
    #   - uses: the connection pool (inject it); tables: add an Alembic migration
    ...
