"""The template interface for question packs: rules, windows, parameters and checks."""

from dataclasses import replace
from fractions import Fraction

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question, QuizPlayer

from quiz_engine.question_generator import QuestionGenerator, difficulty
from quiz_engine.templates import QuestionTemplate

ALICE, BOB, CARA = "a", "b", "c"
NAMES = {ALICE: "Alice", BOB: "Bob", CARA: "Cara"}


def players(*ids: str) -> list[QuizPlayer]:
    return [QuizPlayer(user_id=uid, display_name=NAMES[uid]) for uid in ids]


def match(
    uid: str,
    match_id: str,
    *,
    seconds: int = 1,
    kills: int = 10,
    deaths: int = 10,
    agent: str | None = "Jett",
    team: str = "Red",
    **numbers: float,
) -> PlayerMatchStats:
    labels = {"queue_id": "competitive", "team_id": team}
    if agent is not None:
        labels["agent"] = agent
    row = PlayerMatchStats(
        user_id=uid,
        match_id=match_id,
        game="valorant",
        kills=kills,
        deaths=deaths,
        labels=labels,
        numbers=numbers,
    )
    row.played_at.seconds = seconds
    return row


def template(**fields: object) -> QuestionTemplate:
    base: dict[str, object] = {
        "id": "t",
        "question": "Who? {parameter}",
        "explanation": "{name}: {value} ({numerator}/{denominator})",
        "stats": (),
        "last_n": None,
    }
    base.update(fields)
    return QuestionTemplate(**base)  # type: ignore[arg-type]


def ask(t: QuestionTemplate, stats: list[PlayerMatchStats], *ids: str) -> list[Question]:
    """Every question the template can make.

    A quiz takes one question per template, picked by seed and by which difficulty
    the quiz length wants, so try lengths that want easy, medium and hard questions.
    """
    found: dict[str, Question] = {}
    for count in (1, 3, 8):
        for seed in range(10):
            for question in QuestionGenerator([t]).generate(players(*ids), stats, count, seed):
                found[question.text] = question
    return sorted(found.values(), key=lambda q: q.text)


def test_pooled_ratio_counts_a_deathless_match_but_skips_a_zero_total() -> None:
    kd = template(rule="pooled_ratio", numerator=("kills",), denominator=("deaths",))
    stats = [
        match(ALICE, "1", kills=10, deaths=0),
        match(ALICE, "2", kills=5, deaths=5),
        match(BOB, "3", kills=6, deaths=3),
    ]
    [question] = ask(kd, stats, ALICE, BOB)
    assert question.correct_option_id == ALICE
    assert question.explanation == "Alice: 3 (15/5)"
    stats[2].deaths = 0
    assert ask(kd, stats, ALICE, BOB) == []


@pytest.mark.parametrize(
    ("kills", "deaths", "shown"),
    [
        (23, 18, "1.28 kills per death, 127.8%"),
        (20, 10, "2 kills per death, 200%"),
        # 0.0625: halves round up.
        (1, 16, "0.06 kills per death, 6.3%"),
        (1999, 1000, "2.00 kills per death, 199.9%"),
    ],
)
def test_values_show_two_decimals_and_percentages_one(kills: int, deaths: int, shown: str) -> None:
    kd = template(
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("deaths",),
        explanation="{value} kills per death, {percent}%",
    )
    stats = [match(ALICE, "1", kills=kills, deaths=deaths), match(BOB, "2", kills=0, deaths=10)]
    [question] = ask(kd, stats, ALICE, BOB)
    assert question.explanation == shown


def test_a_number_in_the_question_must_single_out_the_winner() -> None:
    stats = [
        # 1.281 and 1.279 both show as 1.28; 128.1% and 127.9% don't.
        match(ALICE, "1", kills=1281, deaths=1000),
        match(BOB, "2", kills=1279, deaths=1000),
    ]

    def kd(question: str) -> QuestionTemplate:
        return template(
            rule="pooled_ratio", numerator=("kills",), denominator=("deaths",), question=question
        )

    assert ask(kd("Who got {value} kills per death?"), stats, ALICE, BOB) == []
    assert ask(kd("Who got {percent}% as many kills as deaths?"), stats, ALICE, BOB)
    # A number shown only in the explanation doesn't need to.
    [question] = ask(kd("Who got the most kills per death?"), stats, ALICE, BOB)
    assert question.correct_option_id == ALICE


def test_max_in_match_takes_each_players_best_match_and_lowest_can_win() -> None:
    stats = [
        match(ALICE, "1", deaths=4),
        match(ALICE, "2", deaths=20),
        match(BOB, "3", deaths=6),
    ]
    most = template(rule="max_in_match", stat="deaths")
    fewest = template(rule="max_in_match", stat="deaths", direction="lowest")
    assert ask(most, stats, ALICE, BOB)[0].correct_option_id == ALICE
    [question] = ask(fewest, stats, ALICE, BOB)
    assert question.correct_option_id == ALICE
    assert question.source_match_id == "1"


