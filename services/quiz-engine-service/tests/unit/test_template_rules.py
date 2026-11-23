"""The template interface for question packs: rules, windows, parameters and checks."""

from collections.abc import Collection
from dataclasses import replace
from fractions import Fraction

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question, QuizPlayer

from quiz_engine.question_generator import _SCOPE_TEXT, QuestionGenerator, difficulty
from quiz_engine.templates import QuestionTemplate
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
    assert question.explanation == "Alice: 40 (40/1)"
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
    assert (question.text, question.explanation) == ("Who defused 1 time?", "Alice: 1 defuse")
    stats[0].numbers["ninja_defuses"] = 2
    [question] = ask(counted, stats, ALICE, BOB)
    assert (question.text, question.explanation) == ("Who defused 2 times?", "Alice: 2 defuses")


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
    assert question.explanation == "1.50 aces a match"


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
            "Alice, playing Jett: 160 damage in one round.",
        ),
        (
            "recent-first-bloods-v1",
            "Over their last 5 matches, this player drew first blood 10 times, the most in this "
            "group. Who is this player?",
            "Alice opened 10 rounds with the first kill in their last 5 matches.",
        ),
        (
            "average-performance-score-v1",
            "This player averages a Performance Score of 265.10 per match {scope}, the highest in "
            "this group. Who set the pace?",
            "Alice, averaging 265.10 over 10 matches.",
        ),
        (
            "opening-duel-win-rate-v1",
            "This player wins 40% of their opening duels {scope}, the best record of anyone here "
            "with 10 or more. Who takes the first fight?",
            "Alice: 40 first bloods out of 100 opening duels.",
        ),
        (
            "max-round-kills-v1",
            "One of you got 4 kills in a single round {scope}, and nobody else here has "
            "matched it. Who is this player?",
            "Alice, playing Jett: 4 kills in one round.",
        ),
        (
            "post-plant-kills-v1",
            "Once the spike is down, this player gets to work: 15 post-plant kills in their last "
            "5 matches, the most in this group. Who might this player be?",
            "Alice got 15 kills after the plant in their last 5 matches.",
        ),
        (
            "ninja-defuses-v1",
            "Someone here defused the spike 10 times with enemies still alive in the last 30 "
            "days. Which of you has that unique distinction?",
            "Alice: 10 ninja defuses in the last 30 days.",
        ),
        (
            "clutch-win-rate-v1",
            "Left as the last one standing, this player wins 50% of their clutches {scope}. "
            "Nobody here with 5 or more clutch attempts does better. Who stays cold under "
            "pressure?",
            "Alice: 20 clutch wins out of 40 clutch attempts.",
        ),
        (
            "fall-deaths-v1",
            "In the last 30 days, this player fell to their death 10 times, more than anyone "
            "else here. Who does this unfortunate stat belong to?",
            "Alice recorded 10 fall deaths in the last 30 days.",
        ),
        (
            "pistol-round-kills-v1",
            "This player averages 2 pistol-round kills per match {scope}, the most in this group. "
            "Who starts rounds with a bang?",
            "Alice: 20 pistol-round kills in 10 matches.",
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
    assert question.explanation == "Alice: 1 ninja defuse in the last 30 days."


def test_ff409_clutch_rate_needs_five_attempts() -> None:
    # 1 of 1 is 100%, but Alice sits out with too few attempts and Bob's 9 of 20 wins.
    stats = clutches(alice=(1, 1), bob=(9, 20), cara=(2, 10))
    rate = replace(ff409_template("clutch-win-rate-v1"), scopes=("all",))
    [question] = ask(rate, stats, ALICE, BOB, CARA)
    assert question.correct_option_id == BOB
    assert question.explanation == "Bob: 9 clutch wins out of 20 clutch attempts."


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
        ({"rule": "count", "stat": "kills", "question": "Who has {rank}?"}, "{rank}"),
    ],
)
def test_templates_the_generator_cant_evaluate_fail_at_load(
    fields: dict[str, object], problem: str
) -> None:
    with pytest.raises(ValueError, match="Template t:") as error:
        QuestionGenerator([template(**fields)])
    assert problem in str(error.value)
