"""The template interface for question packs: rules, windows, parameters and checks."""

from collections.abc import Collection
from dataclasses import replace
from fractions import Fraction

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question, QuizPlayer

from quiz_engine.question_generator import _SCOPE_TEXT, QuestionGenerator, difficulty
from quiz_engine.templates import TEMPLATES, QuestionTemplate
from tests.unit.test_question_generator import template as pack_template

ALICE, BOB, CARA = "a", "b", "c"
NAMES = {ALICE: "Alice", BOB: "Bob", CARA: "Cara"}


def players(*ids: str, short: Collection[str] = ()) -> list[QuizPlayer]:
    """The room; players in `short` have too few matches (not_enough_matches)."""
    return [
        QuizPlayer(user_id=uid, display_name=NAMES[uid], not_enough_matches=uid in short)
        for uid in ids
    ]


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


def ask(
    t: QuestionTemplate,
    stats: list[PlayerMatchStats],
    *ids: str,
    short: Collection[str] = (),
) -> list[Question]:
    """Every question the template can make.

    A quiz takes one question per template, picked by seed and by which difficulty
    the quiz length wants, so try lengths that want easy, medium and hard questions.
    """
    found: dict[str, Question] = {}
    room = players(*ids, short=short)
    for count in (1, 3, 8):
        for seed in range(10):
            for question in QuestionGenerator([t]).generate(room, stats, count, seed):
                found[question.text] = question
    return sorted(found.values(), key=lambda q: q.text)


def said(question: Question) -> str:
    """The explanation's first sentence, about the answer (the runner-up's comes next)."""
    return question.explanation.split(" Next closest: ")[0]


def option_ids(question: Question) -> list[str]:
    return [option.option_id for option in question.options]


def test_pooled_ratio_counts_a_deathless_match_but_skips_a_zero_total() -> None:
    kd = template(rule="pooled_ratio", numerator=("kills",), denominator=("deaths",))
    stats = [
        match(ALICE, "1", kills=10, deaths=0),
        match(ALICE, "2", kills=5, deaths=5),
        match(BOB, "3", kills=6, deaths=3),
    ]
    [question] = ask(kd, stats, ALICE, BOB)
    assert question.correct_option_id == ALICE
    assert said(question) == "Alice: 3 (15/5)"
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
    assert said(question) == shown


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


def test_skip_missing_leaves_out_matches_without_the_stat() -> None:
    # Bob's second match had no kills, so it has no longest kill.
    stats = [
        match(ALICE, "1", longest_kill_m=40),
        match(BOB, "2", longest_kill_m=30),
        match(BOB, "3"),
    ]
    strict = template(rule="max_in_match", stat="numbers.longest_kill_m")
    assert ask(strict, stats, ALICE, BOB) == []
    lenient = replace(strict, skip_missing=True)
    [question] = ask(lenient, stats, ALICE, BOB)
    assert question.correct_option_id == ALICE
    assert said(question) == "Alice: 40 (40/1)"
    # A player with no match that has the stat is still unknown.
    assert ask(lenient, [match(ALICE, "1", longest_kill_m=40), match(BOB, "3")], ALICE, BOB) == []


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
        match(BOB, "b3", agent="Omen"),
        match(CARA, "c0", agent="Jett"),
        *(match(CARA, f"c{n}", agent="Omen") for n in range(1, 4)),
    ]
    questions = {q.text: q.correct_option_id for q in ask(games, stats, ALICE, BOB, CARA)}
    # Cara never played Sage: a known zero, not missing data.
    assert questions == {"Who? Jett": ALICE, "Who? Sage": BOB, "Who? Omen": CARA}


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
    # Cara never played Sova and sits it out, but every room player is an option.
    assert option_ids(sova) == [ALICE, BOB, CARA]
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
        # Two matches each, so every window is whole (see the equal-window tests).
        match(BOB, "3", team="Blue"),
    ]
    questions = ask(assists, stats, ALICE, BOB, CARA)
    [for_alice] = [q for q in questions if q.text == "Who sets up Alice the most?"]
    assert for_alice.correct_option_id == BOB
    # Alice is the friend, so never the answer, but still an option.
    assert option_ids(for_alice) == [ALICE, BOB, CARA]
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
    # Cara has no solo games and sits it out, but is still an option.
    assert option_ids(question) == [ALICE, BOB, CARA]
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


