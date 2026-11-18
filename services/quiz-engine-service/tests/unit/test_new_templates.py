"""Tests for the ten templates added in batch 2 of templates.py.

Two layers:

1. Structure: every new template follows the Quiz Template Creation Guide (fields per rule,
   declared stats, placeholders, wording rules), none repeats an already-made question, and
   no two new ones compare the same evidence. These only need ``quiz_engine.templates``.
2. Behavior: for each template, a small data set (Rin, Kai and Sam) gives the exact winner,
   question and explanation text, plus the skip or sit-out case that matters for it.

The behavior layer runs on ``ask()`` below, a compact stand-in for the generator that follows
the rules in the guide and in the templates.py docstring. It exists so this file needs no
other test helpers; to run the same cases through the real generator, replace ``ask()`` and
``series()``/``m()`` with the helpers in tests/unit/test_template_rules.py.
"""

from __future__ import annotations

import re
import string
from dataclasses import dataclass
from decimal import ROUND_HALF_UP, Decimal
from fractions import Fraction
from pathlib import Path

import pytest

from quiz_engine.templates import TEMPLATES, QuestionTemplate

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

# Every id already made: the three shipped templates, the two teammate files and the
# worked examples in the guide. New questions must not reuse any of them.
ALREADY_MADE_IDS = {
    "most-kills-v1", "highest-acs-v1", "recent-wins-v1",
    "max-round-damage-v1", "recent-first-bloods-v1", "average-performance-score-v1",
    "opening-duel-win-rate-v1", "max-round-kills-v1", "post-plant-kills-v1",
    "ninja-defuses-v1", "clutch-win-rate-v1", "fall-deaths-v1", "pistol-round-kills-v1",
    "highest-recent-rank-v1", "highest-recent-kd-v1", "highest-recent-headshot-percentage-v1",
    "most-recent-headshots-v1", "most-recent-agent-games-v1", "highest-recent-map-win-rate-v1",
    "highest-recent-agent-kd-v1", "lowest-recent-adr-v1", "highest-recent-adr-v1",
    "highest-recent-round-damage-v1", "most-recent-competitive-wins-v1",
    "most-recent-competitive-kills-v1", "most-recent-kills-v1", "most-recent-assists-v1",
    "most-recent-deaths-v1", "most-recent-first-bloods-v1", "most-recent-plants-v1",
    "most-recent-defuses-v1", "highest-recent-kda-v1", "most-recent-first-deaths-v1",
    "headshot-rate-v1", "map-kd-v1", "assists-for-friend-v1", "giant-killer-v1",
    "avg-scoreboard-position-v1", "solo-flawless-rounds-v1", "attack-kast-v1",
    "attack-kda-v1", "solo-attack-round-win-rate-v1", "first-half-kills-per-round-v1",
    "solo-second-half-round-win-rate-v1",
}

# The stat each already-made question is about (its stat, or its numerators for a ratio).
ALREADY_USED_PRIMARY_STATS = {
    "kills", "deaths", "assists", "won", "matches",
    "numbers.combat_score", "numbers.performance_score", "numbers.rank_tier",
    "numbers.max_round_damage", "numbers.max_round_kills", "numbers.damage_dealt",
    "numbers.headshots", "numbers.first_bloods", "numbers.first_deaths",
    "numbers.post_plant_kills", "numbers.ninja_defuses", "numbers.clutch_wins",
    "numbers.fall_deaths", "numbers.pistol_round_kills", "numbers.plants", "numbers.defuses",
    "numbers.assists_for", "numbers.kills_on_higher_ranked", "numbers.scoreboard_position",
    "numbers.flawless_rounds", "numbers.pistol_rounds_won", "numbers.attack_kast_rounds",
    "numbers.attack_kills", "numbers.attack_assists", "numbers.first_half_kills",
    "numbers.rounds_won_attack", "numbers.rounds_won_second_half",
}

KNOWN_PLACEHOLDERS = {
    "name", "value", "numerator", "denominator", "percent", "scope", "last_n",
    "parameter", "friend",
}


def primary_stats(t: QuestionTemplate) -> set[str]:
    return ({t.stat} if t.stat else set()) | set(t.numerator)


def placeholders(text: str) -> set[str]:
    return {name for _, name, _, _ in string.Formatter().parse(text) if name}


# --------------------------------------------------------------------------------------
# 1. Structure
# --------------------------------------------------------------------------------------


def test_all_ten_new_templates_exist():
    assert [t.id for t in NEW] == list(NEW_IDS)


