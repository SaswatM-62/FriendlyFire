"""Question pack 7 (FF-410): each template through the real QuestionGenerator.

Every case gives Alice, Bob and Cara five matches each, with Alice the answer. It checks
the exact question and explanation, and that a tie at the top or a missing stat skips
the question.
"""

from dataclasses import dataclass
from typing import Any

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question

from quiz_engine.templates import TEMPLATES, QuestionTemplate
from tests.unit.test_template_rules import ALICE, BOB, CARA, ask

PACK = (
    "most-recent-kills-v1",
    "most-recent-assists-v1",
    "most-recent-deaths-v1",
    "most-recent-plants-v1",
    "most-defuses-v1",
    "highest-recent-kda-v1",
    "most-recent-first-deaths-v1",
    "most-recent-isolated-deaths-v1",
    "most-recent-deathless-rounds-v1",
    "most-sniper-kills-v1",
)
BY_ID = {t.id: t for t in TEMPLATES}
ALL = "in the available Competitive and Unrated match history"


def row(uid: str, n: int, spec: dict[str, Any]) -> PlayerMatchStats:
    """One player's nth match (newest last). spec: kills, deaths, assists, won, agent, map and
    catalogue numbers by bare key."""
    spec = dict(spec)
    won = bool(spec.pop("won", False))
    labels = {
        "queue_id": "competitive",
        "team_id": f"team-{uid}",
        "agent": spec.pop("agent", "Jett"),
        "map": spec.pop("map", "Ascent"),
        "result": "win" if won else "loss",
    }
    stats = PlayerMatchStats(
        user_id=uid,
        match_id=f"{uid}-{n}",
        game="valorant",
        kills=int(spec.pop("kills", 10)),
        deaths=int(spec.pop("deaths", 10)),
        assists=int(spec.pop("assists", 0)),
        won=won,
        labels=labels,
        numbers={key: float(value) for key, value in spec.items()},
    )
    stats.played_at.seconds = 1_000 + n
    return stats


def five(**spec: Any) -> list[dict[str, Any]]:
    return [spec] * 5


@dataclass(frozen=True)
class Case:
    id: str
    question: str
    explanation: str
    # Each player's five matches, oldest first; Alice is the answer.
    matches: dict[str, list[dict[str, Any]]]
    # Removed from Alice's oldest match: ("numbers" or "labels", key).
    missing: tuple[str, str]

    def rows(self) -> list[PlayerMatchStats]:
        return [
            row(uid, n, spec) for uid, specs in self.matches.items() for n, spec in enumerate(specs)
        ]