def test_a_player_with_too_few_matches_is_an_option_but_never_compared() -> None:
    kills = template(rule="max_in_match", stat="kills")
    stats = [match(ALICE, "1", kills=20), match(BOB, "2", kills=10), match(CARA, "3", kills=99)]
    [question] = ask(kills, stats, ALICE, BOB, CARA, short={CARA})
    # Cara's 99 kills don't count, so Alice wins with 20.
    assert question.correct_option_id == ALICE
    assert option_ids(question) == [ALICE, BOB, CARA]


def test_missing_data_from_a_player_with_too_few_matches_skips_nothing() -> None:
    kd = template(rule="pooled_ratio", numerator=("kills",), denominator=("deaths",))
    stats = [match(ALICE, "1", kills=20, deaths=10), match(BOB, "2", kills=10, deaths=10)]
    # Cara has no matches: a compared player without data skips the question...
    assert ask(kd, stats, ALICE, BOB, CARA) == []
    # ...but a player with too few matches isn't compared, so it goes ahead.
    [question] = ask(kd, stats, ALICE, BOB, CARA, short={CARA})
    assert question.correct_option_id == ALICE
    assert option_ids(question) == [ALICE, BOB, CARA]


def test_matches_with_a_player_with_too_few_matches_still_count_as_shared() -> None:
    kills = template(rule="max_in_match", stat="kills", scopes=("solo",), contenders_only=True)
    stats = [
        match(ALICE, "with-cara", kills=30),
        match(CARA, "with-cara"),
        match(ALICE, "alice-solo", kills=12),
        match(BOB, "bob-solo", kills=20),
    ]
    # Alice's 30-kill game was with Cara, so it isn't a solo game.
    [question] = ask(kills, stats, ALICE, BOB, CARA, short={CARA})
    assert question.correct_option_id == BOB


def test_a_player_with_too_few_matches_is_never_a_pair_friend() -> None:
    assists = template(
        rule="pair",
        stat="numbers.assists_for",
        question="Who sets up {friend}?",
        explanation="{name}",
    )
    stats = [
        match(ALICE, "1"),
        match(BOB, "1", **{"assists_for:a": 5}),
        match(CARA, "1", **{"assists_for:a": 2}),
    ]
    assert [q.text for q in ask(assists, stats, ALICE, BOB, CARA)] == ["Who sets up Alice?"]
    assert ask(assists, stats, ALICE, BOB, CARA, short={ALICE}) == []


def test_fewer_than_two_players_with_enough_matches_make_no_questions() -> None:
    kills = template(rule="max_in_match", stat="kills")
    stats = [match(ALICE, "1", kills=20), match(BOB, "2", kills=10)]
    assert ask(kills, stats, ALICE, BOB, short={BOB}) == []


def test_lowest_wins_difficulty_uses_the_runner_up() -> None:
    assert difficulty(Fraction(10), Fraction(20), "lowest") == "easy"
    assert difficulty(Fraction(10), Fraction(11), "lowest") == "hard"


def test_plural_forms_follow_the_shown_value() -> None:
    counted = template(
        rule="count",
        stat="numbers.ninja_defuses",
        question="Who defused {value} {time|times}?",
        explanation="{name}: {value} {defuse|defuses}",
    )
    stats = [match(ALICE, "1", ninja_defuses=1), match(BOB, "2", ninja_defuses=0)]
    [question] = ask(counted, stats, ALICE, BOB)
    assert (question.text, said(question)) == ("Who defused 1 time?", "Alice: 1 defuse")
    stats[0].numbers["ninja_defuses"] = 2
    [question] = ask(counted, stats, ALICE, BOB)
    assert (question.text, said(question)) == ("Who defused 2 times?", "Alice: 2 defuses")


def test_a_plural_form_that_shows_as_one_point_something_is_plural() -> None:
    per_match = template(
        rule="average", stat="numbers.aces", explanation="{value} {ace|aces} a match"
    )
    stats = [
        match(ALICE, "1", aces=1),
        match(ALICE, "2", aces=2),
        match(BOB, "3", aces=0),
    ]
    [question] = ask(per_match, stats, ALICE, BOB)
    assert said(question) == "1.50 aces a match"


def clutches(**attempts: tuple[int, int]) -> list[PlayerMatchStats]:
    """One match per player: (clutch wins, clutch attempts)."""
    ids = {"alice": ALICE, "bob": BOB, "cara": CARA}
    return [
        match(ids[name], name, clutch_wins=won, clutch_attempts=tried)
        for name, (won, tried) in attempts.items()
    ]