def test_ids_are_unique_and_versioned():
    ids = [t.id for t in TEMPLATES]
    assert len(ids) == len(set(ids))
    for t in NEW:
        assert re.fullmatch(r"[a-z0-9]+(-[a-z0-9]+)*-v1", t.id), t.id


def test_no_new_id_repeats_an_already_made_question():
    assert not set(NEW_IDS) & ALREADY_MADE_IDS


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_new_template_is_about_a_stat_no_earlier_question_used(t):
    assert not primary_stats(t) & ALREADY_USED_PRIMARY_STATS


def test_new_templates_compare_different_evidence():
    seen: dict[str, str] = {}
    for t in NEW:
        for stat in primary_stats(t):
            assert stat not in seen, f"{t.id} and {seen[stat]} both compare {stat}"
            seen[stat] = t.id
    assert len({(t.rule, t.parameter_label, t.scopes) for t in NEW}) >= 6


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_declared_stats_cover_everything_the_template_reads(t):
    assert "labels.queue_id" in t.stats
    declared = {s.split(":")[0] for s in t.stats}
    read = primary_stats(t) | set(t.denominator)
    assert read <= declared
    if t.last_n is not None:
        assert "played_at" in t.stats
    if t.parameter_label:
        assert f"labels.{t.parameter_label}" in t.stats
    if t.display_label:
        assert f"labels.{t.display_label}" in t.stats
    if t.rule == "pair":
        assert f"{t.stat}:<user_id>" in t.stats and "labels.team_id" in t.stats


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_fields_match_the_rule(t):
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
def test_placeholders_fit_the_rule(t):
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
    assert used <= KNOWN_PLACEHOLDERS | {f"{t.display_label}_name"}
    assert "name" in placeholders(t.explanation)


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_question_follows_the_wording_rules(t):
    q = t.question
    assert q.endswith("?")
    assert not q.startswith("Among these players")
    assert "{value}" in q or "{percent}" in q  # the number singles out the winner
    assert "nobody comes close" not in q.lower()
    if len(t.scopes) > 1:
        assert "{scope}" in q  # several scopes: the question must say which matches count
    if t.scopes == ("solo",):
        assert "{scope}" not in q  # a single solo scope says so in its own words


def test_new_questions_do_not_open_and_close_the_same_way():
    openings = [" ".join(t.question.split()[:3]) for t in NEW]
    closers = [t.question.rsplit(". ", 1)[-1] for t in NEW]
    assert len(set(closers)) == len(closers)
    assert len(set(openings)) >= 9


def _catalogue_text() -> str | None:
    for parent in Path(__file__).resolve().parents:
        candidate = parent / "docs" / "stats-catalogue.md"
        if candidate.exists():
            return candidate.read_text()
    return None


@pytest.mark.parametrize("t", NEW, ids=lambda t: t.id)
def test_every_key_exists_in_the_stats_catalogue(t):
    text = _catalogue_text()
    if text is None:
        pytest.skip("docs/stats-catalogue.md not found from this test file")
    for stat in t.stats:
        key = stat.split(".", 1)[1] if "." in stat else stat
        key = key.split(":")[0]
        assert re.search(rf"`{re.escape(key)}[`:]", text), f"{stat} is not in the catalogue"


# --------------------------------------------------------------------------------------
# 2. Behavior: a stand-in for the generator, then one data set per template
# --------------------------------------------------------------------------------------

SCOPE_TEXT = {
    "all": "in the available Competitive and Unrated match history",
    "shared": "in available matches played with other room players",
    "solo": "in available matches played without other room players",
}


class Skip(Exception):
    """The generator would ask no question."""


@dataclass
class Match:
    at: int  # played_at; also identifies the match across players
    stats: dict
    labels: dict
    team: str | None = "red"
    others: tuple = ()  # other room players in the match (empty means a solo game)


@dataclass(frozen=True)
class Asked:
    name: str
    question: str
    explanation: str


def m(at, stats=None, queue="competitive", team="red", others=(), **labels) -> Match:
    return Match(at, dict(stats or {}), {"queue_id": queue, **labels}, team, tuple(others))


def series(stats: dict, *, queue="competitive", team="red", others=None, **labels):
    """One match per list index, oldest first. A label or team given as a list varies."""
    n = len(next(iter(stats.values())))

    def pick(x, i):
        return x[i] if isinstance(x, list) else x

    return [
        m(
            i + 1,
            {k: v[i] for k, v in stats.items()},
            queue,
            pick(team, i),
            others[i] if others else (),
            **{k: pick(x, i) for k, x in labels.items()},
        )
        for i in range(n)
    ]


