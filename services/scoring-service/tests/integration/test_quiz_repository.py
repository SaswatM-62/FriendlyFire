"""Integration tests for QuizRepository (real Postgres, see conftest.py)."""

import uuid
from datetime import UTC, datetime, timedelta

import pytest
from psycopg.errors import CheckViolation, ForeignKeyViolation, UniqueViolation

from scoring.quiz_repository import QuizAlreadyExists

pytestmark = pytest.mark.integration

T0 = datetime(2026, 1, 1, 12, 0, tzinfo=UTC)


# ---------------------------------------------------------------- helpers


async def results_of(repo, quiz_id):
    """A quiz's round results as plain tuples: (round, player, score, correct)."""
    quiz = await repo.get_quiz(quiz_id)
    return [(r.round_number, r.player_id, r.score, r.correct) for r in quiz.results]


def unique_player():
    """A player id nobody else uses, so history tests only see their own quizzes."""
    return f"player-{uuid.uuid4()}"


async def finished_quiz(new_quiz, set_finished, player, minutes):
    """A quiz this player was in, finished `minutes` after T0."""
    quiz_id = await new_quiz(players=[(player, "P")])
    await set_finished(quiz_id, T0 + timedelta(minutes=minutes))
    return quiz_id


async def collect_pages(repo, player, limit):
    """Walk the whole history page by page using the bookmark."""
    pages = []
    after = None
    for _ in range(20):  # safety net so a bug can't loop forever
        rows, has_more = await repo.list_finished_quizzes(player, limit, after)
        pages.append([row[0] for row in rows])
        if not has_more:
            return pages
        last = rows[-1]
        after = (last[2], last[0])  # (finished_at, quiz_id)
    raise AssertionError("paging never reached the last page")


# ----------------------------------------------------------- create / get


async def test_create_quiz_stores_quiz_and_players(repo, new_quiz):
    quiz_id = await new_quiz(players=[("alice", "Alice"), ("bob", "Bobby")])

    quiz = await repo.get_quiz(quiz_id)

    assert quiz.quiz_id == quiz_id
    assert quiz.room_code == "ROOM"
    assert quiz.settings == {"total_rounds": 3, "seconds_per_question": 30}
    assert quiz.players == [("alice", "Alice"), ("bob", "Bobby")]
    assert quiz.started_at is not None
    assert quiz.finished_at is None
    assert quiz.end_reason is None
    assert quiz.results == []


async def test_get_quiz_returns_none_for_unknown_quiz(repo):
    assert await repo.get_quiz(uuid.uuid4()) is None


async def test_create_quiz_twice_raises_and_keeps_the_original(repo, new_quiz):
    quiz_id = await new_quiz(players=[("alice", "Alice")])

    with pytest.raises(QuizAlreadyExists):
        await repo.create_quiz(quiz_id, "OTHER", [("zed", "Zed")], 5, 10)

    quiz = await repo.get_quiz(quiz_id)
    assert quiz.room_code == "ROOM"
    assert quiz.players == [("alice", "Alice")]


async def test_create_quiz_with_a_repeated_player_saves_nothing(repo):
    quiz_id = uuid.uuid4()

    # Today every UniqueViolation is reported as QuizAlreadyExists, including
    # a repeated player. The important part is that nothing is left behind.
    with pytest.raises(QuizAlreadyExists):
        await repo.create_quiz(quiz_id, "ROOM", [("alice", "A"), ("alice", "A2")], 3, 30)

    assert await repo.get_quiz(quiz_id) is None


async def test_create_quiz_allows_an_empty_player_list(repo, new_quiz):
    quiz_id = await new_quiz(players=[])

    quiz = await repo.get_quiz(quiz_id)

    assert quiz.players == []


async def test_display_name_is_stored_per_quiz_and_keeps_unicode(repo, new_quiz):
    first = await new_quiz(players=[("alice", "Alice")])
    second = await new_quiz(players=[("alice", "Ålice 🎯")])

    assert (await repo.get_quiz(first)).players == [("alice", "Alice")]
    assert (await repo.get_quiz(second)).players == [("alice", "Ålice 🎯")]


# -------------------------------------------------------------- save_round


async def test_saving_the_same_round_twice_leaves_one_result(repo, new_quiz):
    """Done-when #1."""
    quiz_id = await new_quiz()
    answers = [("alice", 800, True), ("bob", 0, False)]

    await repo.save_round(quiz_id, 1, answers)
    await repo.save_round(quiz_id, 1, answers)

    assert await results_of(repo, quiz_id) == [
        (1, "alice", 800, True),
        (1, "bob", 0, False),
    ]


async def test_saving_a_round_again_replaces_the_values(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 1, [("alice", 100, True), ("bob", 0, False)])

    await repo.save_round(quiz_id, 1, [("alice", 0, False), ("bob", 900, True)])

    assert await results_of(repo, quiz_id) == [
        (1, "alice", 0, False),
        (1, "bob", 900, True),
    ]


