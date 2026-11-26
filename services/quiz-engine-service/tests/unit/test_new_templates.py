"""Tests for the ten templates added in batch 2 of templates.py.

Two layers:

1. Structure: every new template follows the Quiz Template Creation Guide (fields per rule,
   declared stats, placeholders, wording rules), and no two templates in TEMPLATES ask
   the same question (same rule, stats and direction). These only read the templates
   and the generator's own constants.
2. Behavior: each template runs through the real QuestionGenerator with a small room
   (Alice, Bob and Cara, from test_template_rules), checking the exact winner, question
   and explanation text, plus the skip or sit-out case that matters for it.
"""

import re
import string
from dataclasses import replace
from pathlib import Path

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question

from quiz_engine.question_generator import _PLACEHOLDERS, _PLURAL
from quiz_engine.templates import TEMPLATES, QuestionTemplate
from tests.unit.test_template_rules import ALICE, BOB, CARA, ask

NEW_IDS = (
    "longest-kill-v1",
    "utility-assists-v1",
    "underdog-kill-share-v1",
    "role-utility-per-round-v1",
    "trades-for-friend-v1",
    "kills-on-enemy-mvp-v1",
    "team-mvps-v1",
    "solo-thrifty-rounds-v1",
    "unarmored-rounds-v1",
    "most-deaths-to-one-opponent-v1",
)

BY_ID = {t.id: t for t in TEMPLATES}
NEW = [BY_ID[i] for i in NEW_IDS if i in BY_ID]


def primary_stats(t: QuestionTemplate) -> set[str]:
    """What the question is about: its stat, or its numerators for a ratio."""
    return ({t.stat} if t.stat else set()) | set(t.numerator)


def placeholders(text: str) -> set[str]:
    text = _PLURAL.sub(r"\2", text)  # {singular|plural} isn't a placeholder
    return {name for _, name, _, _ in string.Formatter().parse(text) if name}


# --------------------------------------------------------------------------------------
# 1. Structure
# --------------------------------------------------------------------------------------


def test_all_ten_new_templates_exist() -> None:
    assert [t.id for t in NEW] == list(NEW_IDS)


def test_ids_are_unique_and_versioned() -> None:
    ids = [t.id for t in TEMPLATES]
    assert len(ids) == len(set(ids))
    for t in NEW:
        assert re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*-v1", t.id), t.id


def test_no_two_templates_ask_the_same_question() -> None:
    # Same rule, stats and direction. A count and a rate of the same stat (first bloods,
    # and first bloods per opening duel) are different questions.
    seen: dict[tuple[object, ...], str] = {}
    for t in TEMPLATES:
        key = (t.rule, t.stat, t.numerator, t.denominator, t.direction)
        assert key not in seen, f"{t.id} and {seen[key]} ask the same question"
        seen[key] = t.id


def test_new_templates_use_a_mix_of_rules() -> None:
    assert len({(t.rule, t.parameter_label, t.scopes) for t in NEW}) >= 6


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_declared_stats_cover_everything_the_template_reads(t: QuestionTemplate) -> None:
    assert "labels.queue_id" in t.stats
    declared = {s.split(":")[0] for s in t.stats}
    read = primary_stats(t) | set(t.denominator)
    assert read <= declared
    if t.last_n is not None or any(n is not None for n in t.windows):
        assert "played_at" in t.stats
    if t.parameter_label:
        assert f"labels.{t.parameter_label}" in t.stats
    if t.display_label:
        assert f"labels.{t.display_label}" in t.stats
    if t.rule == "pair":
        assert f"{t.stat}:<user_id>" in t.stats and "labels.team_id" in t.stats


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_fields_match_the_rule(t: QuestionTemplate) -> None:
    if t.rule == "pooled_ratio":
        assert t.numerator and t.denominator and not t.stat
    else:
        assert t.stat and not t.numerator and not t.denominator
    if t.rule == "pair":
        assert ":" not in t.stat and not t.parameter_label
    if t.display_label:
        assert t.rule == "max_in_match"
    if t.parameter_label:
        assert t.contenders_only and "{parameter}" in t.question
    if t.scopes == ("solo",):
        assert t.contenders_only


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_placeholders_fit_the_rule(t: QuestionTemplate) -> None:
    allowed = {"name", "value", "scope"}
    if t.last_n is not None:
        allowed.add("last_n")
    if t.rule != "max_in_match":
        allowed |= {"numerator", "denominator"}
    if t.rule == "pooled_ratio":
        allowed.add("percent")
    if t.parameter_label:
        allowed.add("parameter")
    if t.rule == "pair":
        allowed.add("friend")
    if t.display_label and t.rule == "max_in_match":
        allowed.add(f"{t.display_label}_name")
    used = placeholders(t.question) | placeholders(t.explanation)
    assert used <= allowed, used - allowed
    assert used <= {*_PLACEHOLDERS, f"{t.display_label}_name"}
    assert "name" in placeholders(t.explanation)


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_question_follows_the_wording_rules(t: QuestionTemplate) -> None:
    q = t.question
    assert q.endswith("?")
    assert not q.startswith("Among these players")
    assert "{value}" in q or "{percent}" in q  # the number singles out the winner
    assert "nobody comes close" not in q.lower()
    if len(t.scopes) > 1:
        assert "{scope}" in q  # several scopes: the question must say which matches count
    if t.scopes == ("solo",):
        assert "{scope}" not in q  # a single solo scope says so in its own words