def test_average_is_per_match() -> None:
    performance = template(rule="average", stat="numbers.performance_score")
    stats = [
        match(ALICE, "1", performance_score=300),
        match(ALICE, "2", performance_score=100),
        match(BOB, "3", performance_score=190),
    ]
    [question] = ask(performance, stats, ALICE, BOB)
    assert question.correct_option_id == ALICE
    assert question.explanation.startswith("Alice: 200")


def test_one_question_per_agent_with_known_zero_counts() -> None:
    games = template(rule="count", stat="matches", parameter_label="agent")
    stats = [
        *(match(ALICE, f"a{n}", agent="Jett") for n in range(3)),
        match(ALICE, "a3", agent="Sage"),
        match(BOB, "b0", agent="Jett"),
        match(BOB, "b1", agent="Sage"),
        match(BOB, "b2", agent="Sage"),
        match(CARA, "c0", agent="Jett"),
    ]
    questions = {q.text: q.correct_option_id for q in ask(games, stats, ALICE, BOB, CARA)}
    # Cara never played Sage: a known zero, not missing data.
    assert questions == {"Who? Jett": ALICE, "Who? Sage": BOB}


def test_ratio_on_an_agent_someone_never_played_is_skipped() -> None:
    kd = template(
        rule="pooled_ratio", numerator=("kills",), denominator=("deaths",), parameter_label="agent"
    )
    stats = [
        match(ALICE, "1", agent="Jett", kills=20),
        match(ALICE, "2", agent="Sage", kills=20),
        match(BOB, "3", agent="Jett", kills=5),
    ]
    assert [q.text for q in ask(kd, stats, ALICE, BOB)] == ["Who? Jett"]


def test_contenders_only_compares_just_the_players_who_played_the_agent() -> None:
    casts = template(
        rule="pooled_ratio",
        numerator=("numbers.ability_casts_ability2",),
        denominator=("numbers.rounds_played",),
        parameter_label="agent",
        contenders_only=True,
    )
    stats = [
        match(ALICE, "1", agent="Sova", ability_casts_ability2=30, rounds_played=20),
        match(BOB, "2", agent="Sova", ability_casts_ability2=10, rounds_played=20),
        match(CARA, "3", agent="Jett", ability_casts_ability2=99, rounds_played=20),
    ]
    [sova] = [q for q in ask(casts, stats, ALICE, BOB, CARA) if q.text.endswith("Sova")]
    assert sova.correct_option_id == ALICE
    assert [o.option_id for o in sova.options] == [ALICE, BOB]
    # Only Cara played Jett, so there is nothing to compare.
    assert not [q for q in ask(casts, stats, ALICE, BOB, CARA) if q.text.endswith("Jett")]


def test_window_is_chosen_before_the_agent_filter() -> None:
    games = template(rule="count", stat="matches", parameter_label="agent", last_n=3)
    alice = [match(ALICE, f"new{n}", seconds=10 + n, agent="Jett") for n in range(3)]
    alice += [match(ALICE, f"old{n}", seconds=n, agent="Sage") for n in range(2)]
    bob = [
        match(BOB, f"b{n}", seconds=10 + n, agent="Sage" if n == 0 else "Jett") for n in range(3)
    ]
    questions = {q.text: q.correct_option_id for q in ask(games, [*alice, *bob], ALICE, BOB)}
    # Alice's two Sage games are older than her last 3, so Bob's one Sage game wins.
    assert questions == {"Who? Jett": ALICE, "Who? Sage": BOB}


def test_a_match_missing_the_parameter_label_skips_the_question() -> None:
    games = template(rule="count", stat="matches", parameter_label="agent")
    stats = [match(ALICE, "1"), match(ALICE, "2"), match(BOB, "3", agent=None)]
    assert ask(games, stats, ALICE, BOB) == []


def test_display_label_names_the_winning_match() -> None:
    rank = template(
        rule="max_in_match",
        stat="numbers.rank_tier",
        display_label="rank",
        question="Who hit {rank_name}?",
    )
    stats = [
        match(ALICE, "1", rank_tier=15),
        match(ALICE, "2", rank_tier=18),
        match(BOB, "3", rank_tier=16),
    ]
    stats[0].labels["rank"] = "Platinum 1"
    stats[1].labels["rank"] = "Diamond 1"
    stats[2].labels["rank"] = "Platinum 2"
    [question] = ask(rank, stats, ALICE, BOB)
    assert question.text == "Who hit Diamond 1?"
    del stats[1].labels["rank"]
    assert ask(rank, stats, ALICE, BOB) == []