CLUTCH_RATE = template(
    rule="pooled_ratio",
    numerator=("numbers.clutch_wins",),
    denominator=("numbers.clutch_attempts",),
    contenders_only=True,
    min_denominator=5,
)


def test_min_denominator_lets_a_player_with_too_few_attempts_sit_out() -> None:
    # Alice's 1 of 1 would be 100%; she sits out, and Bob's 9 of 20 wins.
    stats = clutches(alice=(1, 1), bob=(9, 20), cara=(4, 10))
    [question] = ask(CLUTCH_RATE, stats, ALICE, BOB, CARA)
    assert question.correct_option_id == BOB
    assert option_ids(question) == [ALICE, BOB, CARA]


def test_min_denominator_lets_a_player_with_no_attempts_sit_out() -> None:
    stats = clutches(alice=(0, 0), bob=(9, 20), cara=(4, 10))
    [question] = ask(CLUTCH_RATE, stats, ALICE, BOB, CARA)
    assert question.correct_option_id == BOB
    # Without the minimum, a zero total is unknown and skips the question for everyone.
    assert ask(replace(CLUTCH_RATE, min_denominator=0), stats, ALICE, BOB, CARA) == []


def test_min_denominator_still_needs_two_players_and_known_data() -> None:
    assert ask(CLUTCH_RATE, clutches(alice=(1, 1), bob=(9, 20)), ALICE, BOB) == []
    missing = clutches(alice=(3, 8), bob=(9, 20))
    del missing[0].numbers["clutch_attempts"]
    assert ask(CLUTCH_RATE, missing, ALICE, BOB) == []


FF409_IDS = (
    "max-round-damage-v1",
    "recent-first-bloods-v1",
    "average-performance-score-v1",
    "opening-duel-win-rate-v1",
    "max-round-kills-v1",
    "post-plant-kills-v1",
    "ninja-defuses-v1",
    "clutch-win-rate-v1",
    "fall-deaths-v1",
    "pistol-round-kills-v1",
)


def ff409_template(template_id: str) -> QuestionTemplate:
    return pack_template(template_id)


def ff409_stats(
    template: QuestionTemplate, *, tied: bool = False, shared: bool = False
) -> list[PlayerMatchStats]:
    """Make enough fixture matches for a pack template, with Alice leading by default.

    shared: Alice and Bob play every match together; otherwise every match is solo."""
    count = template.last_n or 10
    rows: list[PlayerMatchStats] = []
    for n in range(count):
        for uid in (ALICE, BOB):
            alice = uid == ALICE
            scores = {
                "max-round-damage-v1": (160, 80),
                "recent-first-bloods-v1": (2, 1),
                "average-performance-score-v1": (265.1, 170.5),
                "opening-duel-win-rate-v1": (4, 2),
                "max-round-kills-v1": (4, 2),
                "post-plant-kills-v1": (3, 1),
                "ninja-defuses-v1": (1, 0),
                "clutch-win-rate-v1": (2, 1),
                "fall-deaths-v1": (1, 0),
                "pistol-round-kills-v1": (2, 1),
            }
            score, other_score = scores[template.id]
            if tied:
                other_score = score
            values = {
                "max_round_damage": score,
                "first_bloods": score,
                "performance_score": score,
                "first_deaths": 6 if alice else (6 if tied else 8),
                "max_round_kills": score,
                "post_plant_kills": score,
                "ninja_defuses": score,
                "clutch_wins": score,
                "clutch_attempts": 4,
                "fall_deaths": score,
                "pistol_round_kills": score,
            }
            if not alice:
                values.update(
                    {
                        "max_round_damage": other_score,
                        "first_bloods": other_score,
                        "performance_score": other_score,
                        "max_round_kills": other_score,
                        "post_plant_kills": other_score,
                        "ninja_defuses": other_score,
                        "clutch_wins": other_score,
                        "fall_deaths": other_score,
                        "pistol_round_kills": other_score,
                    }
                )
            match_id = f"{template.id}-{n}" if shared else f"{template.id}-{uid}-{n}"
            row = match(uid, match_id, seconds=n + 1, **values)
            if template.display_label:
                row.labels[template.display_label] = "Jett" if alice else "Sage"
            rows.append(row)
    return rows