CASES = [
    Case(
        "most-recent-kills-v1",
        "Across their last 5 matches, this player piled up 100 kills, more than anyone else "
        "here. Who's been on a tear lately?",
        "Alice: 100 kills in 5 matches. Next closest: Bob, 75 kills in 5 matches.",
        {ALICE: five(kills=20), BOB: five(kills=15), CARA: five(kills=10)},
        ("labels", "queue_id"),
    ),
    Case(
        "most-recent-assists-v1",
        "Nobody here has helped out more lately: this player picked up 40 assists in their "
        "last 5 matches. Who is the ultimate teammate?",
        "Alice: 40 assists in 5 matches. Next closest: Bob, 30 assists in 5 matches.",
        {ALICE: five(assists=8), BOB: five(assists=6), CARA: five(assists=4)},
        ("labels", "queue_id"),
    ),
    Case(
        "most-recent-deaths-v1",
        "Over their last 5 matches, this player died 90 times, more than anyone else here. "
        "Who's spent the most time spectating?",
        "Alice: 90 deaths in 5 matches. Next closest: Bob, 75 deaths in 5 matches.",
        {ALICE: five(deaths=18), BOB: five(deaths=15), CARA: five(deaths=12)},
        ("labels", "queue_id"),
    ),
    Case(
        "most-recent-plants-v1",
        "Someone here planted the spike 15 times in their last 5 matches, the most in this "
        "group. Who keeps the spike moving?",
        "Alice: 15 plants in 5 matches. Next closest: Bob, 10 plants in 5 matches.",
        {ALICE: five(plants=3), BOB: five(plants=2), CARA: five(plants=1)},
        ("numbers", "plants"),
    ),
    Case(
        "most-defuses-v1",
        f"This player has defused the spike 10 times {ALL}, more than anyone else here. "
        "Who does the team trust with the defuse?",
        "Alice: 10 defuses. Next closest: Bob, 5 defuses.",
        {ALICE: five(defuses=2), BOB: five(defuses=1), CARA: five(defuses=0)},
        ("numbers", "defuses"),
    ),
    Case(
        "highest-recent-kda-v1",
        "Counting assists as well as kills, this player gets 2 for every death in their last "
        "5 matches, the best in this group. Who makes every life count?",
        "Alice: 100 kills and assists against 50 deaths, 2 per death. Next closest: Bob, 75 kills "
        "and assists against 50 deaths, 1.50 per death.",
        {
            ALICE: five(kills=15, assists=5),
            BOB: five(kills=10, assists=5),
            CARA: five(kills=8, assists=2),
        },
        ("labels", "queue_id"),
    ),
    Case(
        "most-recent-first-deaths-v1",
        "Rounds keep starting badly for this player: they were the first one down 20 times in "
        "their last 5 matches, more than anyone else here. Who needs to stop peeking first?",
        "Alice was the first player killed in 20 rounds over 5 matches. Next closest: Bob was the "
        "first player killed in 15 rounds over 5 matches.",
        {ALICE: five(first_deaths=4), BOB: five(first_deaths=3), CARA: five(first_deaths=2)},
        ("numbers", "first_deaths"),
    ),
    Case(
        "most-recent-isolated-deaths-v1",
        "With no teammate within 20 meters, this player went down alone 25 times in their last "
        "5 matches, the most in this group. Who keeps wandering off by themselves?",
        "Alice: 25 deaths with no teammate nearby in 5 matches. Next closest: Bob, 20 deaths with "
        "no teammate nearby in 5 matches.",
        {
            ALICE: five(isolated_deaths=5),
            BOB: five(isolated_deaths=4),
            CARA: five(isolated_deaths=3),
        },
        ("numbers", "isolated_deaths"),
    ),
    Case(
        "most-recent-deathless-rounds-v1",
        "This player made it through 40 rounds without dying in their last 5 matches, the most "
        "in this group. Who has the best survival instincts?",
        "Alice survived 40 rounds in 5 matches. Next closest: Bob survived 30 rounds in 5 matches.",
        {
            ALICE: five(deathless_rounds=8),
            BOB: five(deathless_rounds=6),
            CARA: five(deathless_rounds=4),
        },
        ("numbers", "deathless_rounds"),
    ),
    Case(
        "most-sniper-kills-v1",
        f"Behind a scope, this player has picked up 15 kills {ALL}, more than anyone else "
        "here. Who's the group's sniper?",
        "Alice: 15 kills with sniper rifles. Next closest: Bob, 5 kills with sniper rifles.",
        {ALICE: five(sniper_kills=3), BOB: five(sniper_kills=1), CARA: five(sniper_kills=0)},
        ("numbers", "sniper_kills"),
    ),
]


def questions(case: Case, stats: list[PlayerMatchStats]) -> list[Question]:
    return ask(BY_ID[case.id], stats, ALICE, BOB, CARA)


def test_the_pack_has_ten_templates() -> None:
    assert [t.id for t in TEMPLATES if t.id in PACK] == list(PACK)
    assert [case.id for case in CASES] == list(PACK)


def test_no_template_in_the_pack_repeats_another_question() -> None:
    # Same rule, stats and direction asks the same question, whatever the window.
    def key(t: QuestionTemplate) -> tuple[object, ...]:
        return (t.rule, t.stat, t.numerator, t.denominator, t.direction)

    others = {key(t): t.id for t in TEMPLATES if t.id not in PACK}
    pack = [BY_ID[i] for i in PACK]
    assert len({key(t) for t in pack}) == len(pack)
    for t in pack:
        assert key(t) not in others, f"{t.id} repeats {others[key(t)]}"