def test_new_questions_do_not_open_and_close_the_same_way() -> None:
    openings = [" ".join(t.question.split()[:3]) for t in NEW]
    closers = [t.question.rsplit(". ", 1)[-1] for t in NEW]
    assert len(set(closers)) == len(closers)
    assert len(set(openings)) >= 9


def _catalogue_text() -> str:
    # The repo root in CI; /workspace in the dev container, which mounts docs/ read-only.
    for parent in Path(__file__).resolve().parents:
        candidate = parent / "docs" / "stats-catalogue.md"
        if candidate.exists():
            return candidate.read_text()
    pytest.fail("docs/stats-catalogue.md not found above this test file (is docs/ mounted?)")


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_every_key_exists_in_the_stats_catalogue(t: QuestionTemplate) -> None:
    text = _catalogue_text()
    for stat in t.stats:
        key = stat.split(".", 1)[1] if "." in stat else stat
        key = key.split(":")[0]
        assert re.search(rf"`{re.escape(key)}[`:]", text), f"{stat} is not in the catalogue"


# --------------------------------------------------------------------------------------
# 2. Behavior, through the real QuestionGenerator
# --------------------------------------------------------------------------------------

ALL = "in the available Competitive and Unrated match history"


def game(
    uid: str,
    match_id: str,
    seconds: int,
    numbers: dict[str, float],
    *,
    kills: int = 10,
    team: str = "Red",
    **labels: str,
) -> PlayerMatchStats:
    """One player's row in one match; numbers use the bare catalogue key.

    A match is shared when another room player has a row with the same match_id."""
    row = PlayerMatchStats(
        user_id=uid,
        match_id=match_id,
        game="valorant",
        kills=kills,
        deaths=10,
        labels={"queue_id": "competitive", "team_id": team, **labels},
        numbers=numbers,
    )
    row.played_at.seconds = seconds
    return row


def series(
    uid: str,
    numbers: dict[str, list[float]],
    *,
    kills: list[int] | None = None,
    **labels: str | list[str],
) -> list[PlayerMatchStats]:
    """Solo matches "<uid>-1", "<uid>-2", ..., oldest first, one per list entry.

    A label given as a list varies by match."""
    count = len(next(iter(numbers.values())))
    return [
        game(
            uid,
            f"{uid}-{i + 1}",
            i + 1,
            {key: values[i] for key, values in numbers.items()},
            kills=kills[i] if kills else 10,
            **{k: v[i] if isinstance(v, list) else v for k, v in labels.items()},
        )
        for i in range(count)
    ]


def questions(t: QuestionTemplate, stats: list[PlayerMatchStats], scope: str = "all"):
    """Every question the template makes for Alice, Bob and Cara, in one scope."""
    return ask(replace(t, scopes=(scope,)), stats, ALICE, BOB, CARA)


def only(t: QuestionTemplate, stats: list[PlayerMatchStats], scope: str = "all") -> Question:
    [question] = questions(t, stats, scope)
    return question


def last(n: int, template_id: str) -> QuestionTemplate:
    """Only the last-n-matches version of a template with windows."""
    return replace(BY_ID[template_id], windows=(n,))


def check(question: Question, winner: str, text: str, explanation: str) -> None:
    assert (question.correct_option_id, question.text, question.explanation) == (
        winner,
        text,
        explanation,
    )


# ---- 1. longest-kill-v1 ----------------------------------------------------------------

LONGEST = "longest_kill_m"