@pytest.mark.parametrize("template_id", FF409_IDS)
def test_ff409_template_finds_one_winner(template_id: str) -> None:
    template = ff409_template(template_id)
    questions = ask(template, ff409_stats(template), ALICE, BOB)

    assert questions
    assert {question.correct_option_id for question in questions} == {ALICE}
    assert all(
        [option.option_id for option in question.options] == [ALICE, BOB] for question in questions
    )


@pytest.mark.parametrize("template_id", FF409_IDS)
def test_ff409_template_skips_a_tie(template_id: str) -> None:
    template = ff409_template(template_id)
    assert ask(template, ff409_stats(template, tied=True), ALICE, BOB) == []


@pytest.mark.parametrize("template_id", FF409_IDS)
def test_ff409_template_skips_missing_data(template_id: str) -> None:
    template = ff409_template(template_id)
    stats = ff409_stats(template)
    fields = (template.stat, *template.numerator, *template.denominator)
    number_field = next(field for field in fields if field.startswith("numbers."))
    key = number_field.removeprefix("numbers.")
    del stats[0].numbers[key]

    assert ask(template, stats, ALICE, BOB) == []


def test_ff409_recent_windows_use_all_scope_and_pistol_rounds_use_all_matches() -> None:
    for template_id in (
        "recent-first-bloods-v1",
        "post-plant-kills-v1",
        "ninja-defuses-v1",
        "fall-deaths-v1",
    ):
        assert ff409_template(template_id).scopes == ("all",)
    assert ff409_template("pistol-round-kills-v1").last_n is None


def test_ff409_recent_first_bloods_has_guide_wording() -> None:
    template = ff409_template("recent-first-bloods-v1")
    stats = ff409_stats(template)
    [question] = ask(template, stats, ALICE, BOB)

    assert "last 5 matches" in question.text
    assert question.correct_option_id == ALICE
    assert question.explanation.startswith("Alice opened ")


def test_ff409_fall_deaths_uses_a_blunder_closer() -> None:
    template = ff409_template("fall-deaths-v1")
    stats = ff409_stats(template)
    [question] = ask(template, stats, ALICE, BOB)

    assert "fell to their death" in question.text
    assert "unfortunate stat" in question.text
    assert question.correct_option_id == ALICE


@pytest.mark.parametrize(
    ("template_id", "question", "explanation"),
    [
        (
            "max-round-damage-v1",
            "One of you dealt 160 damage to enemies in a single round {scope}, more than anyone "
            "else here has managed. Who pulled that off?",
            "Alice, playing Jett: 160 damage in one round. Next closest: Bob, playing Sage: 80 "
            "damage in one round.",
        ),
        (
            "recent-first-bloods-v1",
            "Over their last 5 matches, this player drew first blood 10 times, the most in this "
            "group. Who is this player?",
            "Alice opened 10 rounds with the first kill in their last 5 matches. Next closest: Bob "
            "opened 5 rounds with the first kill in their last 5 matches.",
        ),
        (
            "average-performance-score-v1",
            "This player averages a Performance Score of 265.10 per match {scope}, the highest in "
            "this group. Who sets the pace?",
            "Alice, averaging 265.10 over 10 matches. Next closest: Bob, averaging 170.50 over 10 "
            "matches.",
        ),
        (
            "opening-duel-win-rate-v1",
            "This player wins 40% of their opening duels {scope}, the best record of anyone here "
            "with 10 or more. Who takes the first fight?",
            "Alice: 40 first bloods out of 100 opening duels (40%). Next closest: Bob, 20 first "
            "bloods out of 100 opening duels (20%).",
        ),
        (
            "max-round-kills-v1",
            "One of you got 4 kills in a single round {scope}, and nobody else here has "
            "matched it. Who is this player?",
            "Alice, playing Jett: 4 kills in one round. Next closest: Bob, playing Sage: 2 kills "
            "in one round.",
        ),
        (
            "post-plant-kills-v1",
            "Once the spike is down, this player gets to work: 15 post-plant kills in their last "
            "5 matches, the most in this group. Who might this player be?",
            "Alice got 15 kills after the plant in their last 5 matches. Next closest: Bob got 5 "
            "kills after the plant in their last 5 matches.",
        ),
        (
            "ninja-defuses-v1",
            "Someone here defused the spike 10 times with enemies still alive in the last 30 "
            "days. Which of you has that unique distinction?",
            "Alice: 10 ninja defuses in the last 30 days. Next closest: Bob, 0 ninja defuses in "
            "the last 30 days.",
        ),
        (
            "clutch-win-rate-v1",
            "Left as the last one standing, this player wins 50% of their clutches {scope}. "
            "Nobody here with 5 or more clutch attempts does better. Who stays cold under "
            "pressure?",
            "Alice: 20 clutch wins out of 40 clutch attempts (50%). Next closest: Bob, 10 clutch "
            "wins out of 40 clutch attempts (25%).",
        ),
        (
            "fall-deaths-v1",
            "In the last 30 days, this player fell to their death 10 times, more than anyone "
            "else here. Who does this unfortunate stat belong to?",
            "Alice recorded 10 fall deaths in the last 30 days. Next closest: Bob recorded 0 fall "
            "deaths in the last 30 days.",
        ),
        (
            "pistol-round-kills-v1",
            "This player averages 2 pistol-round kills per match {scope}, the most in this group. "
            "Who starts each half with a bang?",
            "Alice: 20 pistol-round kills in 10 matches, 2 per match. Next closest: Bob, 10 "
            "pistol-round kills in 10 matches, 1 per match.",
        ),
    ],
)
def test_ff409_question_and_answer_text(template_id: str, question: str, explanation: str) -> None:
    pack = ff409_template(template_id)
    for scope in pack.scopes:
        stats = ff409_stats(pack, shared=scope == "shared")
        questions = ask(replace(pack, scopes=(scope,)), stats, ALICE, BOB)
        assert [(q.text, q.explanation) for q in questions] == [
            (question.format(scope=_SCOPE_TEXT[scope]), explanation)
        ], scope


