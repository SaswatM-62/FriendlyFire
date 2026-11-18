"""Properties and concrete regressions for the FF-402 question engine."""

from dataclasses import replace
from fractions import Fraction
from time import perf_counter

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question, QuizPlayer
from hypothesis import given
from hypothesis import strategies as st

from quiz_engine.question_generator import (
    LEVELS,
    Difficulty,
    QuestionGenerator,
    arrange,
    difficulty,
    quiz_mix,
)
from quiz_engine.templates import TEMPLATES, QuestionTemplate


def template(template_id: str) -> QuestionTemplate:
    return next(t for t in TEMPLATES if t.id == template_id)


KILLS = template("most-kills-v1")
ACS = template("highest-acs-v1")
WINS = template("recent-wins-v1")


def template_ids(questions: list[Question]) -> list[str]:
    return [q.template_id for q in questions]


def players() -> list[QuizPlayer]:
    return [
        QuizPlayer(user_id="a", display_name="Alice"),
        QuizPlayer(user_id="b", display_name="Bob"),
    ]


def row(
    uid: str,
    match: str,
    kills: int = 1,
    score: float = 100,
    rounds: float = 10,
    seconds: int = 1,
    won: bool = False,
) -> PlayerMatchStats:
    result = PlayerMatchStats(
        user_id=uid,
        match_id=match,
        game="valorant",
        kills=kills,
        won=won,
        labels={"queue_id": "competitive", "result": "win" if won else "loss"},
        numbers={"combat_score": score, "rounds_played": rounds},
    )
    result.played_at.seconds = seconds
    return result


def history() -> list[PlayerMatchStats]:
    return [
        row(
            uid,
            f"shared-{n}" if n < 5 else f"{uid}-{n}",
            kills=30 if uid == "a" else 10,
            score=400 if uid == "a" else 100,
            seconds=n + 1,
            won=uid == "a",
        )
        for n in range(10)
        for uid in ("a", "b")
    ]


def rated(leads: dict[str, int]) -> tuple[list[QuestionTemplate], list[PlayerMatchStats]]:
    """One single-scope kills template per entry, each in its own game.

    Alice has 100 kills and Bob 100 - lead, so each question's lead is lead%.
    """
    templates, stats = [], []
    for template_id, lead in leads.items():
        templates.append(replace(KILLS, id=template_id, game=template_id, scopes=("all",)))
        for uid, kills in (("a", 100), ("b", 100 - lead)):
            match = row(uid, f"{template_id}-{uid}", kills=kills)
            match.game = template_id
            stats.append(match)
    return templates, stats


LEAD: dict[Difficulty, int] = {"easy": 50, "medium": 25, "hard": 10}


def levels_of(questions: list[Question]) -> list[str]:
    """The level of each rated() question; its template ID starts with the level."""
    return [q.template_id.split("-")[0] for q in questions]


def shape(questions: list[Question]) -> str:
    """The quiz's difficulties as letters, e.g. "EEMMHHMMEE"."""
    return "".join(level[0].upper() for level in levels_of(questions))


def pools(easy: int = 0, medium: int = 0, hard: int = 0) -> dict[str, int]:
    counts = {"easy": easy, "medium": medium, "hard": hard}
    return {f"{level}-{n}": LEAD[level] for level in LEVELS for n in range(counts[level])}


