"""Data-only question templates; question_generator.py evaluates them.

Catalogue: docs/stats-catalogue.md. A new question is one more entry here; only a
new kind of calculation needs code in question_generator.py. Templates carry no
difficulty: each generated question is rated easy, medium or hard from how far its
winner leads (question_generator.difficulty).

How a template is evaluated, for every room player who is compared (see below):

1. Rows: the player's matches in the template's game and queues, and in the scope
   (all matches, "shared" ones with 2+ room players, or "solo" ones).
2. Window: the player's newest last_n matches by played_at (fewer than last_n, a
   missing timestamp, or a tie across the boundary skips the question), or every
   stored match when last_n is None. The window is chosen before any filter.
3. Filter: with a parameter_label, one question per value of that label (e.g. per
   agent), keeping only the window's matches with that value. A match missing the
   label skips the question rather than silently dropping out.
4. Value, by rule:
   - max_in_match: the player's best single match for stat (highest, or lowest
     when direction is "lowest"); display_label names a label from that match,
     shown as {<display_label>_name}.
   - count: the sum of stat over the matches ("matches" counts them, "won" counts
     wins). No matches after a filter is a known zero.
   - average: stat averaged per match.
   - pooled_ratio: the sum of the numerator stats over the sum of the denominator
     stats. A zero total denominator is unknown; one zero match still counts.
   - pair: friend-pair counts. One question per room player (the {friend}); each
     other player's value is the sum of the "<stat>:<friend's user ID>" number over
     matches where they were on the friend's team.
5. Compare: the unique highest (or lowest) value wins; a tie skips the question,
   and so does a {value} or {percent} in the question that rounds to the same text
   as the runner-up's (it wouldn't single out the winner). Any player with a
   missing value skips it too, except that with contenders_only players without
   enough matches sit the question out (2+ must remain). "Enough" means at least
   one match in the scope (and at least last_n of them), plus one after the filter.
   Use it for agent or map questions, and for the solo scope: players who mostly
   squad up still have the odd solo game, so pair a solo template with
   contenders_only and a small or no last_n. Pair questions always compare only the
   players who played with the friend.

Every room player is an option in every question, including players who sat it out,
so a question says who it compared ("of anyone here who has played Ascent"). Players
with QuizPlayer.not_enough_matches are never compared: never the answer, a pair
question's friend or the source of a stat. Their matches still count when deciding
whether a match was shared.

Stats: "kills", "deaths", "assists", "won" (needs a labels.result that agrees),
"matches", or "numbers.<key>" from the catalogue.

Placeholders: {name} (winner), {value}, {numerator}, {denominator}, {percent}
(value x 100), {scope}, {last_n}, {parameter}, {friend} and {<display_label>_name}.
Whole numbers show as they are; others round half up to 2 decimals (1.28), or 1
for {percent} (29.7).
"""

from dataclasses import dataclass
from typing import Literal

Scope = Literal["all", "shared", "solo"]
Rule = Literal["max_in_match", "count", "average", "pooled_ratio", "pair"]
Direction = Literal["highest", "lowest"]
ParameterLabel = Literal["", "agent", "map", "role"]

ALL_SCOPES: tuple[Scope, ...] = ("all", "shared", "solo")


@dataclass(frozen=True)
class QuestionTemplate:
    id: str
    question: str
    explanation: str
    # Every catalogue key the template reads, for reviewers.
    stats: tuple[str, ...]
    rule: Rule
    stat: str = ""
    numerator: tuple[str, ...] = ()
    denominator: tuple[str, ...] = ()
    direction: Direction = "highest"
    scopes: tuple[Scope, ...] = ("all",)
    game: str = "valorant"
    queues: tuple[str, ...] = ("competitive", "unrated")
    last_n: int | None = 10
    parameter_label: ParameterLabel = ""
    display_label: str = ""
    contenders_only: bool = False