def fmt(value: Fraction, places: int = 2) -> str:
    if value.denominator == 1:
        return str(value.numerator)
    d = Decimal(value.numerator) / Decimal(value.denominator)
    return str(d.quantize(Decimal(1).scaleb(-places), rounding=ROUND_HALF_UP))


def _num(match: Match, key: str) -> Fraction:
    if key == "matches":
        return Fraction(1)
    if key == "won":
        result = match.labels.get("result")
        if result is None:
            raise Skip("no result label")
        return Fraction(int(result == "win"))
    if key not in match.stats:
        raise Skip(f"{key} is missing")
    value = Fraction(str(match.stats[key]))
    if value < 0:
        raise Skip(f"{key} is negative")
    return value


def _sum(rows, keys) -> Fraction:
    return sum((_num(r, k) for r in rows for k in keys), Fraction(0))


def _in_scope(match: Match, scope: str) -> bool:
    return scope == "all" or (scope == "shared") == bool(match.others)


def _value(t: QuestionTemplate, rows):
    """(value, numerator, denominator, display label text) for one player's matches."""
    if t.rule == "max_in_match":
        pick = (max if t.direction == "highest" else min)(rows, key=lambda x: _num(x, t.stat))
        shown = ""
        if t.display_label:
            if t.display_label not in pick.labels:
                raise Skip("best match has no display label")
            shown = pick.labels[t.display_label]
        v = _num(pick, t.stat)
        return v, v, Fraction(1), shown
    if t.rule in ("count", "average"):
        total, n = _sum(rows, [t.stat]), Fraction(len(rows))
        return (total if t.rule == "count" else total / n), total, n, ""
    num, den = _sum(rows, t.numerator), _sum(rows, t.denominator)
    if den == 0:
        raise Skip("zero denominator")
    return num / den, num, den, ""


def _ask(t, players, scope, parameter, friend, enough):
    if any("queue_id" not in x.labels for rows in players.values() for x in rows):
        raise Skip("a match has no queue_id")
    eligible = [n for n, rows in players.items() if len(rows) >= enough]
    values: dict[str, tuple] = {}

    if t.rule == "pair":
        friend_team = {
            x.at: x.team for x in players[friend] if x.labels["queue_id"] in t.queues
        }
        for n in eligible:
            if n == friend:
                continue
            together = [
                x
                for x in players[n]
                if x.labels["queue_id"] in t.queues
                and x.team is not None
                and friend_team.get(x.at) == x.team
            ]
            if together:
                total = _sum(together, [f"{t.stat}:{friend}"])
                values[n] = (total, total, Fraction(len(together)), "")
    else:
        for n in eligible:
            rows = [
                x
                for x in players[n]
                if x.labels["queue_id"] in t.queues and _in_scope(x, scope)
            ]
            if not rows or (t.last_n is not None and len(rows) < t.last_n):
                if t.contenders_only:
                    continue
                raise Skip("not enough matches")
            if t.last_n is not None:
                rows = sorted(rows, key=lambda x: x.at, reverse=True)[: t.last_n]
            if t.parameter_label:
                if any(t.parameter_label not in x.labels for x in rows):
                    raise Skip("a match lacks the parameter label")
                rows = [x for x in rows if x.labels[t.parameter_label] == parameter]
                if not rows:
                    if t.contenders_only:
                        continue
                    raise Skip("no matches with that parameter")
            values[n] = _value(t, rows)

    if len(values) < 2:
        raise Skip("fewer than 2 players to compare")
    pick = max if t.direction == "highest" else min
    best = pick(v[0] for v in values.values())
    leaders = [n for n, v in values.items() if v[0] == best]
    if len(leaders) != 1:
        raise Skip("tie for the lead")
    winner = leaders[0]

    def shown(v: Fraction) -> str:
        return fmt(v * 100, 1) if "{percent}" in t.question else fmt(v)

    if any(shown(v[0]) == shown(best) for n, v in values.items() if n != winner):
        raise Skip("the number does not single out the winner")

    value, num, den, display = values[winner]
    fields = dict(
        name=winner,
        value=fmt(value),
        numerator=fmt(num),
        denominator=fmt(den),
        percent=fmt(value * 100, 1),
        scope=SCOPE_TEXT[scope],
        last_n=t.last_n,
        parameter=parameter,
        friend=friend,
    )
    if t.display_label:
        fields[f"{t.display_label}_name"] = display
    return Asked(winner, t.question.format(**fields), t.explanation.format(**fields))