def _longest_kill_stats() -> list[PlayerMatchStats]:
    return [
        *series(ALICE, {LONGEST: [30, 41.5, 12, 20, 25]}, map="Ascent"),
        *series(
            BOB, {LONGEST: [20, 52.25, 33, 18, 40]}, map=["Bind", "Haven", "Bind"] + ["Bind"] * 2
        ),
        *series(CARA, {LONGEST: [38, 22, 19, 15, 10]}, map="Split"),
    ]


def test_longest_kill_names_the_longest_shot_and_its_map() -> None:
    check(
        only(BY_ID["longest-kill-v1"], _longest_kill_stats()),
        BOB,
        f"One of you once landed a kill from 52.25 meters away {ALL}, the longest shot in "
        "this group. Who can hit a target from across the map?",
        "Bob, on Haven: a kill from 52.25 meters out. Next closest: Alice, on Ascent: a kill from "
        "41.50 meters out.",
    )


def test_longest_kill_ignores_a_match_with_no_kills() -> None:
    # A match with no kills has no longest kill; it mustn't stop the question.
    stats = [*_longest_kill_stats(), game(CARA, "c-0", 0, {}, kills=0, map="Split")]
    assert only(BY_ID["longest-kill-v1"], stats).correct_option_id == BOB


def test_longest_kill_is_skipped_on_a_tie_or_a_missing_map() -> None:
    t = BY_ID["longest-kill-v1"]
    tied = _longest_kill_stats()
    tied[10].numbers[LONGEST] = 52.25  # Cara's first match
    assert questions(t, tied) == []
    unlabeled = _longest_kill_stats()
    del unlabeled[6].labels["map"]  # Bob's 52.25 m match
    assert questions(t, unlabeled) == []


# ---- 2. utility-assists-v1 -------------------------------------------------------------

UTILITY = "utility_assists"


def _utility_stats() -> list[PlayerMatchStats]:
    return [
        *series(ALICE, {UTILITY: [4, 3, 5, 2, 4, 3, 3, 4, 2, 5]}),
        *series(BOB, {UTILITY: [3, 3, 2, 4, 3, 3, 2, 3, 3, 4]}),
        *series(CARA, {UTILITY: [2, 3, 4, 3, 2, 3, 3, 2, 3, 3]}),
    ]


def test_utility_assists_totals_the_last_ten_matches() -> None:
    check(
        only(last(10, "utility-assists-v1"), _utility_stats()),
        ALICE,
        "Flashes and utility did the work: this player earned 35 assists without damaging "
        "the victim in their last 10 matches, the most in this group. "
        "Who sets everyone else up?",
        "Alice: 35 assists with no damage dealt. Next closest: Bob, 30 assists with no damage "
        "dealt.",
    )


def test_utility_assists_ignores_matches_older_than_the_window() -> None:
    stats = [*_utility_stats(), game(CARA, "c-0", 0, {UTILITY: 50})]
    assert only(last(10, "utility-assists-v1"), stats).correct_option_id == ALICE


def test_utility_assists_is_skipped_when_someone_has_fewer_than_ten_matches() -> None:
    stats = [r for r in _utility_stats() if r.match_id not in {"c-9", "c-10"}]
    assert questions(last(10, "utility-assists-v1"), stats) == []


@pytest.mark.parametrize(
    ("scope", "texts"),
    [
        (
            "all",
            [
                "17 assists without damaging the victim in their last 5 matches",
                "35 assists without damaging the victim in their last 10 matches",
                f"35 assists without damaging the victim {ALL}",
            ],
        ),
        (
            "solo",
            [
                "17 assists without damaging the victim in their last 5 matches without other "
                "room players",
                "35 assists without damaging the victim in their last 10 matches without "
                "other room players",
                "35 assists without damaging the victim in available matches played without "
                "other room players",
            ],
        ),
        ("shared", []),
    ],
)
def test_utility_assists_asks_the_last_5_the_last_10_and_all_matches_in_each_scope(
    scope: str, texts: list[str]
) -> None:
    # The players only have solo matches, so there's no "with other room players" version.
    found = questions(BY_ID["utility-assists-v1"], _utility_stats(), scope)
    shown = sorted(q.text.split("earned ", 1)[1].split(", the most")[0] for q in found)
    assert shown == sorted(texts)
    assert all(q.correct_option_id == ALICE for q in found)


# ---- 3. underdog-kill-share-v1 ---------------------------------------------------------

OUTGUNNED = "outgunned_kills"