def test_pair_questions_compare_the_friends_teammates_by_name() -> None:
    assists = template(
        rule="pair",
        stat="numbers.assists_for",
        question="Who sets up {friend} the most?",
        explanation="{name}: {value}",
    )
    stats = [
        match(ALICE, "1"),
        match(BOB, "1", **{"assists_for:a": 5}),
        match(CARA, "1", **{"assists_for:a": 2}),
        # Alice and Cara on opposite teams: Cara has no pair key for Alice there, and
        # that match doesn't count towards setting Alice up.
        match(ALICE, "2", team="Red"),
        match(CARA, "2", team="Blue"),
    ]
    questions = ask(assists, stats, ALICE, BOB, CARA)
    [for_alice] = [q for q in questions if q.text == "Who sets up Alice the most?"]
    assert for_alice.correct_option_id == BOB
    assert [o.option_id for o in for_alice.options] == [BOB, CARA]
    assert all("a" not in q.text.split() for q in questions)
    # A missing pair key in a match together is unknown, not zero.
    del stats[2].numbers["assists_for:a"]
    assert not [q for q in ask(assists, stats, ALICE, BOB, CARA) if "Alice" in q.text]


def test_a_pack_entry_using_the_defaults_needs_ten_matches_each() -> None:
    kd = QuestionTemplate(
        id="highest-recent-kd-v1",
        question="This player got {value} kills per death over their last {last_n} games, "
        "the best in the group. Who is it?",
        explanation="{name} had {numerator} kills / {denominator} deaths: {value} kills per death.",
        stats=("kills", "deaths"),
        rule="pooled_ratio",
        numerator=("kills",),
        denominator=("deaths",),
    )
    stats = [
        match(uid, f"{uid}{n}", seconds=n, kills=20 if uid == ALICE else 10)
        for uid in (ALICE, BOB)
        for n in range(10)
    ]
    [question] = ask(kd, stats, ALICE, BOB)
    assert question.correct_option_id == ALICE
    assert "their last 10 games" in question.text
    assert ask(kd, stats[1:], ALICE, BOB) == []


def squad_with_a_few_solo_games() -> list[PlayerMatchStats]:
    """Alice, Bob and Cara mostly play together; Alice and Bob also have solo games."""
    together = [match(uid, f"squad{n}", seconds=n) for n in range(5) for uid in (ALICE, BOB, CARA)]
    solo = [
        match(ALICE, "alice-solo-1", seconds=10, kills=30),
        match(ALICE, "alice-solo-2", seconds=11, kills=12),
        match(BOB, "bob-solo-1", seconds=12, kills=20),
    ]
    return [*together, *solo]


def test_solo_questions_compare_the_friends_who_have_solo_games() -> None:
    kills = template(rule="max_in_match", stat="kills", scopes=("solo",), contenders_only=True)
    [question] = ask(kills, squad_with_a_few_solo_games(), ALICE, BOB, CARA)
    assert question.correct_option_id == ALICE
    assert [o.option_id for o in question.options] == [ALICE, BOB]
    # Without contenders_only, Cara's missing solo games skip the question.
    assert (
        ask(replace(kills, contenders_only=False), squad_with_a_few_solo_games(), ALICE, BOB, CARA)
        == []
    )


def test_contenders_only_lets_a_friend_with_too_few_matches_sit_out() -> None:
    kills = template(
        rule="max_in_match", stat="kills", scopes=("solo",), contenders_only=True, last_n=2
    )
    # Alice has 2 solo games, Bob only 1: Bob sits out, leaving Alice alone.
    assert ask(kills, squad_with_a_few_solo_games(), ALICE, BOB, CARA) == []
    stats = [*squad_with_a_few_solo_games(), match(BOB, "bob-solo-2", seconds=13, kills=40)]
    [question] = ask(kills, stats, ALICE, BOB, CARA)
    assert question.correct_option_id == BOB


def test_lowest_wins_difficulty_uses_the_runner_up() -> None:
    assert difficulty(Fraction(10), Fraction(20), "lowest") == "easy"
    assert difficulty(Fraction(10), Fraction(11), "lowest") == "hard"


@pytest.mark.parametrize(
    ("fields", "problem"),
    [
        ({"rule": "pooled_ratio", "numerator": ("kills",)}, "numerator and denominator"),
        ({"rule": "max_in_match"}, "needs a stat"),
        ({"rule": "count", "stat": "kd"}, "stats must be"),
        ({"rule": "count", "stat": "kills", "display_label": "rank"}, "display_label"),
        ({"rule": "pair", "stat": "kills"}, "pair needs"),
        ({"rule": "count", "stat": "kills", "last_n": 0}, "last_n"),
        ({"rule": "count", "stat": "kills", "question": "Who has {rank}?"}, "{rank}"),
    ],
)
def test_templates_the_generator_cant_evaluate_fail_at_load(
    fields: dict[str, object], problem: str
) -> None:
    with pytest.raises(ValueError, match="Template t:") as error:
        QuestionGenerator([template(**fields)])
    assert problem in str(error.value)