def ask(t, players, *, scope="all", parameter=None, friend=None, enough=5) -> Asked | None:
    try:
        return _ask(t, players, scope, parameter, friend, enough)
    except Skip:
        return None


# ---- 1. longest-kill-v1 ----------------------------------------------------------------


def _longest_kill_players():
    key = "numbers.longest_kill_m"
    return {
        "Rin": series({key: [30, 41.5, 12, 20, 25]}, map="Ascent"),
        "Kai": series(
            {key: [20, 52.25, 33, 18, 40]}, map=["Bind", "Haven", "Bind", "Bind", "Bind"]
        ),
        "Sam": series({key: [38, 22, 19, 15, 10]}, map="Split"),
    }


def test_longest_kill_names_the_longest_shot_and_its_map():
    asked = ask(BY_ID["longest-kill-v1"], _longest_kill_players())
    assert asked == Asked(
        "Kai",
        "One of you once landed a kill from 52.25 meters away in the available Competitive "
        "and Unrated match history, the longest shot in this group. Who has the sniper's eye?",
        "Kai, on Haven: a kill from 52.25 meters out.",
    )


def test_longest_kill_is_skipped_on_a_tie_or_a_missing_map():
    t = BY_ID["longest-kill-v1"]
    tied = _longest_kill_players()
    tied["Sam"][0].stats["numbers.longest_kill_m"] = 52.25
    assert ask(t, tied) is None
    unlabeled = _longest_kill_players()
    del unlabeled["Kai"][1].labels["map"]
    assert ask(t, unlabeled) is None


# ---- 2. utility-assists-v1 -------------------------------------------------------------


def _utility_players():
    key = "numbers.utility_assists"
    return {
        "Rin": series({key: [4, 3, 5, 2, 4, 3, 3, 4, 2, 5]}),
        "Kai": series({key: [3, 3, 2, 4, 3, 3, 2, 3, 3, 4]}),
        "Sam": series({key: [2, 3, 4, 3, 2, 3, 3, 2, 3, 3]}),
    }


def test_utility_assists_totals_the_last_ten_matches():
    asked = ask(BY_ID["utility-assists-v1"], _utility_players())
    assert asked == Asked(
        "Rin",
        "In their last 10 matches, this player earned 35 assists without dealing any damage "
        "to the victim, so flashes and utility did the work, the most in this group. "
        "Who sets everyone else up?",
        "Rin had 35 damage-free assists in their last 10 matches.",
    )


def test_utility_assists_ignores_matches_older_than_the_window():
    players = _utility_players()
    players["Sam"].insert(0, m(0, {"numbers.utility_assists": 50}))
    asked = ask(BY_ID["utility-assists-v1"], players)
    assert asked is not None and asked.name == "Rin"


def test_utility_assists_is_skipped_when_someone_has_fewer_than_ten_matches():
    players = _utility_players()
    players["Sam"] = players["Sam"][:8]
    assert ask(BY_ID["utility-assists-v1"], players) is None


# ---- 3. underdog-kill-share-v1 ---------------------------------------------------------


def test_underdog_share_is_pooled_and_shown_as_a_percentage():
    key = "numbers.outgunned_kills"
    players = {
        "Rin": series({"kills": [20, 15, 18, 22, 17], key: [6, 4, 5, 7, 3]}),
        "Kai": series({"kills": [18, 20, 16, 19, 17], key: [3, 4, 2, 5, 3]}),
        "Sam": series({"kills": [21, 14, 19, 16, 20], key: [5, 6, 4, 3, 5]}),
    }
    asked = ask(BY_ID["underdog-kill-share-v1"], players)
    assert asked == Asked(
        "Rin",
        "27.2% of this player's kills came against a better-equipped opponent in the "
        "available Competitive and Unrated match history, the highest share in this "
        "group. Who loves playing the underdog?",
        "Rin: 25 of 92 kills against a pricier loadout.",
    )


def test_underdog_share_is_skipped_when_the_percentages_round_to_the_same_text():
    key = "numbers.outgunned_kills"
    players = {
        "Rin": series({"kills": [20, 20, 20, 20, 20], key: [5, 5, 5, 5, 5]}),
        "Kai": series({"kills": [20, 20, 20, 20, 20], key: [5, 5, 5, 5, 4]}),
        "Sam": series({"kills": [20, 20, 20, 20, 20], key: [1, 1, 1, 1, 1]}),
    }
    assert ask(BY_ID["underdog-kill-share-v1"], players) is not None
    players["Kai"][4].stats[key] = 5  # now Rin and Kai tie exactly
    assert ask(BY_ID["underdog-kill-share-v1"], players) is None