def test_underdog_share_is_pooled_and_shown_as_a_percentage() -> None:
    stats = [
        *series(ALICE, {OUTGUNNED: [6, 4, 5, 7, 3]}, kills=[20, 15, 18, 22, 17]),
        *series(BOB, {OUTGUNNED: [3, 4, 2, 5, 3]}, kills=[18, 20, 16, 19, 17]),
        *series(CARA, {OUTGUNNED: [5, 6, 4, 3, 5]}, kills=[21, 14, 19, 16, 20]),
    ]
    check(
        only(BY_ID["underdog-kill-share-v1"], stats),
        ALICE,
        f"27.2% of this player's kills came against a better-equipped opponent {ALL}, the "
        "highest share in this group. Who wins the fights they shouldn't?",
        "Alice: 25 of 92 kills against a pricier loadout (27.2%). Next closest: Cara, 23 of 90 "
        "kills against a pricier loadout (25.6%).",
    )


def test_underdog_share_is_skipped_on_an_exact_tie() -> None:
    stats = [
        *series(ALICE, {OUTGUNNED: [5, 5, 5, 5, 5]}, kills=[20] * 5),
        *series(BOB, {OUTGUNNED: [5, 5, 5, 5, 4]}, kills=[20] * 5),
        *series(CARA, {OUTGUNNED: [1, 1, 1, 1, 1]}, kills=[20] * 5),
    ]
    t = BY_ID["underdog-kill-share-v1"]
    assert only(t, stats).correct_option_id == ALICE
    stats[9].numbers[OUTGUNNED] = 5  # Bob's last match: now Alice and Bob tie
    assert questions(t, stats) == []


# ---- 4. role-utility-per-round-v1 ------------------------------------------------------


def _role_stats() -> list[PlayerMatchStats]:
    def casts(
        uid: str, rounds: list[float], g: list[float], a1: list[float], a2: list[float], role
    ) -> list[PlayerMatchStats]:
        numbers = {
            "rounds_played": rounds,
            "ability_casts_grenade": g,
            "ability_casts_ability1": a1,
            "ability_casts_ability2": a2,
        }
        return series(uid, numbers, role=role)

    return [
        *casts(
            ALICE,
            [20, 22, 20, 18, 24],
            [20, 22, 5, 18, 5],
            [30, 33, 9, 27, 9],
            [11, 11, 2, 9, 2],
            ["Controller", "Controller", "Duelist", "Controller", "Sentinel"],
        ),
        *casts(
            BOB,
            [24, 20, 20, 20, 20],
            [20, 3, 3, 18, 3],
            [24, 3, 3, 20, 3],
            [8, 2, 2, 6, 2],
            ["Controller", "Duelist", "Duelist", "Controller", "Duelist"],
        ),
        *casts(CARA, [20] * 5, [10] * 5, [10] * 5, [10] * 5, "Duelist"),
    ]


def test_role_utility_compares_only_that_role_and_lets_non_players_sit_out() -> None:
    # Cara never played Controller, so she sits the Controller question out.
    [controller] = [
        q
        for q in questions(BY_ID["role-utility-per-round-v1"], _role_stats())
        if "Controller" in q.text
    ]
    check(
        controller,
        ALICE,
        f"Playing Controller {ALL}, this player casts 3.02 non-ultimate abilities per round, "
        "the most of anyone here who has played that role. Who keeps the utility flowing?",
        "Alice: 181 ability casts in 60 rounds as Controller, 3.02 per round. Next closest: Bob, "
        "96 ability casts in 44 rounds as Controller, 2.18 per round.",
    )


def test_role_utility_is_skipped_when_a_match_has_no_role_label() -> None:
    stats = _role_stats()
    del stats[7].labels["role"]  # one of Bob's matches
    assert questions(BY_ID["role-utility-per-round-v1"], stats) == []


# ---- 5. trades-for-friend-v1 -----------------------------------------------------------

TRADES_FOR_CARA = f"trades_for:{CARA}"


def _pair_stats() -> list[PlayerMatchStats]:
    """Five matches all three played; who was on Cara's team changes."""
    teams = {
        ALICE: ["Red", "Red", "Red", "Blue", "Blue"],
        BOB: ["Red", "Blue", "Red", "Blue", "Blue"],
        CARA: ["Red", "Red", "Red", "Red", "Blue"],
    }
    trades = {ALICE: [3, 2, 4, 9, 3], BOB: [2, 5, 1, 7, 2]}
    return [
        game(
            uid,
            f"shared-{i + 1}",
            i + 1,
            {TRADES_FOR_CARA: trades[uid][i]} if uid in trades else {},
            team=teams[uid][i],
        )
        for uid in (ALICE, BOB, CARA)
        for i in range(5)
    ]