def test_each_question_opens_and_closes_its_own_way() -> None:
    texts = [BY_ID[i].question for i in PACK]
    openings = {" ".join(text.split()[:3]) for text in texts}
    closers = {text.rsplit(". ", 1)[-1].rsplit(": ", 1)[-1] for text in texts}
    assert len(openings) == len(PACK)
    assert len(closers) == len(PACK)


@pytest.mark.parametrize("template_id", PACK)
def test_stats_list_everything_the_template_reads(template_id: str) -> None:
    t = BY_ID[template_id]
    read = {t.stat, *t.numerator, *t.denominator} - {""}
    assert read <= set(t.stats)
    assert "labels.queue_id" in t.stats
    assert ("played_at" in t.stats) == (t.last_n is not None)
    if t.parameter_label:
        assert f"labels.{t.parameter_label}" in t.stats
    if t.display_label:
        assert f"labels.{t.display_label}" in t.stats
    if "won" in read:
        assert "labels.result" in t.stats
    # Players can join a room with five matches.
    assert t.last_n in (None, 5)


@pytest.mark.parametrize("case", CASES, ids=lambda c: c.id)
def test_the_answer_and_the_exact_text(case: Case) -> None:
    by_text = {q.text: q for q in questions(case, case.rows())}

    assert case.question in by_text, sorted(by_text)
    question = by_text[case.question]
    assert question.correct_option_id == ALICE
    assert question.explanation == case.explanation
    assert sorted(o.option_id for o in question.options) == [ALICE, BOB, CARA]


@pytest.mark.parametrize("case", CASES, ids=lambda c: c.id)
def test_a_tie_at_the_top_skips_the_question(case: Case) -> None:
    stats = [r for r in case.rows() if r.user_id != BOB]
    stats += [replace_user(r, BOB) for r in stats if r.user_id == ALICE]

    found = questions(case, stats)

    assert case.question not in {q.text for q in found}
    assert not [q for q in found if q.correct_option_id in (ALICE, BOB)]


@pytest.mark.parametrize("case", CASES, ids=lambda c: c.id)
def test_a_missing_stat_skips_the_question(case: Case) -> None:
    stats = case.rows()
    where, key = case.missing
    del getattr(stats[0], where)[key]  # Alice's oldest match

    found = questions(case, stats)

    assert case.question not in {q.text for q in found}
    assert not [q for q in found if q.correct_option_id == ALICE]


@pytest.mark.parametrize(
    ("template_id", "key", "question", "explanation"),
    [
        (
            "most-defuses-v1",
            "defuses",
            f"This player has defused the spike 1 time {ALL}, more than anyone else here. "
            "Who does the team trust with the defuse?",
            "Alice: 1 defuse. Next closest: Bob, 0 defuses. Cara tied.",
        ),
        (
            "most-sniper-kills-v1",
            "sniper_kills",
            f"Behind a scope, this player has picked up 1 kill {ALL}, more than anyone else "
            "here. Who's the group's sniper?",
            "Alice: 1 kill with sniper rifles. Next closest: Bob, 0 kills with sniper rifles. Cara "
            "tied.",
        ),
    ],
)
def test_a_count_of_one_reads_in_the_singular(
    template_id: str, key: str, question: str, explanation: str
) -> None:
    stats = [row(uid, n, {key: 0}) for uid in (ALICE, BOB, CARA) for n in range(5)]
    stats[0].numbers[key] = 1  # Alice's oldest match

    by_text = {q.text: q for q in ask(BY_ID[template_id], stats, ALICE, BOB, CARA)}

    assert question in by_text, sorted(by_text)
    assert by_text[question].explanation == explanation


def replace_user(stats: PlayerMatchStats, uid: str) -> PlayerMatchStats:
    copy = PlayerMatchStats()
    copy.CopyFrom(stats)
    copy.user_id = uid
    copy.match_id = stats.match_id.replace(stats.user_id, uid, 1)
    copy.labels["team_id"] = f"team-{uid}"
    return copy