async def test_saving_a_round_again_removes_players_no_longer_in_it(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 1, [("alice", 100, True), ("bob", 200, True)])

    await repo.save_round(quiz_id, 1, [("alice", 300, True)])

    assert await results_of(repo, quiz_id) == [(1, "alice", 300, True)]


async def test_saving_one_round_leaves_other_rounds_alone(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 1, [("alice", 100, True)])
    await repo.save_round(quiz_id, 2, [("alice", 200, True)])

    await repo.save_round(quiz_id, 2, [("alice", 999, False)])

    assert await results_of(repo, quiz_id) == [
        (1, "alice", 100, True),
        (2, "alice", 999, False),
    ]


async def test_results_come_back_ordered_by_round_then_player(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 2, [("bob", 2, True), ("alice", 1, True)])
    await repo.save_round(quiz_id, 1, [("bob", 4, True), ("alice", 3, True)])

    assert await results_of(repo, quiz_id) == [
        (1, "alice", 3, True),
        (1, "bob", 4, True),
        (2, "alice", 1, True),
        (2, "bob", 2, True),
    ]


async def test_saving_an_empty_round_clears_it(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 1, [("alice", 100, True)])

    await repo.save_round(quiz_id, 1, [])

    assert await results_of(repo, quiz_id) == []


async def test_save_round_rejects_an_unknown_quiz(repo):
    with pytest.raises(ForeignKeyViolation):
        await repo.save_round(uuid.uuid4(), 1, [("alice", 10, True)])


async def test_save_round_rejects_a_player_who_is_not_in_the_quiz(repo, new_quiz):
    quiz_id = await new_quiz()

    with pytest.raises(ForeignKeyViolation):
        await repo.save_round(quiz_id, 1, [("ghost", 10, True)])

    assert await results_of(repo, quiz_id) == []


async def test_save_round_rejects_round_number_below_one(repo, new_quiz):
    quiz_id = await new_quiz()

    with pytest.raises(CheckViolation):
        await repo.save_round(quiz_id, 0, [("alice", 10, True)])


async def test_save_round_rejects_the_same_player_twice(repo, new_quiz):
    quiz_id = await new_quiz()

    with pytest.raises(UniqueViolation):
        await repo.save_round(quiz_id, 1, [("alice", 1, True), ("alice", 2, True)])

    assert await results_of(repo, quiz_id) == []


async def test_a_failed_save_keeps_the_previous_version_of_the_round(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 1, [("alice", 800, True)])

    # "ghost" breaks the foreign key. The delete of the old round happened in
    # the same transaction, so it must be rolled back too.
    with pytest.raises(ForeignKeyViolation):
        await repo.save_round(quiz_id, 1, [("alice", 1, True), ("ghost", 5, True)])

    assert await results_of(repo, quiz_id) == [(1, "alice", 800, True)]


# ------------------------------------------------------------- finish_quiz


async def test_finish_quiz_sets_the_finish_time_and_reason(repo, new_quiz):
    quiz_id = await new_quiz()

    assert await repo.finish_quiz(quiz_id, "COMPLETED") is True

    quiz = await repo.get_quiz(quiz_id)
    assert quiz.end_reason == "COMPLETED"
    assert quiz.finished_at is not None
    assert quiz.finished_at >= quiz.started_at


async def test_finish_quiz_returns_false_for_an_unknown_quiz(repo):
    assert await repo.finish_quiz(uuid.uuid4(), "COMPLETED") is False


async def test_finish_quiz_keeps_the_results(repo, new_quiz):
    quiz_id = await new_quiz()
    await repo.save_round(quiz_id, 1, [("alice", 800, True)])

    await repo.finish_quiz(quiz_id, "COMPLETED")

    assert await results_of(repo, quiz_id) == [(1, "alice", 800, True)]


# ----------------------------------------------------------------- history


async def test_history_is_newest_first(repo, new_quiz, set_finished):
    """Done-when #2."""
    player = unique_player()
    # Created out of order on purpose, so the result can't be insertion order.
    oldest = await finished_quiz(new_quiz, set_finished, player, 1)
    newest = await finished_quiz(new_quiz, set_finished, player, 3)
    middle = await finished_quiz(new_quiz, set_finished, player, 2)

    rows, has_more = await repo.list_finished_quizzes(player, limit=10)

    assert [row[0] for row in rows] == [newest, middle, oldest]
    assert has_more is False


async def test_history_rows_contain_quiz_id_room_code_and_finish_time(repo, new_quiz, set_finished):
    player = unique_player()
    quiz_id = await new_quiz(players=[(player, "P")], room_code="XYZ9")
    await set_finished(quiz_id, T0)

    rows, _ = await repo.list_finished_quizzes(player, limit=10)

    assert rows == [(quiz_id, "XYZ9", T0)]