def test_ff409_counts_say_one_time_not_one_times() -> None:
    ninja = ff409_template("ninja-defuses-v1")
    stats = [match(ALICE, "a1", ninja_defuses=1), match(BOB, "b1", ninja_defuses=0)]
    [question] = ask(ninja, stats, ALICE, BOB)
    assert question.text.startswith("Someone here defused the spike 1 time with")
    assert said(question) == "Alice: 1 ninja defuse in the last 30 days."


def test_ff409_clutch_rate_needs_five_attempts() -> None:
    # 1 of 1 is 100%, but Alice sits out with too few attempts and Bob's 9 of 20 wins.
    stats = clutches(alice=(1, 1), bob=(9, 20), cara=(2, 10))
    rate = replace(ff409_template("clutch-win-rate-v1"), scopes=("all",))
    [question] = ask(rate, stats, ALICE, BOB, CARA)
    assert question.correct_option_id == BOB
    assert said(question) == "Bob: 9 clutch wins out of 20 clutch attempts (45%)."


def named(row: PlayerMatchStats, **names: str) -> PlayerMatchStats:
    """The row with "name:<key>" labels, as Match Data writes for weapon and agent keys."""
    for key, name in names.items():
        row.labels[f"name:{key}"] = name
    return row


def test_keyed_asks_one_question_per_named_key_and_a_missing_key_is_zero() -> None:
    guns = template(
        rule="keyed",
        stat="numbers.kills_with_weapon",
        question="{value} kills with the {parameter}?",
    )
    stats = [
        named(match(ALICE, "a1", **{"kills_with_weapon:vandal-id": 7}), **{"vandal-id": "Vandal"}),
        named(
            match(BOB, "b1", **{"kills_with_weapon:vandal-id": 3, "kills_with_weapon:op-id": 2}),
            **{"vandal-id": "Vandal", "op-id": "Operator"},
        ),
    ]
    found = {q.text: q.correct_option_id for q in ask(guns, stats, ALICE, BOB)}
    # Alice has no Operator key: 0, so Bob's 2 wins it.
    assert found == {"7 kills with the Vandal?": ALICE, "2 kills with the Operator?": BOB}


def test_keyed_never_asks_a_key_without_exactly_one_name() -> None:
    guns = template(rule="keyed", stat="numbers.kills_with_weapon", question="{value} {parameter}?")
    unnamed = [
        match(ALICE, "a1", **{"kills_with_weapon:x": 7}),
        match(BOB, "b1", **{"kills_with_weapon:x": 3}),
    ]
    assert ask(guns, unnamed, ALICE, BOB) == []
    two_names = [named(unnamed[0], x="Vandal"), named(unnamed[1], x="Phantom")]
    assert ask(guns, two_names, ALICE, BOB) == []


