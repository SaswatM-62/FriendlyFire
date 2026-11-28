"""Question packs 2 to 8 (FF-405 to FF-408, FF-411) and their variants.

Every template runs through the real QuestionGenerator on data built from its own
fields: Alice, Bob and Cara play ten solo matches each, and Alice is the answer. The
tests check the exact question and explanation, and that a tie at the top or a
missing stat skips the question.
"""

from dataclasses import replace

import pytest
from friendlyfire.matchdata.v1.match_data_service_pb2 import PlayerMatchStats
from friendlyfire.quiz.v1.quiz_service_pb2 import Question

from quiz_engine.question_generator import _PLURAL
from quiz_engine.templates import TEMPLATES, QuestionTemplate
from tests.unit.test_template_rules import ALICE, BOB, CARA, ask

PACKS: dict[str, tuple[str, ...]] = {
    "FF-405": (
        "aces-v1",
        "clutches-v1",
        "triple-kill-rounds-v1",
        "four-kill-rounds-v1",
        "kill-less-rounds-v1",
        "double-kills-under-a-second-v1",
        "untouched-multikills-v1",
        "trade-kills-v1",
        "traded-deaths-v1",
        "killed-then-died-v1",
        "revenge-kills-v1",
        "dominations-v1",
        "dominated-by-v1",
        "max-duel-margin-v1",
        "most-kills-on-one-opponent-v1",
    ),
    "FF-406": (
        "weapon-kills-v1",
        "agent-hunter-v1",
        "loadout-rounds-v1",
        "rifle-kill-share-v1",
        "knife-kills-v1",
        "smg-kills-v1",
        "shotgun-kills-v1",
        "machine-gun-kills-v1",
        "eco-pistol-kills-v1",
        "picked-up-gun-kills-v1",
        "alt-fire-kills-v1",
        "ability-kills-v1",
        "ultimate-kills-v1",
        "ultimates-per-match-v1",
        "damage-assists-v1",
        "exit-kills-v1",
    ),
    "FF-407": (
        "fastest-plant-v1",
        "closest-defuse-v1",
        "spike-deaths-v1",
        "fastest-first-blood-v1",
        "average-kill-time-v1",
        "lone-wolf-distance-v1",
        "credits-spent-v1",
        "unspent-credits-v1",
        "cheapest-loadout-v1",
        "damage-per-credits-v1",
        "damage-received-v1",
        "damage-margin-v1",
        "max-round-combat-score-v1",
    ),
    "FF-408": (
        "deathless-streak-v1",
        "kill-streak-rounds-v1",
        "match-mvps-v1",
        "scoreboard-position-v1",
        "team-position-v1",
        "giant-killer-v1",
        "playground-bully-v1",
        "deaths-to-lower-ranked-v1",
        "top-ranked-opponent-kills-v1",
        "bottom-ranked-opponent-kills-v1",
        "rank-swing-v1",
        "kill-swing-v1",
        "performance-swing-v1",
        "most-deaths-in-a-match-v1",
        "most-assists-in-a-match-v1",
        "most-damage-in-a-match-v1",
        "best-performance-match-v1",
        "most-first-bloods-in-a-match-v1",
        "most-plants-in-a-match-v1",
    ),
    "FF-411": (
        "solo-flawless-rounds-v1",
        "solo-team-ace-rounds-v1",
        "solo-eco-rounds-won-v1",
        "solo-pistol-rounds-v1",
        "solo-round-win-rate-v1",
        "solo-overtime-v1",
        "solo-biggest-deficit-v1",
        "assists-for-friend-v1",
        "finishes-for-friend-v1",
        "solo-attack-round-win-rate-v1",
        "solo-defense-round-win-rate-v1",
        "solo-first-half-round-win-rate-v1",
        "solo-second-half-round-win-rate-v1",
    ),
    "lowest": (
        "lowest-kd-v1",
        "lowest-kast-v1",
        "lowest-headshot-percentage-v1",
        "lowest-acs-v1",
        "lowest-performance-score-v1",
        "fewest-recent-deaths-v1",
        "fewest-first-deaths-v1",
        "lowest-damage-received-v1",
        "lowest-opening-duel-win-rate-v1",
        "lowest-clutch-win-rate-v1",
        "lowest-map-win-rate-v1",
    ),
    "sides": (
        "attack-kills-per-round-v1",
        "defense-kills-per-round-v1",
        "attack-survival-v1",
        "defense-survival-v1",
        "attack-adr-v1",
        "defense-adr-v1",
        "attack-kast-v1",
        "defense-kast-v1",
        "attack-first-bloods-v1",
        "defense-first-bloods-v1",
        "attack-first-deaths-v1",
        "defense-first-deaths-v1",
        "attack-assists-per-round-v1",
        "defense-assists-per-round-v1",
        "plants-per-attack-round-v1",
    ),
    "halves": (
        "first-half-kills-v1",
        "second-half-kills-v1",
        "first-half-survival-v1",
        "second-half-survival-v1",
        "first-half-kast-v1",
        "second-half-kast-v1",
        "first-half-assists-v1",
        "second-half-assists-v1",
    ),
    "topped": (
        "topped-match-kills-v1",
        "topped-match-deaths-v1",
        "topped-match-assists-v1",
        "topped-match-headshots-v1",
        "topped-match-legshots-v1",
        "topped-match-damage-v1",
        "topped-match-damage-received-v1",
        "topped-match-combat-score-v1",
        "topped-match-first-bloods-v1",
        "topped-match-first-deaths-v1",
        "topped-match-plants-v1",
        "topped-match-defuses-v1",
        "topped-match-headshot-percentage-v1",
        "topped-match-max-round-damage-v1",
        "topped-match-max-round-kills-v1",
        "topped-team-kills-v1",
        "topped-team-deaths-v1",
        "topped-team-assists-v1",
        "topped-team-first-bloods-v1",
        "topped-team-first-deaths-v1",
        "topped-team-plants-v1",
        "topped-team-defuses-v1",
        "topped-team-damage-received-v1",
    ),
}
NEW = tuple(i for ids in PACKS.values() for i in ids)
BY_ID = {t.id: t for t in TEMPLATES}
SCALARS = ("kills", "deaths", "assists")
MATCHES = 10
# Stats that are 1 or 0 in a match.
FLAGS = ("numbers.topped_", "numbers.match_mvp", "numbers.overtime")
# What rows() names the one key of each keyed family.
KEY_NAMES = {
    "numbers.kills_with_weapon": "Vandal",
    "numbers.rounds_with_loadout": "Vandal",
    "numbers.kills_vs_agent": "Reyna",
}


def reads(t: QuestionTemplate) -> tuple[str, ...]:
    return tuple(name for name in (t.stat, *t.numerator, *t.denominator) if name)