# ---- 4. role-utility-per-round-v1 ------------------------------------------------------


def _role_players():
    def casts(rounds, g, a1, a2, role):
        return series(
            {
                "numbers.rounds_played": rounds,
                "numbers.ability_casts_grenade": g,
                "numbers.ability_casts_ability1": a1,
                "numbers.ability_casts_ability2": a2,
            },
            role=role,
        )

    return {
        "Rin": casts(
            [20, 22, 20, 18, 24],
            [20, 22, 5, 18, 5],
            [30, 33, 9, 27, 9],
            [11, 11, 2, 9, 2],
            ["Controller", "Controller", "Duelist", "Controller", "Sentinel"],
        ),
        "Kai": casts(
            [24, 20, 20, 20, 20],
            [20, 3, 3, 18, 3],
            [24, 3, 3, 20, 3],
            [8, 2, 2, 6, 2],
            ["Controller", "Duelist", "Duelist", "Controller", "Duelist"],
        ),
        "Sam": casts([20] * 5, [10] * 5, [10] * 5, [10] * 5, "Duelist"),
    }


def test_role_utility_compares_only_matches_in_that_role_and_lets_non_players_sit_out():
    asked = ask(BY_ID["role-utility-per-round-v1"], _role_players(), parameter="Controller")
    assert asked == Asked(
        "Rin",
        "Playing Controller in the available Competitive and Unrated match history, this "
        "player casts 3.02 non-ultimate abilities per round, the most of anyone here who "
        "has played that role. Who keeps the utility flowing?",
        "Rin: 181 ability casts in 60 rounds as Controller.",
    )


def test_role_utility_is_skipped_when_a_match_has_no_role_label():
    players = _role_players()
    del players["Kai"][2].labels["role"]
    assert ask(BY_ID["role-utility-per-round-v1"], players, parameter="Controller") is None


# ---- 5. trades-for-friend-v1 -----------------------------------------------------------


def _pair_players():
    key = "numbers.trades_for:Sam"
    return {
        "Rin": series({key: [3, 2, 4, 9, 3]}, team=["red", "red", "red", "blue", "blue"]),
        "Kai": series({key: [2, 5, 1, 7, 2]}, team=["red", "blue", "red", "blue", "blue"]),
        "Sam": series({"kills": [10, 11, 12, 13, 14]}, team=["red", "red", "red", "red", "blue"]),
    }


def test_trades_for_friend_counts_only_matches_on_the_friends_team():
    asked = ask(BY_ID["trades-for-friend-v1"], _pair_players(), friend="Sam")
    assert asked == Asked(
        "Rin",
        "Someone here has avenged Sam 12 times in the available Competitive and Unrated "
        "match history by killing their killer within 3 seconds, more than anyone else who "
        "has played on Sam's team. Who has Sam's back?",
        "Rin, across 4 matches together.",
    )


def test_trades_for_friend_is_skipped_when_a_shared_match_lacks_the_pair_key():
    players = _pair_players()
    del players["Rin"][0].stats["numbers.trades_for:Sam"]
    assert ask(BY_ID["trades-for-friend-v1"], players, friend="Sam") is None


# ---- 6. kills-on-enemy-mvp-v1 ----------------------------------------------------------


def test_kills_on_enemy_mvp_totals_the_last_ten_matches():
    key = "numbers.kills_on_enemy_mvp"
    players = {
        "Rin": series({key: [3, 2, 4, 3, 2, 3, 4, 2, 3, 3]}),
        "Kai": series({key: [2, 2, 3, 2, 1, 3, 2, 2, 2, 3]}),
        "Sam": series({key: [3, 3, 2, 2, 3, 2, 3, 2, 3, 2]}),
    }
    asked = ask(BY_ID["kills-on-enemy-mvp-v1"], players)
    assert asked == Asked(
        "Rin",
        "Over their last 10 matches, this player took down the other team's MVP 29 times, "
        "the most in this group. Who hunts the star player?",
        "Rin killed the opposing MVP 29 times in their last 10 matches.",
    )


# ---- 7. team-mvps-v1 -------------------------------------------------------------------