def test_range_is_the_gap_between_the_best_and_worst_match() -> None:
    swing = template(
        rule="range",
        stat="kills",
        question="Swung by {value}?",
        explanation="{name}: {numerator} down to {denominator}",
    )
    stats = [
        match(ALICE, "a1", kills=30),
        match(ALICE, "a2", kills=10),
        match(BOB, "b1", kills=20),
        match(BOB, "b2", kills=15),
    ]
    [question] = ask(swing, stats, ALICE, BOB)
    assert (question.text, said(question)) == ("Swung by 20?", "Alice: 30 down to 10")
    # One match has no range.
    assert ask(swing, [*stats[:2], stats[2]], ALICE, BOB) == []


def test_absent_is_zero_counts_a_missing_key_as_zero() -> None:
    triples = template(rule="count", stat="numbers.rounds_with_kills:3", question="{value}?")
    stats = [
        match(ALICE, "a1", **{"rounds_with_kills:3": 2}),
        match(ALICE, "a2"),
        match(BOB, "b1"),
        match(BOB, "b2"),
    ]
    assert ask(triples, stats, ALICE, BOB) == []  # missing is unknown
    [question] = ask(replace(triples, absent_is_zero=True), stats, ALICE, BOB)
    assert (question.text, question.correct_option_id) == ("2?", ALICE)


def test_absent_is_zero_works_in_a_ratio() -> None:
    quiet = template(
        rule="pooled_ratio",
        numerator=("numbers.rounds_with_kills:0",),
        denominator=("numbers.rounds_played",),
        question="{percent}%?",
        absent_is_zero=True,
    )
    stats = [
        match(ALICE, "a1", rounds_played=20, **{"rounds_with_kills:0": 5}),
        match(BOB, "b1", rounds_played=20),
    ]
    [question] = ask(quiet, stats, ALICE, BOB)
    assert (question.text, question.correct_option_id) == ("25%?", ALICE)


def test_value_scale_changes_the_shown_value_and_the_rounding_check() -> None:
    plant = template(
        rule="max_in_match",
        stat="numbers.fastest_plant_ms",
        direction="lowest",
        question="{value} seconds?",
        value_scale=0.001,
    )
    stats = [match(ALICE, "a1", fastest_plant_ms=15761), match(BOB, "b1", fastest_plant_ms=30000)]
    [question] = ask(plant, stats, ALICE, BOB)
    assert question.text == "15.76 seconds?"
    # 15761 ms and 15764 ms both show as 15.76 seconds: that doesn't single out a winner.
    stats[1].numbers["fastest_plant_ms"] = 15764
    assert ask(plant, stats, ALICE, BOB) == []


def test_windows_make_one_version_per_window_and_scope() -> None:
    t = template(rule="count", stat="kills", question="{value} kills {scope}?", windows=(2, None))
    stats = [
        match(uid, f"{uid}{n}", seconds=n, kills=k)
        for uid, k in ((ALICE, 9), (BOB, 5))
        for n in range(3)
    ]
    texts = [q.text for q in ask(replace(t, scopes=("all", "solo")), stats, ALICE, BOB)]
    assert sorted(texts) == sorted(
        [
            "18 kills in their last 2 matches?",
            "18 kills in their last 2 matches without other room players?",
            f"27 kills {_SCOPE_TEXT['all']}?",
            f"27 kills {_SCOPE_TEXT['solo']}?",
        ]
    )


def test_a_window_longer_than_someones_history_skips_only_that_version() -> None:
    t = template(rule="count", stat="kills", question="{value} kills {scope}?", windows=(2, 5))
    stats = [
        match(uid, f"{uid}{n}", seconds=n, kills=k)
        for uid, k in ((ALICE, 9), (BOB, 5))
        for n in range(3)
    ]
    assert [q.text for q in ask(t, stats, ALICE, BOB)] == ["18 kills in their last 2 matches?"]


def test_a_quiz_asks_one_version_of_a_template_with_windows() -> None:
    t = template(
        rule="count", stat="kills", question="{value} kills {scope}?", windows=(1, 2, None)
    )
    stats = [
        match(uid, f"{uid}{n}", seconds=n, kills=k)
        for uid, k in ((ALICE, 9), (BOB, 5))
        for n in range(3)
    ]
    assert len(ask(t, stats, ALICE, BOB)) == 3
    for seed in range(10):
        assert len(QuestionGenerator([t]).generate(players(ALICE, BOB), stats, 5, seed)) == 1