def one_version(t: QuestionTemplate) -> QuestionTemplate:
    """The first scope and window only, so the question text is known."""
    return replace(t, scopes=t.scopes[:1], windows=t.windows[:1])


def rows(t: QuestionTemplate) -> list[PlayerMatchStats]:
    """Ten matches each, where every stat the template compares puts Alice first (or
    last, for "lowest"), Bob second and Cara third. A stat only in the denominator is
    20 for everyone. Values double in odd matches, so every swing differs; a 1-or-0 stat
    is 1 in the first 9, 5 or 2 matches."""
    lead = {ALICE: 2, BOB: 5, CARA: 9} if t.direction == "lowest" else {ALICE: 9, BOB: 5, CARA: 2}
    compared = {t.stat, *t.numerator} - {""}
    flat = set(t.denominator) - compared
    scale = round(1 / t.value_scale)  # 0.001 shows ms as seconds: use whole seconds
    pair = t.rule == "pair"
    out = []
    for uid, level in lead.items():
        for n in range(MATCHES):
            value = level * (2 if n % 2 else 1) * scale
            if t.stat.startswith(FLAGS):
                value = int(n < level)
            numbers: dict[str, float] = {}
            scalars = dict.fromkeys(SCALARS, 20)
            for name in compared | flat:
                v = 20 if name in flat else value
                if name in SCALARS:
                    scalars[name] = v
                elif name.startswith("numbers.") and not pair and t.rule != "keyed":
                    numbers[name.removeprefix("numbers.")] = v
            labels = {
                "queue_id": "competitive",
                "team_id": "Red",
                "agent": "Jett",
                "map": "Ascent",
            }
            if t.rule == "keyed":
                numbers[f"{t.stat.removeprefix('numbers.')}:gun-id"] = value
                labels["name:gun-id"] = KEY_NAMES[t.stat]
            if pair:
                for friend in lead:
                    if friend != uid:
                        numbers[f"{t.stat.removeprefix('numbers.')}:{friend}"] = value
            won = "won" in compared and n < level
            labels["result"] = "win" if won else "loss"
            row = PlayerMatchStats(
                user_id=uid,
                # Pair questions need matches played together; the rest are solo.
                match_id=f"shared-{n}" if pair else f"{uid}-{n}",
                game="valorant",
                won=won,
                labels=labels,
                numbers=numbers,
                **scalars,
            )
            row.played_at.seconds = 1_000 + n
            out.append(row)
    return out


def questions(t: QuestionTemplate, stats: list[PlayerMatchStats]) -> list[Question]:
    return ask(one_version(t), stats, ALICE, BOB, CARA)


def generated(template_id: str) -> Question:
    t = BY_ID[template_id]
    found = questions(t, rows(t))
    if t.rule == "pair":
        # One question per friend; Cara's is the one Alice wins.
        found = [q for q in found if "Cara" in q.text]
    [question] = found
    return question