@st.composite
def players_and_stats(draw: st.DrawFn) -> tuple[list[QuizPlayer], list[PlayerMatchStats]]:
    ids = draw(st.lists(st.uuids().map(str), min_size=2, max_size=5, unique=True))
    # Some players have too few matches: options, but never compared.
    short = draw(st.lists(st.booleans(), min_size=len(ids), max_size=len(ids)))
    roster = [
        QuizPlayer(user_id=uid, display_name=f"Player {n}", not_enough_matches=flag)
        for n, (uid, flag) in enumerate(zip(ids, short, strict=True))
    ]
    # Half the examples give everyone 10+ matches (so recent wins can apply), and half
    # leave every stat in place (so pooled ACS can apply); the rest cover the skips.
    full_history = draw(st.booleans())
    damaged = draw(st.booleans())
    stats = []
    for uid in ids:
        count = draw(st.integers(10, 15) if full_history else st.integers(0, 15))
        shared_count = draw(st.integers(0, count))
        for n in range(count):
            match = row(
                uid,
                f"shared-{n}" if n < shared_count else f"{uid}-{n}",
                kills=draw(st.integers(0, 60)),
                score=draw(st.integers(0, 10000)),
                rounds=draw(st.integers(1, 30)),
                seconds=n + 1,
                won=draw(st.booleans()),
            )
            # In a damaged example, about one match in ten loses a stat.
            if damaged and draw(st.integers(0, 9)) == 0:
                missing = draw(st.sampled_from(("combat_score", "rounds_played", "result")))
                if missing == "result":
                    del match.labels[missing]
                else:
                    del match.numbers[missing]
            stats.append(match)
    return roster, stats


@given(inputs=players_and_stats(), seed=st.integers(min_value=0, max_value=2**31 - 1))
def test_every_question_has_exactly_one_correct_option_among_its_options(
    inputs: tuple[list[QuizPlayer], list[PlayerMatchStats]], seed: int
) -> None:
    roster, stats = inputs
    questions = QuestionGenerator().generate(roster, stats, question_count=5, seed=seed)
    ids = {p.user_id for p in roster}
    compared = {p.user_id for p in roster if not p.not_enough_matches}
    for question in questions:
        option_ids = [option.option_id for option in question.options]
        assert set(option_ids) == ids
        assert option_ids.count(question.correct_option_id) == 1
        # Independent semantic oracle: verify the question, not just its answer ID.
        members = {
            r.match_id: {other.user_id for other in stats if other.match_id == r.match_id}
            for r in stats
        }
        selected = stats
        if "without other room players" in question.text:
            selected = [r for r in stats if len(members[r.match_id]) == 1]
        elif "with other room players" in question.text:
            selected = [r for r in stats if len(members[r.match_id]) >= 2]
        scores = {}
        for uid in compared:
            own = [r for r in selected if r.user_id == uid]
            assert own
            if question.template_id == "most-kills-v1":
                scores[uid] = Fraction(max(r.kills for r in own))
            elif question.template_id == "highest-acs-v1":
                assert all(
                    "combat_score" in r.numbers and "rounds_played" in r.numbers for r in own
                )
                scores[uid] = sum(Fraction(r.numbers["combat_score"]) for r in own) / sum(
                    Fraction(r.numbers["rounds_played"]) for r in own
                )
            else:
                assert question.template_id == "recent-wins-v1"
                assert len(own) >= 10
                recent = sorted(own, key=lambda r: r.played_at.seconds, reverse=True)[:10]
                assert all(r.labels.get("result") in {"win", "loss", "draw"} for r in recent)
                scores[uid] = Fraction(sum(r.won for r in recent))
        winners = [uid for uid, score in scores.items() if score == max(scores.values())]
        assert winners == [question.correct_option_id]
    assert len(template_ids(questions)) == len(set(template_ids(questions)))
    assert len({q.question_id for q in questions}) == len(questions)
    assert questions == QuestionGenerator().generate(roster[::-1], stats[::-1], 5, seed)


def test_nonempty_quiz_has_three_templates_and_mixes_shared_and_solo() -> None:
    questions = QuestionGenerator().generate(players(), history(), 5, 42)
    assert len(questions) == 3
    assert all(q.correct_option_id == "a" for q in questions)
    assert any("with other room players" in q.text for q in questions)
    assert any("without other room players" in q.text for q in questions)


def test_every_question_names_its_template() -> None:
    questions = QuestionGenerator().generate(players(), history(), 5, 42)
    assert sorted(template_ids(questions)) == sorted(t.id for t in TEMPLATES)
    for question in questions:
        assert len(question.question_id) == 64
        assert not question.question_id.startswith(question.template_id)