def test_trades_for_friend_counts_only_matches_on_the_friends_team() -> None:
    # Alice was on Cara's team in 4 matches (3 + 2 + 4 + 3), Bob in 3 (2 + 1 + 2).
    check(
        only(BY_ID["trades-for-friend-v1"], _pair_stats()),
        ALICE,
        f"Someone here has avenged Cara 12 times {ALL} by killing their killer within 3 "
        "seconds, more than anyone else who has played on Cara's team. Who has Cara's back?",
        "Alice avenged Cara 12 times in 4 matches together. Next closest: Bob avenged Cara 5 times "
        "in 3 matches together.",
    )


def test_trades_for_friend_is_skipped_when_a_shared_match_lacks_the_pair_key() -> None:
    stats = _pair_stats()
    del stats[0].numbers[TRADES_FOR_CARA]  # Alice, on Cara's team in shared-1
    assert questions(BY_ID["trades-for-friend-v1"], stats) == []


# ---- 6. kills-on-enemy-mvp-v1 ----------------------------------------------------------

MVP_KILLS = "kills_on_enemy_mvp"


def test_kills_on_enemy_mvp_totals_the_last_ten_matches() -> None:
    stats = [
        *series(ALICE, {MVP_KILLS: [3, 2, 4, 3, 2, 3, 4, 2, 3, 3]}),
        *series(BOB, {MVP_KILLS: [2, 2, 3, 2, 1, 3, 2, 2, 2, 3]}),
        *series(CARA, {MVP_KILLS: [3, 3, 2, 2, 3, 2, 3, 2, 3, 2]}),
    ]
    check(
        only(last(10, "kills-on-enemy-mvp-v1"), stats),
        ALICE,
        "This player took down the other team's MVP 29 times in their last 10 matches, "
        "the most in this group. Who hunts the star player?",
        "Alice killed the opposing MVP 29 times. Next closest: Cara killed the opposing MVP 25 "
        "times.",
    )


def test_kills_on_enemy_mvp_says_one_time_not_one_times() -> None:
    stats = [
        *series(ALICE, {MVP_KILLS: [1, 0, 0, 0, 0, 0, 0, 0, 0, 0]}),
        *series(BOB, {MVP_KILLS: [0] * 10}),
        *series(CARA, {MVP_KILLS: [0] * 10}),
    ]
    check(
        only(last(10, "kills-on-enemy-mvp-v1"), stats),
        ALICE,
        "This player took down the other team's MVP 1 time in their last 10 matches, "
        "the most in this group. Who hunts the star player?",
        "Alice killed the opposing MVP 1 time. Next closest: Bob killed the opposing MVP 0 times. "
        "Cara tied.",
    )


# ---- 7. team-mvps-v1 -------------------------------------------------------------------

TEAM_MVP = "team_mvp"


def _team_mvp_stats(bob: list[float]) -> list[PlayerMatchStats]:
    return [
        *series(ALICE, {TEAM_MVP: [1, 0, 1, 1, 0, 1, 0, 1, 1, 0]}),
        *series(BOB, {TEAM_MVP: bob}),
        *series(CARA, {TEAM_MVP: [0, 0, 1, 0, 1, 0, 0, 1, 0, 1]}),
    ]


def test_team_mvps_counts_awards_in_the_last_ten_matches() -> None:
    check(
        only(last(10, "team-mvps-v1"), _team_mvp_stats([0, 1, 0, 1, 0, 0, 1, 0, 1, 0])),
        ALICE,
        "This player was their team's MVP 6 times in their last 10 matches, more than anyone "
        "else in this group. Who carries the squad?",
        "Alice: team MVP 6 times. Next closest: Bob, team MVP 4 times. Cara tied.",
    )


def test_team_mvps_is_skipped_on_a_tie_for_the_lead() -> None:
    # Alice and Bob tie over the last 5 and the last 10 (all of their matches).
    stats = _team_mvp_stats([1, 1, 0, 1, 0, 1, 0, 1, 0, 1])
    assert questions(BY_ID["team-mvps-v1"], stats) == []


# ---- 8. solo-thrifty-rounds-v1 ---------------------------------------------------------

THRIFTY = "thrifty_rounds"