# The exact text each template produces from rows(). Values: Alice's 9 per match (2 when
# lowest wins), doubled in odd matches; ratios over 20.
EXPECTED: dict[str, tuple[str, str]] = {
    "aces-v1": (
        "Five enemies, one player: someone here has pulled off 135 aces in their recent matches, "
        "more than anyone else. Who wiped out a whole team?",
        "Alice: 135 aces. Next closest: Bob, 75 aces.",
    ),
    "clutches-v1": (
        "Left alone against the odds, this player won 135 clutches in their recent matches, the "
        "most in this group. Who's the clutch king?",
        "Alice: 135 clutches. Next closest: Bob, 75 clutches.",
    ),
    "triple-kill-rounds-v1": (
        "This player got exactly three kills in 72 rounds in their last 5 matches, the most in "
        "this group. Who keeps finding triples?",
        "Alice: 72 three-kill rounds. Next closest: Bob, 40 three-kill rounds.",
    ),
    "four-kill-rounds-v1": (
        "Four kills in a single round, 135 times in their recent matches: nobody here has done it "
        "more. Who was one kill away from an ace?",
        "Alice: 135 four-kill rounds. Next closest: Bob, 75 four-kill rounds.",
    ),
    "kill-less-rounds-v1": (
        "This player went without a kill in 67.5% of their rounds in their recent matches, more "
        "than anyone else here. Who's been a little too quiet?",
        "Alice: no kill in 135 of 200 rounds (67.5%). Next closest: Bob, no kill in 75 of 200 "
        "rounds (37.5%).",
    ),
    "double-kills-under-a-second-v1": (
        "Two kills less than a second apart: this player has done it 135 times in their recent "
        "matches, the most in this group. Whose reflexes are the fastest?",
        "Alice: 135 pairs of kills under a second apart. Next closest: Bob, 75 pairs of kills "
        "under a second apart.",
    ),
    "untouched-multikills-v1": (
        "Without taking a single hit, this player got two or more kills in 135 rounds in their "
        "recent matches, the most in this group. Who wins fights without a scratch?",
        "Alice: 135 untouched multi-kill rounds. Next closest: Bob, 75 untouched multi-kill "
        "rounds.",
    ),
    "trade-kills-v1": (
        "When a teammate went down, this player paid it back within 3 seconds 72 times in their "
        "last 5 matches, the most in this group. Who always gets the trade?",
        "Alice: 72 trade kills in 5 matches. Next closest: Bob, 40 trade kills in 5 matches.",
    ),
    "traded-deaths-v1": (
        "Someone always had this player's back: 72 of their deaths were traded within 3 seconds "
        "in their last 5 matches, the most in this group. Who has the best backup?",
        "Alice: 72 traded deaths in 5 matches. Next closest: Bob, 40 traded deaths in 5 matches.",
    ),
    "killed-then-died-v1": (
        "A kill, then a death within 3 seconds: it happened to this player 72 times in their last "
        "5 matches, more than anyone else here. Who can't enjoy a kill for long?",
        "Alice: 72 kills followed by a quick death. Next closest: Bob, 40 kills followed by a "
        "quick death.",
    ),
    "revenge-kills-v1": (
        "Payback: this player took down an opponent who had killed them 72 times in their last 5 "
        "matches, the most in this group. Who never forgets?",
        "Alice: 72 revenge kills in 5 matches. Next closest: Bob, 40 revenge kills in 5 matches.",
    ),
    "dominations-v1": (
        "This player dominated an opponent, with six or more kills on them than deaths to them, "
        "135 times in their recent matches, the most in this group. Who finds one opponent and "
        "won't let go?",
        "Alice: 135 dominations. Next closest: Bob, 75 dominations.",
    ),
    "dominated-by-v1": (
        "Dominated: an opponent killed this player at least six more times than they died back, "
        "135 times in their recent matches, more than anyone else here. Who has a nemesis?",
        "Alice: dominated by 135 opponents. Next closest: Bob, dominated by 75 opponents.",
    ),
    "max-duel-margin-v1": (
        "One of you once beat a single opponent by 18 kills in one match in their recent matches, "
        "the biggest one-on-one margin in this group. Who owned that matchup?",
        "Alice, playing Jett: up 18 kills on one opponent. Next closest: Bob, playing Jett: up 10 "
        "kills on one opponent.",
    ),
    "most-kills-on-one-opponent-v1": (
        "Someone here killed the same opponent 18 times in a single match in their recent "
        "matches, more than anyone else managed. Who had a favourite target?",
        "Alice, on Ascent: 18 kills on one opponent. Next closest: Bob, on Ascent: 10 kills on one "
        "opponent.",
    ),
    "weapon-kills-v1": (
        "This player has 135 kills with the Vandal in their recent matches, more than anyone else "
        "here. Who's the group's Vandal expert?",
        "Alice: 135 Vandal kills. Next closest: Bob, 75 Vandal kills.",
    ),
    "agent-hunter-v1": (
        "Enemy Reyna players have fallen to this player 135 times in their recent matches, more "
        "than to anyone else here. Who has Reyna's number?",
        "Alice: 135 kills on enemy Reyna players. Next closest: Bob, 75 kills on enemy Reyna "
        "players.",
    ),
    "loadout-rounds-v1": (
        "The Vandal was in this player's hands in 135 rounds in their recent matches, more than "
        "anyone else here. Whose favourite gun is the Vandal?",
        "Alice: 135 rounds with the Vandal. Next closest: Bob, 75 rounds with the Vandal.",
    ),
    "rifle-kill-share-v1": (
        "72% of this player's kills in their last 5 matches came from rifles, the highest share "
        "in this group. Who lives on the Vandal and Phantom?",
        "Alice: 72 of 100 kills with rifles (72%). Next closest: Bob, 40 of 100 kills with rifles "
        "(40%).",
    ),
    "knife-kills-v1": (
        "Melee only: this player has 135 knife kills in their recent matches, more than anyone "
        "else here. Who brings a knife to a gunfight?",
        "Alice: 135 knife kills. Next closest: Bob, 75 knife kills.",
    ),
    "smg-kills-v1": (
        "With an SMG in hand, this player has 135 kills in their recent matches, the most in this "
        "group. Who's happiest spraying on the move?",
        "Alice: 135 SMG kills. Next closest: Bob, 75 SMG kills.",
    ),
    "shotgun-kills-v1": (
        "Up close and personal: 135 shotgun kills in their recent matches for this player, more "
        "than anyone else here. Who hides behind corners?",
        "Alice: 135 shotgun kills. Next closest: Bob, 75 shotgun kills.",
    ),
    "machine-gun-kills-v1": (
        "Heavy artillery: this player has 135 machine-gun kills in their recent matches, more "
        "than anyone else here. Who brings out the big guns?",
        "Alice: 135 kills with the Ares or Odin. Next closest: Bob, 75 kills with the Ares or "
        "Odin.",
    ),
    "eco-pistol-kills-v1": (
        "Outside the pistol rounds, this player still got 72 sidearm kills in their last 5 "
        "matches, the most in this group. Who wins fights on an eco?",
        "Alice: 72 sidearm kills outside pistol rounds. Next closest: Bob, 40 sidearm kills "
        "outside pistol rounds.",
    ),
    "picked-up-gun-kills-v1": (
        "Finders keepers: this player got 72 kills with a gun they picked up in their last 5 "
        "matches, the most in this group. Who makes the best use of other people's guns?",
        "Alice: 72 kills with picked-up guns. Next closest: Bob, 40 kills with picked-up guns.",
    ),
    "alt-fire-kills-v1": (
        "Right-click specialist: 135 alt-fire kills in their recent matches, the most of anyone "
        "here. Who knows their right mouse button?",
        "Alice: 135 alt-fire kills. Next closest: Bob, 75 alt-fire kills.",
    ),
    "ability-kills-v1": (
        "Abilities have done the killing for this player 135 times in their recent matches, more "
        "than for anyone else here. Whose utility hits hardest?",
        "Alice: 135 ability kills. Next closest: Bob, 75 ability kills.",
    ),
    "ultimate-kills-v1": (
        "Ultimates have earned this player 135 kills in their recent matches, the most in this "
        "group. Who makes their ult count?",
        "Alice: 135 kills with their ultimate. Next closest: Bob, 75 kills with their ultimate.",
    ),
    "ultimates-per-match-v1": (
        "This player uses their ultimate 13.50 times per match in their recent matches, the most "
        "in this group. Who's always got ult ready?",
        "Alice: 135 ultimates in 10 matches, 13.50 per match. Next closest: Bob, 75 ultimates in "
        "10 matches, 7.50 per match.",
    ),
    "damage-assists-v1": (
        "This player did the damage before a teammate finished the job 72 times in their last 5 "
        "matches, the most in this group. Who does the dirty work?",
        "Alice: 72 assists after damaging the victim. Next closest: Bob, 40 assists after damaging "
        "the victim.",
    ),
    "exit-kills-v1": (
        "After the spike had already gone off, this player still got 135 kills in their recent "
        "matches, more than anyone else here. Who keeps fighting after the round is over?",
        "Alice: 135 kills after the explosion. Next closest: Bob, 75 kills after the explosion.",
    ),
    "fastest-plant-v1": (
        "One of you planted the spike just 2 seconds into a round in their recent matches, faster "
        "than anyone else here. Who rushed the site?",
        "Alice, on Ascent: a plant 2 seconds in. Next closest: Bob, on Ascent: a plant 5 seconds "
        "in.",
    ),
    "closest-defuse-v1": (
        "Someone here finished a defuse with only 2 seconds left on the spike in their recent "
        "matches, the closest call in this group. Who has such impeccable timing?",
        "Alice, on Ascent: defused with 2 seconds to spare. Next closest: Bob, on Ascent: defused "
        "with 5 seconds to spare.",
    ),
    "spike-deaths-v1": (
        "This player has been caught in the spike explosion 135 times in their recent matches, "
        "more than anyone else here. Who forgot to run?",
        "Alice: 135 spike deaths. Next closest: Bob, 75 spike deaths.",
    ),
    "fastest-first-blood-v1": (
        "Barely 2 seconds into a round, one of you had already drawn first blood in their recent "
        "matches, quicker than anyone else here. Who was first through the door?",
        "Alice, playing Jett: first blood after 2 seconds. Next closest: Bob, playing Jett: first "
        "blood after 5 seconds.",
    ),
    "average-kill-time-v1": (
        "This player's kills come early: 3.20 seconds into the round on average in their last 5 "
        "matches, the earliest in this group. Who plays fast?",
        "Alice: kills 3.20 seconds into a round on average. Next closest: Bob, kills 8 seconds "
        "into a round on average.",
    ),
    "lone-wolf-distance-v1": (
        "When this player gets a kill, their nearest teammate is 14.40 meters away on average in "
        "their last 5 matches, the farthest in this group. Who's the lone wolf?",
        "Alice: 14.40 meters from the nearest teammate. Next closest: Bob, 8 meters from the "
        "nearest teammate.",
    ),
    "credits-spent-v1": (
        "This player spends 14.40 credits a round on average in their last 5 matches, more than "
        "anyone else here. Who's the big spender?",
        "Alice: 14.40 credits a round. Next closest: Bob, 8 credits a round.",
    ),
    "unspent-credits-v1": (
        "This player ends the buy with 14.40 credits still in the bank on average in their last 5 "
        "matches, the most in this group. Who's saving for a rainy day?",
        "Alice: 14.40 credits left over per round. Next closest: Bob, 8 credits left over per "
        "round.",
    ),
    "cheapest-loadout-v1": (
        "This player walks into rounds carrying 3.20 credits of gear on average in their last 5 "
        "matches, the least in this group. Who travels light?",
        "Alice: gear worth 3.20 credits per round on average. Next closest: Bob, gear worth 8 "
        "credits per round on average.",
    ),
    "damage-per-credits-v1": (
        "Bang for the buck: this player deals 14.40 damage for every 1,000 credits spent in their "
        "last 5 matches, the best in this group. Who gets the most out of their money?",
        "Alice: 14.40 damage per 1,000 credits. Next closest: Bob, 8 damage per 1,000 credits.",
    ),
    "damage-received-v1": (
        "This player takes 0.68 damage per round in their recent matches, more than anyone else "
        "here. Who's the group's damage sponge?",
        "Alice: 135 damage taken over 200 rounds, 0.68 per round. Next closest: Bob, 75 damage "
        "taken over 200 rounds, 0.38 per round.",
    ),
    "damage-margin-v1": (
        "In a single match in their recent matches, one of you dealt 18 more damage than they "
        "took, the biggest margin in this group. Who won the damage race?",
        "Alice, playing Jett: 18 more damage dealt than taken. Next closest: Bob, playing Jett: 10 "
        "more damage dealt than taken.",
    ),
    "max-round-combat-score-v1": (
        "A monster round: someone here scored 18 combat score in a single round in their recent "
        "matches, the best in this group. Whose round was that?",
        "Alice, playing Jett: 18 combat score in one round. Next closest: Bob, playing Jett: 10 "
        "combat score in one round.",
    ),
    "deathless-streak-v1": (
        "One of you survived 18 rounds in a row without dying in one match in their recent "
        "matches, the longest streak in this group. Who refused to die?",
        "Alice, on Ascent: 18 rounds in a row without a death. Next closest: Bob, on Ascent: 10 "
        "rounds in a row without a death.",
    ),
    "kill-streak-rounds-v1": (
        "A kill in 18 rounds in a row: someone here managed it in one match in their recent "
        "matches, the longest run in this group. Who was on fire that match?",
        "Alice, playing Jett: a kill in 18 straight rounds. Next closest: Bob, playing Jett: a "
        "kill in 10 straight rounds.",
    ),
    "match-mvps-v1": (
        "This player was the whole lobby's match MVP 4 times in their last 5 matches, more than "
        "anyone else here. Who's the best player on the server?",
        "Alice: match MVP 4 times. Nobody else here did it.",
    ),
    "scoreboard-position-v1": (
        "This player's average spot on the match scoreboard is 3.20 in their last 5 matches, the "
        "closest to the top in this group. Who's rarely far from first place?",
        "Alice: an average scoreboard position of 3.20. Next closest: Bob, an average scoreboard "
        "position of 8.",
    ),
    "team-position-v1": (
        "On their own team, this player's average scoreboard spot is 14.40 in their last 5 "
        "matches, the lowest in this group. Who's been quiet on the scoreboard?",
        "Alice: an average team position of 14.40. Next closest: Bob, an average team position of "
        "8.",
    ),
    "giant-killer-v1": (
        "Giant killer: this player took down higher-ranked opponents 72 times in their last 5 "
        "Competitive matches, the most in this group. Who punches above their weight?",
        "Alice: 72 kills on higher-ranked opponents. Next closest: Bob, 40 kills on higher-ranked "
        "opponents.",
    ),
    "playground-bully-v1": (
        "This player has taken down lower-ranked opponents 72 times in their last 5 Competitive "
        "matches, the most in this group. Who's the playground bully?",
        "Alice: 72 kills on lower-ranked opponents. Next closest: Bob, 40 kills on lower-ranked "
        "opponents.",
    ),
    "deaths-to-lower-ranked-v1": (
        "Lower-ranked opponents got the better of this player 72 times in their last 5 "
        "Competitive matches, more than anyone else here. Who's had a few upsets?",
        "Alice: 72 deaths to lower-ranked opponents. Next closest: Bob, 40 deaths to lower-ranked "
        "opponents.",
    ),
    "top-ranked-opponent-kills-v1": (
        "The best-ranked opponent in the lobby fell to this player 72 times in their last 5 "
        "Competitive matches, more than anyone else here. Who isn't scared of the top player?",
        "Alice: 72 kills on the lobby's highest-ranked opponent. Next closest: Bob, 40 kills on "
        "the lobby's highest-ranked opponent.",
    ),
    "bottom-ranked-opponent-kills-v1": (
        "This player found the lowest-ranked opponent in the lobby 72 times in their last 5 "
        "Competitive matches, more than anyone else here. Who spots the weakest link?",
        "Alice: 72 kills on the lobby's lowest-ranked opponent. Next closest: Bob, 40 kills on the "
        "lobby's lowest-ranked opponent.",
    ),
    "rank-swing-v1": (
        "Up or down, this player's rank swung by 9 divisions between their highest and lowest "
        "Competitive match in the last 30 days, the biggest swing in this group. Whose rank has "
        "been on a rollercoaster?",
        "Alice: 9 divisions between their highest and lowest rank. Next closest: Bob, 5 divisions "
        "between their highest and lowest rank.",
    ),
    "kill-swing-v1": (
        "Between their best and worst match in their recent matches, this player's kill count "
        "swung by 9, the widest gap in this group. Who's the most unpredictable?",
        "Alice: 9 kills between their best and worst match. Next closest: Bob, 5 kills between "
        "their best and worst match.",
    ),
    "performance-swing-v1": (
        "Over their last 5 matches, this player's Performance Score swung by 9 between their best "
        "and worst game, more than anyone else here. Who blows hot and cold?",
        "Alice: 9 points between their best and worst match. Next closest: Bob, 5 points between "
        "their best and worst match.",
    ),
    "most-deaths-in-a-match-v1": (
        "This player once died 18 times in a single match in their recent matches, more than "
        "anyone else here. Who had the roughest game?",
        "Alice, playing Jett: 18 deaths in one match. Next closest: Bob, playing Jett: 10 deaths "
        "in one match.",
    ),
    "most-assists-in-a-match-v1": (
        "18 assists in a single match in their recent matches: nobody here has helped out more. "
        "Who was everyone's wingman that day?",
        "Alice, playing Jett: 18 assists in one match. Next closest: Bob, playing Jett: 10 assists "
        "in one match.",
    ),
    "most-damage-in-a-match-v1": (
        "Someone here dealt 18 damage in a single match in their recent matches, the most anyone "
        "here has managed. Who went on a rampage?",
        "Alice, playing Jett: 18 damage in one match. Next closest: Bob, playing Jett: 10 damage "
        "in one match.",
    ),
    "best-performance-match-v1": (
        "Their best game in their recent matches earned this player a Performance Score of 18, "
        "the highest in this group. Whose game was that?",
        "Alice, on Ascent: a Performance Score of 18. Next closest: Bob, on Ascent: a Performance "
        "Score of 10.",
    ),
    "most-first-bloods-in-a-match-v1": (
        "In one match in their recent matches, this player drew first blood 18 times, more than "
        "anyone else here has. Who kept opening rounds that day?",
        "Alice, playing Jett: 18 first bloods in one match. Next closest: Bob, playing Jett: 10 "
        "first bloods in one match.",
    ),
    "most-plants-in-a-match-v1": (
        "Spike duty: someone here planted it 18 times in a single match in their recent matches, "
        "the most in this group. Who carried the spike that day?",
        "Alice, on Ascent: 18 plants in one match. Next closest: Bob, on Ascent: 10 plants in one "
        "match.",
    ),
    "solo-flawless-rounds-v1": (
        "Playing without the rest of you, this player's team won 135 rounds without losing "
        "anyone, the most in this group. Whose solo teams are the most ruthless?",
        "Alice: 135 flawless rounds in solo matches. Next closest: Bob, 75 flawless rounds in solo "
        "matches.",
    ),
    "solo-team-ace-rounds-v1": (
        "Without the rest of you, this player's team had all five players get a kill in the same "
        "round 135 times, the most in this group. Whose solo teams work together best?",
        "Alice: 135 team-ace rounds. Next closest: Bob, 75 team-ace rounds.",
    ),
    "solo-eco-rounds-won-v1": (
        "In their solo games, this player's team won 135 eco rounds against a full buy, more than "
        "anyone else here. Whose solo teams pull off the most upsets?",
        "Alice: 135 eco rounds won. Next closest: Bob, 75 eco rounds won.",
    ),
    "solo-pistol-rounds-v1": (
        "Away from the group, this player's team wins 13.50 pistol rounds per match, the most of "
        "anyone here who plays solo. Who's lucky with pistols?",
        "Alice: 135 pistol rounds won in 10 solo matches, 13.50 per match. Next closest: Bob, 75 "
        "pistol rounds won in 10 solo matches, 7.50 per match.",
    ),
    "solo-round-win-rate-v1": (
        "Playing solo, this player's team wins 67.5% of its rounds, the best of anyone here who "
        "plays solo. Who carries random teammates?",
        "Alice: 135 of 200 rounds won in solo matches (67.5%). Next closest: Bob, 75 of 200 rounds "
        "won in solo matches (37.5%).",
    ),
    "solo-overtime-v1": (
        "Overtime again: 9 of this player's solo matches went past round 24, more than anyone "
        "else here. Who can't finish a match in regulation?",
        "Alice: 9 overtime matches. Next closest: Bob, 5 overtime matches.",
    ),
    "solo-biggest-deficit-v1": (
        "In a solo match, this player's team once fell 18 rounds behind, the biggest hole anyone "
        "here has been in. Who's been buried the deepest?",
        "Alice, on Ascent: down 18 rounds at one point. Next closest: Bob, on Ascent: down 10 "
        "rounds at one point.",
    ),
    "assists-for-friend-v1": (
        "Someone here has assisted Cara's kills 135 times in their recent matches, more than "
        "anyone else on Cara's team. Who sets up Cara?",
        "Alice: 135 assists on Cara's kills. Next closest: Bob, 75 assists on Cara's kills.",
    ),
    "finishes-for-friend-v1": (
        "Cara did the damage, but this player finished off 135 of those opponents in their recent "
        "matches, more than anyone else on Cara's team. Who steals Cara's kills?",
        "Alice: 135 kills on enemies Cara hit first. Next closest: Bob, 75 kills on enemies Cara "
        "hit first.",
    ),
    "solo-attack-round-win-rate-v1": (
        "Solo on attack, this player's team wins 67.5% of its rounds, the best of anyone here who "
        "plays solo. Whose solo teams break sites?",
        "Alice: 135 of 200 attack rounds won solo (67.5%). Next closest: Bob, 75 of 200 attack "
        "rounds won solo (37.5%).",
    ),
    "solo-defense-round-win-rate-v1": (
        "Solo on defense, this player's team holds 67.5% of its rounds, the best of anyone here "
        "who plays solo. Whose solo teams lock sites down?",
        "Alice: 135 of 200 defense rounds won solo (67.5%). Next closest: Bob, 75 of 200 defense "
        "rounds won solo (37.5%).",
    ),
    "solo-first-half-round-win-rate-v1": (
        "First half, solo: this player's team wins 67.5% of those rounds, the best of anyone here "
        "who plays solo. Whose solo teams start fast?",
        "Alice: 135 of 200 first-half rounds won solo (67.5%). Next closest: Bob, 75 of 200 "
        "first-half rounds won solo (37.5%).",
    ),
    "solo-second-half-round-win-rate-v1": (
        "Second half, solo: this player's team takes 67.5% of those rounds, the best of anyone "
        "here who plays solo. Whose solo teams finish the job?",
        "Alice: 135 of 200 second-half rounds won solo (67.5%). Next closest: Bob, 75 of 200 "
        "second-half rounds won solo (37.5%).",
    ),
    "lowest-kd-v1": (
        "Only 0.15 kills for every death in their recent matches: the lowest ratio in this group. "
        "Who's been in a rough patch?",
        "Alice: 30 kills and 200 deaths, 0.15 per death. Next closest: Bob, 75 kills and 200 "
        "deaths, 0.38 per death.",
    ),
    "lowest-kast-v1": (
        "This player killed, assisted, survived or got traded in just 15% of their rounds in "
        "their recent matches, the lowest in this group. Who needs to get more involved?",
        "Alice: an impact in 30 of 200 rounds (15%). Next closest: Bob, an impact in 75 of 200 "
        "rounds (37.5%).",
    ),
    "lowest-headshot-percentage-v1": (
        "Only 7.4% of this player's hits landed on the head in their last 5 matches, the lowest "
        "in this group. Whose crosshair needs a little lift?",
        "Alice: 16 headshots out of 216 hits (7.4%). Next closest: Bob, 40 headshots out of 240 "
        "hits (16.7%).",
    ),
    "lowest-acs-v1": (
        "This player earns just 0.15 combat score per round in their recent matches, the lowest "
        "in this group. Who's had a quiet stretch?",
        "Alice: 30 combat score over 200 rounds, 0.15 per round. Next closest: Bob, 75 combat "
        "score over 200 rounds, 0.38 per round.",
    ),
    "lowest-performance-score-v1": (
        "This player's Performance Score averages 3 per match in their recent matches, the lowest "
        "in this group. Who's due a big game?",
        "Alice, averaging 3 over 10 matches. Next closest: Bob, averaging 7.50 over 10 matches.",
    ),
    "fewest-recent-deaths-v1": (
        "Only 16 deaths in their last 5 matches: nobody here has died less. Who's the hardest to "
        "pin down?",
        "Alice: 16 deaths in 5 matches. Next closest: Bob, 40 deaths in 5 matches.",
    ),
    "fewest-first-deaths-v1": (
        "In their last 5 matches, this player was the first one down only 16 times, the fewest in "
        "this group. Who never dies first?",
        "Alice: first to die 16 times in 5 matches. Next closest: Bob, first to die 40 times in 5 "
        "matches.",
    ),
    "lowest-damage-received-v1": (
        "Enemies barely touch this player: just 0.15 damage taken per round in their recent "
        "matches, the least in this group. Who's the hardest to hit?",
        "Alice: 30 damage taken over 200 rounds, 0.15 per round. Next closest: Bob, 75 damage "
        "taken over 200 rounds, 0.38 per round.",
    ),
    "lowest-opening-duel-win-rate-v1": (
        "This player wins only 13.0% of their opening duels in their recent matches, the lowest "
        "of anyone here with 10 or more. Who finds the first fight hardest?",
        "Alice: 30 first bloods out of 230 opening duels (13.0%). Next closest: Bob, 75 first "
        "bloods out of 275 opening duels (27.3%).",
    ),
    "lowest-clutch-win-rate-v1": (
        "Last one standing, this player wins only 15% of their clutches in their recent matches. "
        "Nobody here with 5 or more attempts does worse. Who wishes for backup in a clutch?",
        "Alice: 30 clutch wins out of 200 attempts (15%). Next closest: Bob, 75 clutch wins out of "
        "200 attempts (37.5%).",
    ),
    "lowest-map-win-rate-v1": (
        "On Ascent, this player wins only 20% of their matches, the worst record of anyone here "
        "who has played it. Who should avoid Ascent?",
        "Alice won 2 of 10 matches on Ascent (20%). Next closest: Bob won 5 of 10 matches on "
        "Ascent (50%).",
    ),
    "attack-kills-per-round-v1": (
        "On attack, this player gets 0.68 kills per round in their recent matches, the most in "
        "this group. Who leads the charge?",
        "Alice: 135 kills in 200 attack rounds, 0.68 per round. Next closest: Bob, 75 kills in 200 "
        "attack rounds, 0.38 per round.",
    ),
    "defense-kills-per-round-v1": (
        "Holding a site, this player gets 0.68 kills per round in their recent matches, the most "
        "in this group. Who's the wall on defense?",
        "Alice: 135 kills in 200 defense rounds, 0.68 per round. Next closest: Bob, 75 kills in "
        "200 defense rounds, 0.38 per round.",
    ),
    "attack-survival-v1": (
        "This player survives attack rounds better than anyone here: they die in only 15% of them "
        "in their recent matches. Who makes it out alive?",
        "Alice: 30 deaths in 200 attack rounds (15%). Next closest: Bob, 75 deaths in 200 attack "
        "rounds (37.5%).",
    ),
    "defense-survival-v1": (
        "On defense, this player dies in only 15% of rounds in their recent matches, the fewest "
        "in this group. Who's impossible to dig out?",
        "Alice: 30 deaths in 200 defense rounds (15%). Next closest: Bob, 75 deaths in 200 defense "
        "rounds (37.5%).",
    ),
    "attack-adr-v1": (
        "Pushing onto a site, this player deals 0.68 damage per round in their recent matches, "
        "the most in this group. Who hits hardest on attack?",
        "Alice: 135 damage in 200 attack rounds, 0.68 per round. Next closest: Bob, 75 damage in "
        "200 attack rounds, 0.38 per round.",
    ),
    "defense-adr-v1": (
        "This player deals 0.68 damage per round on defense in their recent matches, the most in "
        "this group. Who punishes every push?",
        "Alice: 135 damage in 200 defense rounds, 0.68 per round. Next closest: Bob, 75 damage in "
        "200 defense rounds, 0.38 per round.",
    ),
    "attack-kast-v1": (
        "Attack after attack, this player makes an impact in 67.5% of rounds in their recent "
        "matches, the highest in this group. Who never wastes an attack round?",
        "Alice: an impact in 135 of 200 attack rounds (67.5%). Next closest: Bob, an impact in 75 "
        "of 200 attack rounds (37.5%).",
    ),
    "defense-kast-v1": (
        "Defending, this player has an impact in 67.5% of rounds in their recent matches, more "
        "than anyone else here. Who anchors the defense?",
        "Alice: an impact in 135 of 200 defense rounds (67.5%). Next closest: Bob, an impact in 75 "
        "of 200 defense rounds (37.5%).",
    ),
    "attack-first-bloods-v1": (
        "This player opens 67.5% of attack rounds with the first kill in their recent matches, "
        "the most in this group. Who's the entry fragger?",
        "Alice: first blood in 135 of 200 attack rounds (67.5%). Next closest: Bob, first blood in "
        "75 of 200 attack rounds (37.5%).",
    ),
    "defense-first-bloods-v1": (
        "Holding the line, this player gets the round's first kill in 67.5% of defense rounds in "
        "their recent matches, more than anyone else here. Who catches the rush first?",
        "Alice: first blood in 135 of 200 defense rounds (67.5%). Next closest: Bob, first blood "
        "in 75 of 200 defense rounds (37.5%).",
    ),
    "attack-first-deaths-v1": (
        "This player is the first to fall in 67.5% of attack rounds in their recent matches, more "
        "than anyone else here. Who dry-peeks on attack?",
        "Alice: first death in 135 of 200 attack rounds (67.5%). Next closest: Bob, first death in "
        "75 of 200 attack rounds (37.5%).",
    ),
    "defense-first-deaths-v1": (
        "Too eager on defense: this player goes down first in 67.5% of defense rounds in their "
        "recent matches, the most in this group. Who keeps peeking too early?",
        "Alice: first death in 135 of 200 defense rounds (67.5%). Next closest: Bob, first death "
        "in 75 of 200 defense rounds (37.5%).",
    ),
    "attack-assists-per-round-v1": (
        "When attacking, this player picks up 0.68 assists per round in their recent matches, the "
        "most in this group. Who sets up every execute?",
        "Alice: 135 assists in 200 attack rounds, 0.68 per round. Next closest: Bob, 75 assists in "
        "200 attack rounds, 0.38 per round.",
    ),
    "defense-assists-per-round-v1": (
        "This player chips in 0.68 assists per round on defense in their recent matches, more "
        "than anyone else here. Who helps hold every site?",
        "Alice: 135 assists in 200 defense rounds, 0.68 per round. Next closest: Bob, 75 assists "
        "in 200 defense rounds, 0.38 per round.",
    ),
    "plants-per-attack-round-v1": (
        "Whatever the site, this player plants the spike in 67.5% of their attack rounds in their "
        "recent matches, more than anyone else here. Who gets the spike down?",
        "Alice: 135 plants in 200 attack rounds (67.5%). Next closest: Bob, 75 plants in 200 "
        "attack rounds (37.5%).",
    ),
    "first-half-kills-v1": (
        "In the first half of a match, this player gets 0.68 kills per round in their recent "
        "matches, the most in this group. Who starts strong?",
        "Alice: 135 kills in 200 first-half rounds, 0.68 per round. Next closest: Bob, 75 kills in "
        "200 first-half rounds, 0.38 per round.",
    ),
    "second-half-kills-v1": (
        "After the side swap, this player gets 0.68 kills per round in their recent matches, the "
        "most in this group. Who finishes strong?",
        "Alice: 135 kills in 200 second-half rounds, 0.68 per round. Next closest: Bob, 75 kills "
        "in 200 second-half rounds, 0.38 per round.",
    ),
    "first-half-survival-v1": (
        "This player dies in only 15% of first-half rounds in their recent matches, the fewest in "
        "this group. Who's careful early on?",
        "Alice: 30 deaths in 200 first-half rounds (15%). Next closest: Bob, 75 deaths in 200 "
        "first-half rounds (37.5%).",
    ),
    "second-half-survival-v1": (
        "In the second half, this player dies in only 15% of rounds in their recent matches, the "
        "fewest in this group. Who tightens up after the swap?",
        "Alice: 30 deaths in 200 second-half rounds (15%). Next closest: Bob, 75 deaths in 200 "
        "second-half rounds (37.5%).",
    ),
    "first-half-kast-v1": (
        "This player makes an impact in 67.5% of first-half rounds in their recent matches, the "
        "highest in this group. Who's switched on from round one?",
        "Alice: an impact in 135 of 200 first-half rounds (67.5%). Next closest: Bob, an impact in "
        "75 of 200 first-half rounds (37.5%).",
    ),
    "second-half-kast-v1": (
        "Second half, this player has an impact in 67.5% of rounds in their recent matches, more "
        "than anyone else here. Who gets better as the match goes on?",
        "Alice: an impact in 135 of 200 second-half rounds (67.5%). Next closest: Bob, an impact "
        "in 75 of 200 second-half rounds (37.5%).",
    ),
    "first-half-assists-v1": (
        "Early in a match, this player sets up 0.68 assists per round in their recent matches, "
        "the most in this group. Who supports from the start?",
        "Alice: 135 assists in 200 first-half rounds, 0.68 per round. Next closest: Bob, 75 "
        "assists in 200 first-half rounds, 0.38 per round.",
    ),
    "second-half-assists-v1": (
        "This player picks up 0.68 assists per round in the second half in their recent matches, "
        "the most in this group. Who helps close matches out?",
        "Alice: 135 assists in 200 second-half rounds, 0.68 per round. Next closest: Bob, 75 "
        "assists in 200 second-half rounds, 0.38 per round.",
    ),
    "topped-match-kills-v1": (
        "This player had the most kills in the whole lobby 4 times in their last 5 matches, more "
        "than anyone else here. Who tops the frag chart?",
        "Alice: top fragger in 4 matches. Nobody else here did it.",
    ),
    "topped-match-deaths-v1": (
        "Most deaths in the whole lobby: that was this player 4 times in their last 5 matches, "
        "more often than anyone else here. Who keeps topping the wrong list?",
        "Alice: most deaths in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-assists-v1": (
        "This player led the whole lobby in assists 4 times in their last 5 matches, more than "
        "anyone else here. Who's the server's best teammate?",
        "Alice: most assists in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-headshots-v1": (
        "Most headshots in the lobby: this player managed it 4 times in their last 5 matches, "
        "more than anyone else here. Who out-aims the whole server?",
        "Alice: most headshots in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-legshots-v1": (
        "This player hit more legs than anyone else in the lobby 4 times in their last 5 matches, "
        "the most in this group. Who aims at the ankles?",
        "Alice: most leg shots in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-damage-v1": (
        "This player dealt the most damage in the lobby 4 times in their last 5 matches, more "
        "than anyone else here. Who leaves everyone low?",
        "Alice: most damage in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-damage-received-v1": (
        "This player took the most damage in the lobby 4 times in their last 5 matches, "
        "more often than anyone else here. Who's the human shield?",
        "Alice: most damage taken in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-combat-score-v1": (
        "This player topped the lobby in combat score 4 times in their last 5 matches, more than "
        "anyone else here. Who tops the scoreboard?",
        "Alice: top combat score in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-first-bloods-v1": (
        "This player opened more rounds than anyone in the lobby 4 times in their last 5 matches, "
        "the most here. Who starts every fight?",
        "Alice: most first bloods in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-first-deaths-v1": (
        "First to fall, again: this player led the lobby in first deaths 4 times in "
        "their last 5 matches, more often than anyone else here. Who's always first to go?",
        "Alice: most first deaths in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-plants-v1": (
        "This player planted more than anyone else in the lobby 4 times in their last 5 matches, "
        "the most here. Who carries the spike?",
        "Alice: most plants in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-defuses-v1": (
        "This player out-defused the whole lobby 4 times in their last 5 matches, more than "
        "anyone else here. Who's on defuse duty?",
        "Alice: most defuses in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-headshot-percentage-v1": (
        "Cleanest aim in the lobby: this player had the best headshot rate 4 times in their last "
        "5 matches, more than anyone else here. Whose aim is the cleanest?",
        "Alice: best headshot rate in the lobby 4 times. Nobody else here did it.",
    ),
    "topped-match-max-round-damage-v1": (
        "The biggest single round of damage in the lobby belonged to this player 4 times in their "
        "last 5 matches, the most here. Who has the biggest rounds?",
        "Alice: the lobby's biggest damage round 4 times. Nobody else here did it.",
    ),
    "topped-match-max-round-kills-v1": (
        "Best kill round in the lobby: this player had it 4 times in their last 5 matches, more "
        "than anyone else here. Who has the biggest multi-kills?",
        "Alice: the lobby's best kill round 4 times. Nobody else here did it.",
    ),
    "topped-team-kills-v1": (
        "On their own team, this player had the most kills 4 times in their last 5 matches, more "
        "than anyone else here. Who's the team's top fragger?",
        "Alice: their team's top fragger 4 times. Nobody else here did it.",
    ),
    "topped-team-deaths-v1": (
        "This player died more than any of their teammates 4 times in their last 5 matches, the "
        "most here. Who's had the toughest time on their team?",
        "Alice: most deaths on their team 4 times. Nobody else here did it.",
    ),
    "topped-team-assists-v1": (
        "Their team's assist leader 4 times in their last 5 matches: nobody here has done it "
        "more. Who makes their teammates look good?",
        "Alice: most assists on their team 4 times. Nobody else here did it.",
    ),
    "topped-team-first-bloods-v1": (
        "This player got the most first bloods on their team 4 times in their last 5 matches, "
        "more than anyone else here. Who's the team's opener?",
        "Alice: most first bloods on their team 4 times. Nobody else here did it.",
    ),
    "topped-team-first-deaths-v1": (
        "This player was their team's most frequent first death 4 times in their last 5 matches, "
        "the most here. Who's always first in for the team?",
        "Alice: most first deaths on their team 4 times. Nobody else here did it.",
    ),
    "topped-team-plants-v1": (
        "Spike carrier: this player planted more than any teammate 4 times in their last 5 "
        "matches, the most here. Who always ends up with the spike?",
        "Alice: most plants on their team 4 times. Nobody else here did it.",
    ),
    "topped-team-defuses-v1": (
        "This player defused more than any teammate 4 times in their last 5 matches, more than "
        "anyone else here. Who sticks the defuse for the team?",
        "Alice: most defuses on their team 4 times. Nobody else here did it.",
    ),
    "topped-team-damage-received-v1": (
        "This player soaked up more damage than any teammate 4 times in their last 5 matches, the "
        "most here. Who takes the hits for the team?",
        "Alice: most damage taken on their team 4 times. Nobody else here did it.",
    ),
}


def test_there_are_133_new_templates_in_pack_order() -> None:
    assert len(NEW) == len(set(NEW)) == 133
    assert [t.id for t in TEMPLATES if t.id in NEW] == list(NEW)


@pytest.mark.parametrize("pack", PACKS)
def test_each_question_in_a_pack_opens_and_closes_its_own_way(pack: str) -> None:
    texts = [_PLURAL.sub(r"\2", BY_ID[i].question) for i in PACKS[pack]]
    openings = [" ".join(text.split()[:3]) for text in texts]
    assert len(set(openings)) == len(openings), sorted(openings)
    closers = [text.rsplit(". ", 1)[-1].rsplit(": ", 1)[-1] for text in texts]
    assert len(set(closers)) == len(closers), sorted(closers)


def test_no_new_question_closes_like_another_question() -> None:
    def closer(t: QuestionTemplate) -> str:
        return t.question.rsplit(". ", 1)[-1].rsplit(": ", 1)[-1]

    seen: dict[str, str] = {}
    for t in TEMPLATES:
        if t.id in NEW:
            assert closer(t) not in seen, f"{t.id} closes like {seen[closer(t)]}"
        seen.setdefault(closer(t), t.id)


@pytest.mark.parametrize("template_id", NEW)
def test_stats_list_everything_the_template_reads(template_id: str) -> None:
    t = BY_ID[template_id]
    declared = {s.split(":<")[0] for s in t.stats}
    assert set(reads(t)) <= declared
    assert "labels.queue_id" in t.stats
    windowed = t.last_n is not None or any(n is not None for n in t.windows)
    assert ("played_at" in t.stats) == windowed
    for label in (t.parameter_label, t.display_label):
        if label:
            assert f"labels.{label}" in t.stats
    if "won" in reads(t):
        assert "labels.result" in t.stats
    if t.rule == "keyed":
        assert "labels.name:<key>" in t.stats
    # Players can join a room with five matches.
    assert t.last_n in (None, 5)


@pytest.mark.parametrize("template_id", NEW)
def test_the_answer_and_the_exact_text(template_id: str) -> None:
    question = generated(template_id)
    assert question.correct_option_id == ALICE
    assert (question.text, question.explanation) == EXPECTED[template_id]
    assert sorted(o.option_id for o in question.options) == [ALICE, BOB, CARA]


@pytest.mark.parametrize("template_id", NEW)
def test_a_tie_at_the_top_skips_the_question(template_id: str) -> None:
    t = BY_ID[template_id]
    stats = [r for r in rows(t) if r.user_id != BOB]
    stats += [as_player(r, BOB) for r in stats if r.user_id == ALICE]
    found = questions(t, stats)
    assert EXPECTED[template_id][0] not in {q.text for q in found}
    if t.rule != "pair":
        assert not [q for q in found if q.correct_option_id in (ALICE, BOB)]


def missing_key(t: QuestionTemplate) -> str | None:
    """A stat to remove from Alice's newest match that makes her value unknown, or None
    where a missing stat is allowed: scalars always exist, absent_is_zero and keyed
    count a missing key as 0, and skip_missing leaves the match out."""
    if t.absent_is_zero or t.skip_missing or t.rule in ("keyed", "pair"):
        return None
    name = (t.numerator or (t.stat,))[0]
    if name in SCALARS or name == "matches":
        return None
    return name


@pytest.mark.parametrize("template_id", [i for i in NEW if missing_key(BY_ID[i])])
def test_a_missing_stat_skips_the_question(template_id: str) -> None:
    t = BY_ID[template_id]
    stats = rows(t)
    newest = max((r for r in stats if r.user_id == ALICE), key=lambda r: r.played_at.seconds)
    name = missing_key(t)
    if name == "won":
        del newest.labels["result"]
    else:
        del newest.numbers[str(name).removeprefix("numbers.")]
    found = questions(t, stats)
    assert EXPECTED[template_id][0] not in {q.text for q in found}
    assert not [q for q in found if q.correct_option_id == ALICE]


def as_player(stats: PlayerMatchStats, uid: str) -> PlayerMatchStats:
    copy = PlayerMatchStats()
    copy.CopyFrom(stats)
    copy.user_id = uid
    copy.match_id = stats.match_id.replace(stats.user_id, uid, 1)
    return copy