TEMPLATES = (
    QuestionTemplate(
        id="most-kills-v1",
        question="This player once racked up {value} kills in a single match {scope}, the most "
        "in this group. Who might this player be?",
        explanation="{name}, with {value} kills in one match.",
        stats=("kills", "labels.queue_id"),
        rule="max_in_match",
        stat="kills",
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="highest-acs-v1",
        question="This player averages {value} combat score per round {scope}, the highest "
        "in this group. Who is this player?",
        explanation="{name} had the highest pooled ACS {scope}: {value} "
        "({numerator} total combat score / {denominator} rounds).",
        stats=("numbers.combat_score", "numbers.rounds_played", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.combat_score",),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    QuestionTemplate(
        id="recent-wins-v1",
        question="This player won {value} of their last {last_n} Competitive matches, more "
        "than anyone else in this group. Who keeps winning?",
        explanation="{name} won {value} of their last {last_n} available competitive matches "
        "(draws count as non-wins).",
        stats=("played_at", "labels.queue_id", "labels.result", "won"),
        rule="count",
        stat="won",
        queues=("competitive",),
        last_n=10,
    ),
    # --- Batch 2: ten new, original questions -------------------------------------------
    # 1. Single-match record: longest kill distance, with the map it happened on.
    QuestionTemplate(
        id="longest-kill-v1",
        question="One of you once landed a kill from {value} meters away {scope}, the longest "
        "shot in this group. Who has the sniper's eye?",
        explanation="{name}, on {map_name}: a kill from {value} meters out.",
        stats=("numbers.longest_kill_m", "labels.map", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.longest_kill_m",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="map",
    ),
    # 2. Totals: assists that came from pure utility (no damage on the victim).
    QuestionTemplate(
        id="utility-assists-v1",
        question="In their last {last_n} matches, this player earned {value} assists without "
        "dealing any damage to the victim, so flashes and utility did the work, the most in "
        "this group. Who sets everyone else up?",
        explanation="{name} had {value} damage-free assists in their last {last_n} matches.",
        stats=("numbers.utility_assists", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.utility_assists",
        last_n=10,
    ),
    # 3. Rates: share of kills made against a pricier loadout.
    QuestionTemplate(
        id="underdog-kill-share-v1",
        question="{percent}% of this player's kills came against a better-equipped opponent "
        "{scope}, the highest share in this group. Who loves playing the underdog?",
        explanation="{name}: {numerator} of {denominator} kills against a pricier loadout.",
        stats=("numbers.outgunned_kills", "kills", "labels.queue_id"),
        rule="pooled_ratio",
        numerator=("numbers.outgunned_kills",),
        denominator=("kills",),
        scopes=ALL_SCOPES,
        last_n=None,
    ),
    # 4. Agent, map and role: non-ultimate ability casts per round, one question per role.
    QuestionTemplate(
        id="role-utility-per-round-v1",
        question="Playing {parameter} {scope}, this player casts {value} non-ultimate "
        "abilities per round, the most of anyone here who has played that role. "
        "Who keeps the utility flowing?",
        explanation="{name}: {numerator} ability casts in {denominator} rounds as {parameter}.",
        stats=(
            "numbers.ability_casts_grenade",
            "numbers.ability_casts_ability1",
            "numbers.ability_casts_ability2",
            "numbers.rounds_played",
            "labels.role",
            "labels.queue_id",
        ),
        rule="pooled_ratio",
        numerator=(
            "numbers.ability_casts_grenade",
            "numbers.ability_casts_ability1",
            "numbers.ability_casts_ability2",
        ),
        denominator=("numbers.rounds_played",),
        scopes=ALL_SCOPES,
        last_n=None,
        parameter_label="role",
        contenders_only=True,
    ),
    # 5. Friend pairs: who avenges a given friend's deaths the most.
    QuestionTemplate(
        id="trades-for-friend-v1",
        question="Someone here has avenged {friend} {value} times {scope} by killing their "
        "killer within 3 seconds, more than anyone else who has played on {friend}'s team. "
        "Who has {friend}'s back?",
        explanation="{name}, across {denominator} matches together.",
        stats=("numbers.trades_for:<user_id>", "labels.team_id", "labels.queue_id"),
        rule="pair",
        stat="numbers.trades_for",
        last_n=None,
    ),
    # 6. Opponents: kills on the other team's MVP.
    QuestionTemplate(
        id="kills-on-enemy-mvp-v1",
        question="Over their last {last_n} matches, this player took down the other team's MVP "
        "{value} times, the most in this group. Who hunts the star player?",
        explanation="{name} killed the opposing MVP {value} times in their last {last_n} matches.",
        stats=("numbers.kills_on_enemy_mvp", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.kills_on_enemy_mvp",
        last_n=10,
    ),
    # 7. Scoreboard standing: team MVP awards.
    QuestionTemplate(
        id="team-mvps-v1",
        question="This player was their team's MVP in {value} of their last {last_n} matches, "
        "more than anyone else in this group. Who carries the squad?",
        explanation="{name} was team MVP in {value} of their last {last_n} matches.",
        stats=("numbers.team_mvp", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.team_mvp",
        last_n=10,
    ),
    # 8. Team-shared results: thrifty rounds, solo games only (teammates share the value).
    QuestionTemplate(
        id="solo-thrifty-rounds-v1",
        question="Queueing without the rest of you, this player's team wins {value} thrifty "
        "rounds per match (rounds won against much bigger buys), the most of anyone here "
        "who plays solo. Who wins on a budget?",
        explanation="{name}: {numerator} thrifty rounds in {denominator} solo matches.",
        stats=("numbers.thrifty_rounds", "labels.queue_id"),
        rule="average",
        stat="numbers.thrifty_rounds",
        scopes=("solo",),
        last_n=None,
        contenders_only=True,
    ),
    # 9. Rounds and economy: rounds that started with no armor (a friendly-ribbing stat).
    QuestionTemplate(
        id="unarmored-rounds-v1",
        question="Across their last {last_n} matches, this player headed into {value} rounds with "
        "no armor, more than anyone else here. Who's the culprit?",
        explanation="{name} started {value} rounds unarmored over their last {last_n} matches.",
        stats=("numbers.rounds_without_armor", "played_at", "labels.queue_id"),
        rule="count",
        stat="numbers.rounds_without_armor",
        last_n=5,
    ),
    # 10. Single-match record (a blunder): most deaths to one opponent, with the agent played.
    QuestionTemplate(
        id="most-deaths-to-one-opponent-v1",
        question="One of you once died {value} times to the same opponent in a single match "
        "{scope}, more than anyone else here. Who would rather forget this one?",
        explanation="{name}, playing {agent_name}: {value} deaths to one opponent.",
        stats=("numbers.max_deaths_to_one_opponent", "labels.agent", "labels.queue_id"),
        rule="max_in_match",
        stat="numbers.max_deaths_to_one_opponent",
        scopes=ALL_SCOPES,
        last_n=None,
        display_label="agent",
    ),
)