def test_outsiders_cannot_change_questions_or_leak_into_text_or_explanations() -> None:
    stats = history()
    original = [r.SerializeToString(deterministic=True) for r in stats]
    outsider = row("OUTSIDER_ID", "a-9", kills=999999, score=987654321)
    outsider.labels.update({"agent": "OUTSIDER_NAME", "map": "SECRET_MAP"})
    generator = QuestionGenerator()
    expected = generator.generate(players(), stats, 3, 7)
    actual = generator.generate(players(), [*stats, outsider], 3, 7)
    assert actual == expected
    assert actual
    for question in actual:
        rendered = question.text + question.explanation
        for secret in ("OUTSIDER_ID", "OUTSIDER_NAME", "SECRET_MAP", "999999", "987654321"):
            assert secret not in rendered
    assert original == [r.SerializeToString(deterministic=True) for r in stats]


def test_play_again_with_every_template_used_is_still_full_oldest_first() -> None:
    generator = QuestionGenerator()
    first = generator.generate(players(), history(), 3, 1)
    asked = template_ids(first)
    for seed in (0, 1, 42):
        again = generator.generate(players(), history(), 3, seed, asked_template_ids=asked)
        assert template_ids(again) == template_ids(first)
    assert generator.generate(players(), history(), 3, 1) == first


@pytest.mark.parametrize("seed", range(10))
def test_a_used_template_is_skipped_in_every_scope_while_others_are_left(seed: int) -> None:
    questions = QuestionGenerator([KILLS, ACS]).generate(
        players(), history(), 1, seed, asked_template_ids=[KILLS.id]
    )
    assert template_ids(questions) == [ACS.id]


def test_template_asked_longest_ago_is_reused_first() -> None:
    generator = QuestionGenerator([replace(t, scopes=("all",)) for t in TEMPLATES])
    # Most kills was asked first but asked again last, so it's the most recent.
    asked = [KILLS.id, ACS.id, WINS.id, KILLS.id]
    for seed in range(10):
        questions = generator.generate(players(), history(), 2, seed, asked_template_ids=asked)
        assert template_ids(questions) == [ACS.id, WINS.id]


def test_play_again_rotates_through_every_template() -> None:
    # Real scopes: recent wins has no shared/solo version, so mixing scopes mustn't
    # keep swapping it out once it's the template asked longest ago.
    generator = QuestionGenerator()
    asked: list[str] = []
    last_asked: dict[str, int] = {}
    for quiz in range(7):
        questions = generator.generate(players(), history(), 2, quiz, asked_template_ids=asked)
        unused = [t.id for t in TEMPLATES if t.id not in last_asked]
        longest_ago = sorted(last_asked, key=lambda t: last_asked[t])
        if len(unused) >= 2:
            assert set(template_ids(questions)) <= set(unused)
        else:
            assert set(template_ids(questions)) == {*unused, *longest_ago[: 2 - len(unused)]}
        # The room appends what it asked, so the list stays ordered by when last asked.
        for question in questions:
            asked.append(question.template_id)
            last_asked[question.template_id] = len(asked)


def test_same_fact_with_different_template_wording_is_not_repeated() -> None:
    template = replace(KILLS, scopes=("all",))
    generator = QuestionGenerator([template, replace(template, id="paraphrase")])
    assert len(generator.generate(players(), history(), 2, 3)) == 1


def test_ties_and_empty_history_produce_no_questions() -> None:
    generator = QuestionGenerator()
    assert generator.generate(players(), [], 3, 0) == []
    assert generator.generate(players(), [row("a", "x"), row("b", "x")], 3, 0) == []


def test_missing_player_does_not_become_zero() -> None:
    assert QuestionGenerator().generate(players(), [row("a", "x", kills=50)], 3, 0) == []


def test_pooled_acs_uses_round_weighting_not_mean_of_match_acs() -> None:
    stats = [
        row("a", "a1", score=100, rounds=1),
        row("a", "a2", score=0, rounds=99),
        row("b", "b1", score=20, rounds=10),
    ]
    questions = QuestionGenerator([ACS]).generate(players(), stats, 1, 0)
    assert len(questions) == 1
    assert questions[0].correct_option_id == "b"
    assert "20 combat score over 10 rounds" in questions[0].explanation