async def test_history_skips_quizzes_that_are_still_running(repo, new_quiz, set_finished):
    player = unique_player()
    done = await finished_quiz(new_quiz, set_finished, player, 1)
    await new_quiz(players=[(player, "P")])  # never finished

    rows, _ = await repo.list_finished_quizzes(player, limit=10)

    assert [row[0] for row in rows] == [done]


async def test_history_only_includes_the_requested_players_quizzes(repo, new_quiz, set_finished):
    me, other = unique_player(), unique_player()
    mine = await finished_quiz(new_quiz, set_finished, me, 1)
    await finished_quiz(new_quiz, set_finished, other, 2)

    rows, _ = await repo.list_finished_quizzes(me, limit=10)

    assert [row[0] for row in rows] == [mine]


async def test_a_shared_quiz_shows_up_in_every_players_history(repo, new_quiz, set_finished):
    a, b = unique_player(), unique_player()
    quiz_id = await new_quiz(players=[(a, "A"), (b, "B")])
    await set_finished(quiz_id, T0)

    rows_a, _ = await repo.list_finished_quizzes(a, limit=10)
    rows_b, _ = await repo.list_finished_quizzes(b, limit=10)

    assert [row[0] for row in rows_a] == [quiz_id]
    assert [row[0] for row in rows_b] == [quiz_id]


async def test_a_quiz_appears_once_even_with_many_rounds(repo, new_quiz, set_finished):
    player = unique_player()
    quiz_id = await new_quiz(players=[(player, "P")])
    for round_number in (1, 2, 3):
        await repo.save_round(quiz_id, round_number, [(player, 100, True)])
    await set_finished(quiz_id, T0)

    rows, _ = await repo.list_finished_quizzes(player, limit=10)

    assert [row[0] for row in rows] == [quiz_id]


async def test_history_is_empty_for_a_player_with_no_quizzes(repo):
    rows, has_more = await repo.list_finished_quizzes(unique_player(), limit=10)

    assert rows == []
    assert has_more is False


async def test_history_limit_with_exactly_one_page_has_no_more(repo, new_quiz, set_finished):
    player = unique_player()
    for minutes in (1, 2, 3):
        await finished_quiz(new_quiz, set_finished, player, minutes)

    rows, has_more = await repo.list_finished_quizzes(player, limit=3)

    assert len(rows) == 3
    assert has_more is False


async def test_history_limit_smaller_than_total_returns_the_newest_and_has_more(
    repo, new_quiz, set_finished
):
    player = unique_player()
    quizzes = [
        await finished_quiz(new_quiz, set_finished, player, minutes) for minutes in (1, 2, 3, 4)
    ]

    rows, has_more = await repo.list_finished_quizzes(player, limit=3)

    assert [row[0] for row in rows] == [quizzes[3], quizzes[2], quizzes[1]]
    assert has_more is True


async def test_paging_walks_the_whole_history_newest_first(repo, new_quiz, set_finished):
    player = unique_player()
    q1, q2, q3, q4, q5 = [
        await finished_quiz(new_quiz, set_finished, player, minutes) for minutes in (1, 2, 3, 4, 5)
    ]

    pages = await collect_pages(repo, player, limit=2)

    assert pages == [[q5, q4], [q3, q2], [q1]]


async def test_quizzes_finished_at_the_same_instant_are_ordered_by_id(repo, new_quiz, set_finished):
    player = unique_player()
    ids = [await finished_quiz(new_quiz, set_finished, player, 7) for _ in range(3)]

    rows, _ = await repo.list_finished_quizzes(player, limit=10)

    assert [row[0] for row in rows] == sorted(ids, reverse=True)


async def test_paging_through_ties_skips_and_repeats_nothing(repo, new_quiz, set_finished):
    player = unique_player()
    ids = [await finished_quiz(new_quiz, set_finished, player, 7) for _ in range(5)]

    pages = await collect_pages(repo, player, limit=2)

    assert [len(page) for page in pages] == [2, 2, 1]
    flat = [quiz_id for page in pages for quiz_id in page]
    assert flat == sorted(ids, reverse=True)  # every quiz once, in a stable order


async def test_paging_with_ties_next_to_other_times(repo, new_quiz, set_finished):
    player = unique_player()
    newest = await finished_quiz(new_quiz, set_finished, player, 10)
    tied = [await finished_quiz(new_quiz, set_finished, player, 5) for _ in range(3)]
    oldest = await finished_quiz(new_quiz, set_finished, player, 1)

    pages = await collect_pages(repo, player, limit=2)

    flat = [quiz_id for page in pages for quiz_id in page]
    assert flat == [newest, *sorted(tied, reverse=True), oldest]


async def test_bookmark_past_the_oldest_quiz_returns_nothing(repo, new_quiz, set_finished):
    player = unique_player()
    quiz_id = await finished_quiz(new_quiz, set_finished, player, 1)

    rows, has_more = await repo.list_finished_quizzes(
        player, limit=10, after=(T0 + timedelta(minutes=1), quiz_id)
    )

    assert rows == []
    assert has_more is False