def _team_mvp_players():
    key = "numbers.team_mvp"
    return {
        "Rin": series({key: [1, 0, 1, 1, 0, 1, 0, 1, 1, 0]}),
        "Kai": series({key: [0, 1, 0, 1, 0, 0, 1, 0, 1, 0]}),
        "Sam": series({key: [0, 0, 1, 0, 1, 0, 0, 1, 0, 1]}),
    }


def test_team_mvps_counts_awards_in_the_last_ten_matches():
    asked = ask(BY_ID["team-mvps-v1"], _team_mvp_players())
    assert asked == Asked(
        "Rin",
        "This player was their team's MVP in 6 of their last 10 matches, more than anyone "
        "else in this group. Who carries the squad?",
        "Rin was team MVP in 6 of their last 10 matches.",
    )


def test_team_mvps_is_skipped_on_a_tie_for_the_lead():
    players = _team_mvp_players()
    players["Kai"] = series({"numbers.team_mvp": [1, 1, 0, 1, 0, 1, 0, 1, 0, 1]})
    assert ask(BY_ID["team-mvps-v1"], players) is None


# ---- 8. solo-thrifty-rounds-v1 ---------------------------------------------------------


def _thrifty_players():
    key = "numbers.thrifty_rounds"
    solo, with_kai, with_rin = (), ("Kai",), ("Rin",)
    return {
        "Rin": series(
            {key: [3, 9, 2, 9, 2]}, others=[solo, with_kai, solo, with_kai, solo]
        ),
        "Kai": series(
            {key: [1, 9, 2, 9, 2]}, others=[solo, with_rin, solo, with_rin, solo]
        ),
        "Sam": series({key: [4, 4, 4, 4, 4]}, others=[with_kai] * 5),
    }


def test_solo_thrifty_uses_solo_games_only_and_lets_a_player_without_any_sit_out():
    asked = ask(BY_ID["solo-thrifty-rounds-v1"], _thrifty_players(), scope="solo")
    assert asked == Asked(
        "Rin",
        "Queueing without the rest of you, this player's team wins 2.33 thrifty rounds per "
        "match (rounds won against much bigger buys), the most of anyone here who plays "
        "solo. Who wins on a budget?",
        "Rin: 7 thrifty rounds in 3 solo matches.",
    )


def test_solo_thrifty_is_skipped_when_only_one_player_has_solo_games():
    players = _thrifty_players()
    players["Kai"] = series({"numbers.thrifty_rounds": [1] * 5}, others=[("Rin",)] * 5)
    assert ask(BY_ID["solo-thrifty-rounds-v1"], players, scope="solo") is None


# ---- 9. unarmored-rounds-v1 ------------------------------------------------------------


def test_unarmored_rounds_totals_the_last_five_matches_only():
    key = "numbers.rounds_without_armor"
    players = {
        "Rin": series({key: [14, 12, 15, 13, 16]}),
        "Kai": [m(0, {key: 40})] + series({key: [10, 9, 11, 12, 10]}),
        "Sam": series({key: [12, 13, 11, 14, 12]}),
    }
    asked = ask(BY_ID["unarmored-rounds-v1"], players)
    assert asked == Asked(
        "Rin",
        "Across their last 5 matches, this player headed into 70 rounds with no armor, more "
        "than anyone else here. Who's the culprit?",
        "Rin started 70 rounds unarmored over their last 5 matches.",
    )


# ---- 10. most-deaths-to-one-opponent-v1 ------------------------------------------------


def _farmed_players():
    key = "numbers.max_deaths_to_one_opponent"
    return {
        "Rin": series({key: [3, 5, 2, 4, 3]}, agent="Jett"),
        "Kai": series(
            {key: [6, 2, 3, 2, 2]}, agent=["Omen", "Sova", "Sova", "Sova", "Sova"]
        ),
        "Sam": series({key: [4, 4, 3, 5, 2]}, agent="Sage"),
    }


def test_deaths_to_one_opponent_names_the_worst_match_and_agent():
    asked = ask(BY_ID["most-deaths-to-one-opponent-v1"], _farmed_players())
    assert asked == Asked(
        "Kai",
        "One of you once died 6 times to the same opponent in a single match in the "
        "available Competitive and Unrated match history, more than anyone else here. "
        "Who would rather forget this one?",
        "Kai, playing Omen: 6 deaths to one opponent.",
    )


def test_deaths_to_one_opponent_is_skipped_when_the_worst_match_has_no_agent():
    players = _farmed_players()
    del players["Kai"][0].labels["agent"]
    assert ask(BY_ID["most-deaths-to-one-opponent-v1"], players) is None