def _thrifty_stats(*, bob_solo: bool = True) -> list[PlayerMatchStats]:
    """Alice and Bob share two matches; Cara only ever plays with Bob, so she has no solo
    games. Thrifty rounds are a team stat, so teammates in a match share the value."""
    stats = [
        game(ALICE, "a-1", 1, {THRIFTY: 3}),
        game(ALICE, "a-3", 3, {THRIFTY: 2}),
        game(ALICE, "a-5", 5, {THRIFTY: 2}),
    ]
    for n in (2, 4):
        stats += [game(uid, f"ab-{n}", n, {THRIFTY: 9}) for uid in (ALICE, BOB)]
    for n in range(1, 6):
        stats += [game(uid, f"bc-{n}", 10 + n, {THRIFTY: 4}) for uid in (BOB, CARA)]
    if bob_solo:
        stats += [
            game(BOB, "b-1", 1, {THRIFTY: 1}),
            game(BOB, "b-3", 3, {THRIFTY: 2}),
            game(BOB, "b-5", 5, {THRIFTY: 2}),
        ]
    return stats


def test_solo_thrifty_uses_solo_games_only_and_lets_a_player_without_any_sit_out() -> None:
    # Solo: Alice 3 + 2 + 2 in 3 matches, Bob 1 + 2 + 2 in 3; Cara has none and sits out.
    check(
        only(BY_ID["solo-thrifty-rounds-v1"], _thrifty_stats(), scope="solo"),
        ALICE,
        "Queueing without the rest of you, this player's team wins 2.33 thrifty rounds per "
        "match (rounds won against much bigger buys), the most of anyone here who plays "
        "solo. Who wins on a budget?",
        "Alice: 7 thrifty rounds in 3 solo matches, 2.33 per match. Next closest: Bob, 5 thrifty "
        "rounds in 3 solo matches, 1.67 per match.",
    )


def test_solo_thrifty_is_skipped_when_only_one_player_has_solo_games() -> None:
    stats = _thrifty_stats(bob_solo=False)
    assert questions(BY_ID["solo-thrifty-rounds-v1"], stats, scope="solo") == []


# ---- 9. unarmored-rounds-v1 ------------------------------------------------------------

UNARMORED = "rounds_without_armor"


def test_unarmored_rounds_totals_the_last_five_matches_only() -> None:
    stats = [
        *series(ALICE, {UNARMORED: [14, 12, 15, 13, 16]}),
        game(BOB, "b-0", 0, {UNARMORED: 40}),  # older than Bob's last 5
        *series(BOB, {UNARMORED: [10, 9, 11, 12, 10]}),
        *series(CARA, {UNARMORED: [12, 13, 11, 14, 12]}),
    ]
    check(
        only(BY_ID["unarmored-rounds-v1"], stats),
        ALICE,
        "Across their last 5 matches, this player headed into 70 rounds with no armor, more "
        "than anyone else here. Who's playing on hard mode?",
        "Alice started 70 rounds unarmored over their last 5 matches. Next closest: Cara started "
        "62 rounds unarmored over their last 5 matches.",
    )


# ---- 10. most-deaths-to-one-opponent-v1 ------------------------------------------------

FARMED = "max_deaths_to_one_opponent"


def _farmed_stats() -> list[PlayerMatchStats]:
    return [
        *series(ALICE, {FARMED: [3, 5, 2, 4, 3]}, agent="Jett"),
        *series(BOB, {FARMED: [6, 2, 3, 2, 2]}, agent=["Omen"] + ["Sova"] * 4),
        *series(CARA, {FARMED: [4, 4, 3, 5, 2]}, agent="Sage"),
    ]


def test_deaths_to_one_opponent_names_the_worst_match_and_agent() -> None:
    check(
        only(BY_ID["most-deaths-to-one-opponent-v1"], _farmed_stats()),
        BOB,
        f"One of you once died 6 times to the same opponent in a single match {ALL}, more "
        "than anyone else here. Who got farmed that day?",
        "Bob, playing Omen: 6 deaths to one opponent. Next closest: Alice, playing Jett: 5 deaths "
        "to one opponent. Cara tied.",
    )


def test_deaths_to_one_opponent_is_skipped_when_the_worst_match_has_no_agent() -> None:
    stats = _farmed_stats()
    del stats[5].labels["agent"]  # Bob's 6-death match
    assert questions(BY_ID["most-deaths-to-one-opponent-v1"], stats) == []
