"""Question pack 1 (FF-404): each template through the real QuestionGenerator.

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
    "highest-kd-v1",
    "headshot-percentage-v1",
    "most-headshots-in-a-match-v1",
    "most-agent-games-v1",
    "map-win-rate-v1",
    "agent-kills-per-round-v1",
    "lowest-adr-v1",
    "highest-adr-v1",
    "kast-v1",
    "round-winning-kills-v1",
)
BY_ID = {t.id: t for t in TEMPLATES}
ALL = "in the available Competitive and Unrated match history"


def row(uid: str, n: int, spec: dict[str, Any]) -> PlayerMatchStats:
    """One player's nth match (newest last). spec: kills, deaths, won, agent, map and
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
        "highest-kd-v1",
        f"For every death {ALL}, this player gets 1.20 kills, the best ratio in this group. "
        "Who comes out on top in their fights?",
        "Alice: 60 kills and 50 deaths, 1.20 per death. Next closest: Bob, 50 kills and 50 deaths, "
        "1 per death.",
        {ALICE: five(kills=12), BOB: five(kills=10), CARA: five(kills=8)},
        ("labels", "queue_id"),
    ),
    Case(
        "headshot-percentage-v1",
        "In their last 5 matches, 25% of this player's hits landed on the head, the best aim "
        "in this group. Who keeps clicking heads?",
        "Alice: 25 headshots out of 100 hits (25%). Next closest: Bob, 15 headshots out of 100 "
        "hits (15%).",
        {
            ALICE: five(headshots=5, bodyshots=10, legshots=5),
            BOB: five(headshots=3, bodyshots=12, legshots=5),
            CARA: five(headshots=2, bodyshots=15, legshots=3),
        },
        ("numbers", "headshots"),
    ),
    Case(
        "most-headshots-in-a-match-v1",
        f"One of you landed 9 headshots in a single match {ALL}, more than anyone else here "
        "has managed. Who was seeing heads that day?",
        "Alice, playing Reyna: 9 headshots in one match. Next closest: Bob, playing Jett: 7 "
        "headshots in one match.",
        {
            ALICE: [*five(headshots=4)[:4], {"headshots": 9, "agent": "Reyna"}],
            BOB: five(headshots=7),
            CARA: five(headshots=5),
        },
        ("numbers", "headshots"),
    ),
    Case(
        "most-agent-games-v1",
        "This player picked Omen in 4 of their last 5 matches, more than anyone else here. "
        "Who is the group's Omen main?",
        "Alice played Omen in 4 of their last 5 matches. Next closest: Bob played Omen in 2 of "
        "their last 5 matches.",
        {
            ALICE: [*five(agent="Omen")[:4], {"agent": "Jett"}],
            BOB: [*five(agent="Omen")[:2], *five(agent="Jett")[:3]],
            CARA: five(agent="Jett"),
        },
        ("labels", "agent"),
    ),
    Case(
        "map-win-rate-v1",
        "On Ascent, this player wins 75% of their matches, the best record of anyone here who "
        "has played it. Who owns Ascent?",
        "Alice won 3 of 4 matches on Ascent (75%). Next closest: Bob won 1 of 5 matches on Ascent "
        "(20%).",
        {
            ALICE: [*five(won=True)[:3], {"won": False}, {"map": "Bind"}],
            BOB: [{"won": True}, *five(won=False)[:4]],
            CARA: five(map="Bind", won=True)[:2] + five(map="Bind")[:3],
        },
        ("labels", "result"),
    ),
    Case(
        "agent-kills-per-round-v1",
        "As Jett, this player averages 0.90 kills per round, the most of anyone here who has "
        "played Jett. Who is the deadliest Jett?",
        "Alice: 90 kills in 100 rounds as Jett, 0.90 per round. Next closest: Bob, 75 kills in 100 "
        "rounds as Jett, 0.75 per round.",
        {
            ALICE: five(kills=18, rounds_played=20),
            BOB: five(kills=15, rounds_played=20),
            CARA: five(kills=30, rounds_played=20, agent="Sage"),
        },
        ("numbers", "rounds_played"),
    ),
    Case(
        "lowest-adr-v1",
        f"This player deals 50 damage per round {ALL}, the lowest in this group. "
        "Who's been going easy on the enemy?",
        "Alice: 5000 damage over 100 rounds, 50 per round. Next closest: Bob, 10000 damage over "
        "100 rounds, 100 per round.",
        {
            ALICE: five(damage_dealt=1000, rounds_played=20),
            BOB: five(damage_dealt=2000, rounds_played=20),
            CARA: five(damage_dealt=3000, rounds_played=20),
        },
        ("numbers", "damage_dealt"),
    ),
    Case(
        "highest-adr-v1",
        f"Nobody here hits harder: this player deals 150 damage per round {ALL}. "
        "Who brings the pain?",
        "Alice: 15000 damage over 100 rounds, 150 per round. Next closest: Bob, 10000 damage over "
        "100 rounds, 100 per round.",
        {
            ALICE: five(damage_dealt=3000, rounds_played=20),
            BOB: five(damage_dealt=2000, rounds_played=20),
            CARA: five(damage_dealt=1000, rounds_played=20),
        },
        ("numbers", "damage_dealt"),
    ),
    Case(
        "kast-v1",
        "Kill, assist, survive or get traded: this player did one of those in 80% of their "
        f"rounds {ALL}, the most consistent in this group. Who always pulls their weight?",
        "Alice made an impact in 80 of 100 rounds (80%). Next closest: Bob made an impact in 70 of "
        "100 rounds (70%).",
        {
            ALICE: five(kast_rounds=16, rounds_played=20),
            BOB: five(kast_rounds=14, rounds_played=20),
            CARA: five(kast_rounds=10, rounds_played=20),
        },
        ("numbers", "kast_rounds"),
    ),
    Case(
        "round-winning-kills-v1",
        "When a round ends with the last enemy down, this player is often the one who did it: "
        "15 times in their last 5 matches, the most in this group. Who closes rounds out?",
        "Alice got the final kill of 15 won rounds in their last 5 matches. Next closest: Bob got "
        "the final kill of 10 won rounds in their last 5 matches.",
        {
            ALICE: five(round_winning_kills=3),
            BOB: five(round_winning_kills=2),
            CARA: five(round_winning_kills=1),
        },
        ("numbers", "round_winning_kills"),
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


def replace_user(stats: PlayerMatchStats, uid: str) -> PlayerMatchStats:
    copy = PlayerMatchStats()
    copy.CopyFrom(stats)
    copy.user_id = uid
    copy.match_id = stats.match_id.replace(stats.user_id, uid, 1)
    copy.labels["team_id"] = f"team-{uid}"
    return copy