@pytest.mark.parametrize("bad", [None, float("nan"), float("inf"), -1])
def test_missing_or_invalid_combat_score_skips_comparison(bad: float | None) -> None:
    stats = [row("a", "x", score=100), row("b", "x", score=200)]
    if bad is None:
        del stats[1].numbers["combat_score"]
    else:
        stats[1].numbers["combat_score"] = bad
    assert QuestionGenerator([ACS]).generate(players(), stats, 1, 0) == []


def test_zero_rounds_skips_acs_and_zero_kills_are_valid() -> None:
    stats = [row("a", "x", kills=1, rounds=0), row("b", "x", kills=0)]
    questions = QuestionGenerator().generate(players(), stats, 3, 0)
    assert len(questions) == 1
    assert questions[0].correct_option_id == "a"


def test_recent_wins_filters_queue_before_taking_latest_ten_and_counts_draws() -> None:
    stats = history()
    for r in stats:
        if r.user_id == "a":
            r.won = False
            r.labels["result"] = "draw"
        else:
            r.won = True
            r.labels["result"] = "win"
    extra = row("a", "new-unrated", won=True, seconds=100)
    extra.labels["queue_id"] = "unrated"
    stats.append(extra)
    questions = QuestionGenerator([WINS]).generate(players(), stats, 1, 0)
    assert len(questions) == 1
    assert questions[0].correct_option_id == "b"
    assert "won 10 of their last 10" in questions[0].text


@pytest.mark.parametrize(
    "case", ["short", "missing-time", "missing-result", "inconsistent", "boundary-tie"]
)
def test_recent_wins_requires_complete_unambiguous_window(case: str) -> None:
    stats = history()
    if case == "short":
        stats.pop()
    elif case == "missing-time":
        stats[0].ClearField("played_at")
    elif case == "missing-result":
        del stats[0].labels["result"]
    elif case == "inconsistent":
        stats[0].won = False
    else:
        stats.append(row("a", "extra", seconds=1))
    assert QuestionGenerator([WINS]).generate(players(), stats, 1, 0) == []


def test_recent_wins_does_not_include_older_matches() -> None:
    stats = [*history(), row("b", "old", won=True, seconds=0)]
    assert QuestionGenerator([WINS]).generate(players(), stats, 1, 0)[0].correct_option_id == "a"


def test_unknown_queue_is_not_silently_discarded() -> None:
    stats = history()
    del stats[0].labels["queue_id"]
    assert QuestionGenerator().generate(players(), stats, 3, 0) == []


def test_duplicate_rows_do_not_count_twice_and_conflicts_are_rejected() -> None:
    stats = history()
    generator = QuestionGenerator()
    assert generator.generate(players(), stats * 2, 3, 0) == generator.generate(
        players(), stats, 3, 0
    )
    conflict = PlayerMatchStats()
    conflict.CopyFrom(stats[0])
    conflict.kills += 1
    with pytest.raises(ValueError, match="Conflicting"):
        generator.generate(players(), [*stats, conflict], 3, 0)


@pytest.mark.parametrize("roster", [[], players()[:1], players() * 2, [QuizPlayer(), players()[0]]])
def test_invalid_roster_is_rejected(roster: list[QuizPlayer]) -> None:
    with pytest.raises(ValueError, match="unique nonempty"):
        QuestionGenerator().generate(roster, [], 1, 0)


def test_counts_and_duplicate_template_ids() -> None:
    generator = QuestionGenerator()
    assert generator.generate(players(), history(), 0, 0) == []
    assert len(generator.generate(players(), history(), 1, 0)) == 1
    with pytest.raises(ValueError, match="nonnegative"):
        generator.generate(players(), history(), -1, 0)
    with pytest.raises(ValueError, match="Template IDs must be unique"):
        QuestionGenerator([KILLS, KILLS])


