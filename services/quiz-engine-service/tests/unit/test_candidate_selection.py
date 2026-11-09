"""Check the larger bank's selector against exhaustive small graphs."""

from itertools import combinations

from friendlyfire.quiz.v1.quiz_service_pb2 import Question
from hypothesis import given
from hypothesis import strategies as st

from quiz_engine.question_generator import _Candidate, _select
from quiz_engine.templates import Scope


@given(
    entries=st.lists(
        st.tuples(st.integers(0, 3), st.integers(0, 3), st.sampled_from(["all", "shared", "solo"])),
        max_size=8,
    ),
    count=st.integers(0, 5),
)
def test_selection_matches_exhaustive_optimum(
    entries: list[tuple[int, int, Scope]], count: int
) -> None:
    candidates = [_Candidate(str(t), str(f), scope, Question()) for t, f, scope in entries]
    selected = _select(candidates, count)
    assert len(selected) <= count
    assert len({c.template_id for c in selected}) == len(selected)
    assert len({c.fact_key for c in selected}) == len(selected)
    optimum = (0, 0)
    for size in range(min(count, len(candidates)) + 1):
        for choice in combinations(candidates, size):
            if (
                len({c.template_id for c in choice}) != size
                or len({c.fact_key for c in choice}) != size
            ):
                continue
            scopes = {c.scope for c in choice}
            optimum = max(optimum, (size, int("shared" in scopes and "solo" in scopes)))
    scopes = {c.scope for c in selected}
    assert (len(selected), int("shared" in scopes and "solo" in scopes)) == optimum


def test_large_bank_does_not_enumerate_every_combination() -> None:
    candidates = [_Candidate(str(n), str(n), "all", Question()) for n in range(100)]
    assert len(_select(candidates, 20)) == 20
