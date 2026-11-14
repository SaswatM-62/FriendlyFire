"""Template contract checks; evaluator integration and runtime fixtures await FF-402."""

from string import Formatter

import pytest

from quiz_engine.templates import TEMPLATES, QuestionTemplate


def test_question_bank_has_unique_ids_and_comparisons() -> None:
    assert len(TEMPLATES) >= 10
    assert len({template.id for template in TEMPLATES}) == len(TEMPLATES)
    assert len(
        {
            (
                template.rule,
                template.stat,
                template.numerator,
                template.denominator,
                template.direction,
                template.parameter_label,
                template.queues,
                template.last_n,
                template.scopes,
            )
            for template in TEMPLATES
        }
    ) == len(TEMPLATES)


@pytest.mark.parametrize("template", TEMPLATES, ids=lambda template: template.id)
def test_question_wording_supplies_value_and_scope(template: QuestionTemplate) -> None:
    context = {
        "last_n": 10,
        "parameter": "Jett" if template.parameter_label == "agent" else "Ascent",
        "name": "Alice",
        "value": "2.5",
        "numerator": "25",
        "denominator": "10",
        "percent": "25",
        "rank_name": "Diamond 1",
    }
    fields = {name for _, name, _, _ in Formatter().parse(template.question) if name}
    assert fields & {"value", "percent", "rank_name"}
    assert template.question.startswith("This player ")
    assert template.question.endswith("Who is it?")
    assert "10" in template.question.format(**context)
    assert "Alice" in template.explanation.format(**context)
    assert "{" not in template.question.format(**context)
    assert template.last_n == 10
    assert template.scopes == ("all",)
    assert template.game == "valorant"
    for jargon in ("pooled", "ADR", "tier", "within"):
        assert jargon not in template.question
    if template.parameter_label:
        assert f"labels.{template.parameter_label}" in template.stats
        assert "{parameter}" in template.question


@pytest.mark.parametrize("template", TEMPLATES, ids=lambda template: template.id)
def test_calculation_inputs_are_declared(template: QuestionTemplate) -> None:
    if template.rule == "pooled_ratio":
        assert template.numerator and template.denominator
        assert not template.stat
        inputs = {*template.numerator, *template.denominator} - {"matches"}
        assert inputs <= set(template.stats)
        assert template.zero_denominator == "skip"
    else:
        assert template.stat
        assert not template.numerator and not template.denominator
        assert template.stat == "matches" or template.stat in template.stats


def template(template_id: str) -> QuestionTemplate:
    return next(template for template in TEMPLATES if template.id == template_id)


def test_rank_uses_competitive_window_and_rank_name() -> None:
    rank = template("highest-recent-rank-v1")
    assert rank.queues == ("competitive",)
    assert rank.rule == "max_in_match"
    assert rank.stat == "numbers.rank_tier"
    assert rank.display_label == "rank"
    assert "labels.rank" in rank.stats
    assert "{rank_name}" in rank.question
    assert "{value}" not in rank.question
    assert "Competitive games" in rank.question


def test_adr_variants_share_one_calculation() -> None:
    low = template("lowest-recent-adr-v1")
    high = template("highest-recent-adr-v1")
    assert low.rule == high.rule == "pooled_ratio"
    assert low.numerator == high.numerator == ("numbers.damage_dealt",)
    assert low.denominator == high.denominator == ("numbers.rounds_played",)
    assert low.direction == "lowest" and high.direction == "highest"


def test_agent_kd_shares_calculation_and_defines_zero_deaths_policy() -> None:
    kd = template("highest-recent-kd-v1")
    agent = template("highest-recent-agent-kd-v1")
    assert kd.rule == agent.rule == "pooled_ratio"
    assert kd.numerator == agent.numerator == ("kills",)
    assert kd.denominator == agent.denominator == ("deaths",)
    assert kd.zero_denominator == agent.zero_denominator == "skip"
    assert kd.parameter_label == ""
    assert agent.parameter_label == "agent"
    assert "zero total deaths" in kd.explanation
    assert "zero total deaths" in agent.explanation


def test_headshot_rate_counts_hits_not_all_fired_shots() -> None:
    headshots = template("highest-recent-headshot-percentage-v1")
    assert headshots.numerator == ("numbers.headshots",)
    assert headshots.denominator == (
        "numbers.headshots",
        "numbers.bodyshots",
        "numbers.legshots",
    )
    assert "of their hits" in headshots.question
    assert "misses are not included" in headshots.explanation


def test_map_win_rate_has_explicit_denominator_and_draw_policy() -> None:
    win_rate = template("highest-recent-map-win-rate-v1")
    assert win_rate.rule == "pooled_ratio"
    assert win_rate.numerator == ("won",)
    assert win_rate.denominator == ("matches",)
    assert "labels.result" in win_rate.stats
    assert "Draws count as losses" in win_rate.explanation