def test_questions_have_no_weekdays_or_wall_clock_dependency() -> None:
    questions = QuestionGenerator().generate(players(), history(), 3, 0)
    assert questions
    for q in questions:
        for weekday in (
            "Monday",
            "Tuesday",
            "Wednesday",
            "Thursday",
            "Friday",
            "Saturday",
            "Sunday",
        ):
            assert weekday not in q.text + q.explanation


def test_fifty_templates_finish_in_under_three_seconds() -> None:
    # Each template has three viable scopes; enumeration would try 4**50 choices.
    templates = [replace(KILLS, id=f"kills-{n}", game=f"game-{n}") for n in range(50)]
    stats = []
    for template in templates:
        for match in history():
            match.game = template.game
            stats.append(match)
    start = perf_counter()
    questions = QuestionGenerator(templates).generate(players(), stats, 50, 42)
    elapsed = perf_counter() - start
    assert elapsed < 3
    assert len(questions) == 50
    assert len(set(template_ids(questions))) == 50
    assert any("with other room players" in q.text for q in questions)
    assert any("without other room players" in q.text for q in questions)


def test_reuse_still_rejects_repeated_facts() -> None:
    templates = [replace(KILLS, id=f"wording-{n}", scopes=("all",)) for n in range(3)]
    questions = QuestionGenerator(templates).generate(
        players(), history(), 3, 42, asked_template_ids=[t.id for t in templates]
    )
    assert template_ids(questions) == ["wording-0"]


def test_question_key_survives_sync_but_changes_with_correct_answer() -> None:
    generator = QuestionGenerator([replace(KILLS, scopes=("all",))])
    stats = history()
    first = generator.generate(players(), stats, 1, 0)[0]
    stats.append(row("b", "new-match", kills=20, seconds=100))
    same = generator.generate(players(), stats, 1, 0)[0]
    assert same.question_id == first.question_id
    stats[-1].kills = 50
    changed = generator.generate(players(), stats, 1, 0)[0]
    assert changed.correct_option_id == "b"
    assert changed.question_id != first.question_id


def test_question_key_changes_when_value_in_question_text_changes() -> None:
    template = replace(KILLS, scopes=("all",), question="Who recorded {value} kills?")
    generator = QuestionGenerator([template])
    stats = history()
    first = generator.generate(players(), stats, 1, 0)[0]
    stats.append(row("a", "new-record", kills=40))
    changed = generator.generate(players(), stats, 1, 0)[0]
    assert changed.correct_option_id == first.correct_option_id
    assert changed.question_id != first.question_id


@pytest.mark.parametrize("seed", range(10))
def test_scope_repair_keeps_templates_and_facts_unique(seed: int) -> None:
    # Different rules allow a full two-question quiz with both scopes.
    questions = QuestionGenerator([KILLS, ACS]).generate(players(), history(), 2, seed)
    assert len(questions) == 2
    assert len(set(template_ids(questions))) == 2
    assert any("with other room players" in q.text for q in questions)
    assert any("without other room players" in q.text for q in questions)


def test_new_template_precedes_reused_ones_when_history_is_partly_used() -> None:
    generator = QuestionGenerator([replace(t, scopes=("all",)) for t in TEMPLATES])
    actual = generator.generate(players(), history(), 3, 42, asked_template_ids=[KILLS.id, ACS.id])
    assert template_ids(actual) == [WINS.id, KILLS.id, ACS.id]


def test_scope_repair_on_reused_templates_keeps_age_order_and_unique_templates() -> None:
    # Every scope of both templates was asked; highest ACS was last asked before most kills.
    asked = [ACS.id, KILLS.id] * 3
    questions = QuestionGenerator([KILLS, ACS]).generate(
        players(), history(), 2, 42, asked_template_ids=asked
    )
    assert any("with other room players" in q.text for q in questions)
    assert any("without other room players" in q.text for q in questions)
    assert template_ids(questions) == [ACS.id, KILLS.id]


# Difficulty: easy, medium and hard pools sized by quiz length.