@pytest.mark.parametrize(
    ("fields", "problem"),
    [
        ({"rule": "pooled_ratio", "numerator": ("kills",)}, "numerator and denominator"),
        ({"rule": "max_in_match"}, "needs a stat"),
        ({"rule": "count", "stat": "kd"}, "stats must be"),
        ({"rule": "count", "stat": "kills", "display_label": "rank"}, "display_label"),
        ({"rule": "pair", "stat": "kills"}, "pair needs"),
        ({"rule": "count", "stat": "kills", "last_n": 0}, "last_n"),
        ({"rule": "count", "stat": "kills", "min_denominator": 5}, "min_denominator"),
        (
            {
                "rule": "pooled_ratio",
                "numerator": ("kills",),
                "denominator": ("deaths",),
                "min_denominator": 5,
            },
            "min_denominator",
        ),
        ({"rule": "count", "stat": "kills", "skip_missing": True}, "skip_missing"),
        ({"rule": "count", "stat": "kills", "windows": (5,), "last_n": 10}, "windows"),
        ({"rule": "count", "stat": "kills", "windows": (5, 5)}, "windows"),
        ({"rule": "count", "stat": "kills", "windows": (0, None)}, "windows"),
        (
            {"rule": "count", "stat": "kills", "windows": (5,), "question": "{last_n}?"},
            "{last_n} can't be used",
        ),
        ({"rule": "keyed", "stat": "kills", "question": "{parameter}?"}, "keyed needs"),
        ({"rule": "keyed", "stat": "numbers.kills_with_weapon:x"}, "keyed needs"),
        ({"rule": "keyed", "stat": "numbers.kills_with_weapon", "question": "?"}, "keyed needs"),
        ({"rule": "count", "stat": "kills", "absent_is_zero": True}, "absent_is_zero"),
        ({"rule": "count", "stat": "kills", "value_scale": 0}, "value_scale"),
        ({"rule": "count", "stat": "kills", "question": "Who has {rank}?"}, "{rank}"),
    ],
)
def test_templates_the_generator_cant_evaluate_fail_at_load(
    fields: dict[str, object], problem: str
) -> None:
    with pytest.raises(ValueError, match="Template t:") as error:
        QuestionGenerator([template(**fields)])
    assert problem in str(error.value)


def test_totals_over_every_match_compare_each_players_newest_matches_equally() -> None:
    # Alice has played twice as much: over all 6 matches she'd win 12 to 9, but like for
    # like (each player's newest 3) Bob's 9 beats her 6.
    kills = template(rule="count", stat="kills", question="{value}?")
    stats = [
        *(match(ALICE, f"a{n}", seconds=n, kills=2) for n in range(1, 7)),
        *(match(BOB, f"b{n}", seconds=n, kills=3) for n in range(1, 4)),
    ]
    [question] = ask(kills, stats, ALICE, BOB)
    assert (question.text, question.correct_option_id) == ("9?", BOB)
    assert said(question) == "Bob: 9 (9/3)"


def test_keyed_totals_compare_equal_windows_too() -> None:
    guns = template(rule="keyed", stat="numbers.kills_with_weapon")
    named = {"kills_with_weapon:vandal": 1}
    stats = [
        *(match(ALICE, f"a{n}", seconds=n, **named) for n in range(1, 5)),
        *(match(BOB, f"b{n}", seconds=n, **{"kills_with_weapon:vandal": 3}) for n in (1, 2)),
    ]
    for row in stats:
        row.labels["name:vandal"] = "Vandal"
    [question] = ask(guns, stats, ALICE, BOB)
    assert (question.text, question.correct_option_id) == ("Who? Vandal", BOB)


def test_equal_windows_skip_a_tie_at_the_cut() -> None:
    kills = template(rule="count", stat="kills", question="{value}?")
    # Bob's newest 2 is unclear: his 2nd and 3rd newest matches have the same time.
    stats = [
        *(match(ALICE, f"a{n}", seconds=n, kills=2) for n in (1, 2)),
        match(BOB, "b1", seconds=1, kills=5),
        match(BOB, "b2", seconds=1, kills=5),
        match(BOB, "b3", seconds=2, kills=5),
    ]
    assert ask(kills, stats, ALICE, BOB) == []