@pytest.mark.parametrize(
    ("best", "runner_up", "expected"),
    [
        (20, 13, "easy"),  # 35% lead, the easy cutoff
        (20, 10, "easy"),
        (1, 0, "easy"),  # a friend's only ace
        (20, 14, "medium"),  # 30%
        (20, 17, "medium"),  # 15%, the medium cutoff
        (Fraction(20), Fraction(171, 10), "hard"),  # 14.5%
        (20, 19, "hard"),
    ],
)
def test_difficulty_comes_from_the_winners_lead(
    best: Fraction, runner_up: Fraction, expected: str
) -> None:
    assert difficulty(Fraction(best), Fraction(runner_up)) == expected


@pytest.mark.parametrize(
    ("count", "easy", "medium", "hard"),
    [
        (5, 3, 2, 0),
        (7, 5, 2, 0),
        (8, 4, 3, 1),
        (10, 4, 4, 2),
        (12, 5, 5, 2),
        (13, 4, 6, 3),
        (15, 5, 6, 4),
        (20, 6, 8, 6),
    ],
)
def test_quiz_length_sets_the_mix(count: int, easy: int, medium: int, hard: int) -> None:
    assert quiz_mix(count) == {"easy": easy, "medium": medium, "hard": hard}


@given(count=st.integers(0, 60))
def test_mix_always_adds_up_to_the_quiz_length(count: int) -> None:
    mix = quiz_mix(count)
    assert sum(mix.values()) == count
    assert all(n >= 0 for n in mix.values())


def test_arrange_puts_the_hardest_questions_in_the_middle() -> None:
    levels: list[Difficulty] = ["easy"] * 4 + ["medium"] * 4 + ["hard"] * 2
    order = arrange(levels)
    assert [levels[i][0].upper() for i in order] == list("EEMMHHMMEE")
    # Questions of one difficulty keep their order.
    for level in LEVELS:
        mine = [i for i in order if levels[i] == level]
        assert mine == sorted(mine)
    assert arrange([]) == []
    assert arrange(["hard"]) == [0]


def test_quiz_takes_each_level_from_its_own_pool_and_puts_hard_in_the_middle() -> None:
    templates, stats = rated(pools(easy=6, medium=6, hard=6))
    for seed in range(5):
        questions = QuestionGenerator(templates).generate(players(), stats, 10, seed)
        assert shape(questions) == "EEMMHHMMEE"


def test_short_quiz_uses_only_easy_and_medium() -> None:
    templates, stats = rated(pools(easy=6, medium=6, hard=6))
    questions = QuestionGenerator(templates).generate(players(), stats, 5, 3)
    assert sorted(levels_of(questions)) == ["easy", "easy", "easy", "medium", "medium"]


def test_short_pool_borrows_from_the_nearest_level_and_stays_full_length() -> None:
    # A 10-question quiz wants 4 easy, 4 medium and 2 hard. With no hard questions,
    # hard borrows from medium (none spare after its own 3), then from easy.
    templates, stats = rated(pools(easy=7, medium=3))
    questions = QuestionGenerator(templates).generate(players(), stats, 10, 0)
    assert shape(questions) == "EEEMMMEEEE"


def test_hard_borrows_medium_before_easy() -> None:
    templates, stats = rated(pools(easy=6, medium=6))
    questions = QuestionGenerator(templates).generate(players(), stats, 10, 0)
    # 4 easy, then medium's 4 plus hard's 2 borrowed from medium.
    assert sorted(levels_of(questions)) == ["easy"] * 4 + ["medium"] * 6


def test_unused_templates_fill_every_level_before_a_template_is_reused() -> None:
    # Two easy slots: the used easy template waits while an unused medium one can fill in.
    templates, stats = rated({"easy-old": 50, "easy-new": 50, "medium-new": 25})
    questions = QuestionGenerator(templates).generate(
        players(), stats, 2, 0, asked_template_ids=["easy-old"]
    )
    assert sorted(template_ids(questions)) == ["easy-new", "medium-new"]