def test_rates_records_and_last_n_windows_keep_every_match_they_had() -> None:
    stats = [
        match(ALICE, "a1", seconds=1, kills=40, deaths=10),
        *(match(ALICE, f"a{n}", seconds=n, kills=10, deaths=10) for n in range(2, 5)),
        match(BOB, "b1", seconds=1, kills=20, deaths=10),
    ]
    # Alice's oldest match is still her best one, and still in her ratio.
    best = template(rule="max_in_match", stat="kills", question="{value}?")
    [question] = ask(best, stats, ALICE, BOB)
    assert (question.text, question.correct_option_id) == ("40?", ALICE)
    kd = template(
        rule="pooled_ratio", numerator=("kills",), denominator=("deaths",), question="{value}?"
    )
    [question] = ask(kd, stats, ALICE, BOB)
    assert (question.text, question.correct_option_id) == ("2?", BOB)
    assert said(question) == "Bob: 2 (20/10)"
    alice_kd = [q for q in ask(replace(kd, direction="lowest"), stats, ALICE, BOB)]
    assert [said(q) for q in alice_kd] == ["Alice: 1.75 (70/40)"]


def test_blunders_are_the_questions_about_a_bad_stat() -> None:
    blunders = {t.id for t in TEMPLATES if t.blunder}
    # Lowest isn't the same as bad: the fewest damage taken is a feat.
    assert "lowest-damage-received-v1" not in blunders
    assert "fewest-recent-deaths-v1" not in blunders
    assert {t.id for t in TEMPLATES if t.id.startswith("lowest-")} - blunders == {
        "lowest-damage-received-v1"
    }
    assert {
        "most-recent-deaths-v1",
        "fall-deaths-v1",
        "spike-deaths-v1",
        "topped-match-deaths-v1",
        "topped-team-first-deaths-v1",
        "attack-first-deaths-v1",
    } <= blunders
    assert len(blunders) == 33


def test_the_explanation_adds_the_runner_up_in_the_templates_own_words() -> None:
    kills = template(
        rule="count", stat="kills", question="{value}?", explanation="{name}: {value} kills."
    )
    stats = [
        match(ALICE, "a1", kills=30),
        match(BOB, "b1", kills=20),
        match(CARA, "c1", kills=10),
    ]
    [question] = ask(kills, stats, ALICE, BOB, CARA)
    # "{name}: ..." reads "Bob, ..." after "Next closest:".
    assert question.explanation == "Alice: 30 kills. Next closest: Bob, 20 kills."
    worded = replace(kills, explanation="{name} got {value} kills.")
    [question] = ask(worded, stats, ALICE, BOB, CARA)
    assert question.explanation == "Alice got 30 kills. Next closest: Bob got 20 kills."


def test_for_a_lowest_stat_the_runner_up_is_the_second_lowest() -> None:
    deaths = template(
        rule="count",
        stat="deaths",
        direction="lowest",
        question="{value}?",
        explanation="{name}: {value} deaths.",
    )
    stats = [
        match(ALICE, "a1", deaths=4),
        match(BOB, "b1", deaths=12),
        match(CARA, "c1", deaths=7),
    ]
    [question] = ask(deaths, stats, ALICE, BOB, CARA)
    assert question.explanation == "Alice: 4 deaths. Next closest: Cara, 7 deaths."


def test_the_runner_up_gets_their_own_plural_and_ties_for_second_are_named() -> None:
    defuses = template(
        rule="count",
        stat="numbers.defuses",
        question="{value}?",
        explanation="{name}: {value} {defuse|defuses}.",
    )
    stats = [
        match(ALICE, "a1", defuses=3),
        match(BOB, "b1", defuses=1),
        match(CARA, "c1", defuses=1),
    ]
    [question] = ask(defuses, stats, ALICE, BOB, CARA)
    assert question.explanation == "Alice: 3 defuses. Next closest: Bob, 1 defuse. Cara tied."


def test_a_player_without_enough_matches_is_never_the_runner_up() -> None:
    kills = template(
        rule="count", stat="kills", question="{value}?", explanation="{name}: {value} kills."
    )
    stats = [
        match(ALICE, "a1", kills=30),
        match(BOB, "b1", kills=10),
        match(CARA, "c1", kills=25),
    ]
    [question] = ask(kills, stats, ALICE, BOB, CARA, short=[CARA])
    assert question.explanation == "Alice: 30 kills. Next closest: Bob, 10 kills."
