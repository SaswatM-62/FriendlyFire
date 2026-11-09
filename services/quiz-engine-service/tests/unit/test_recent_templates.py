"""Specification checks only; generator integration is deliberately pending."""

from string import Formatter

import pytest

from quiz_engine.recent_templates import RECENT_TEMPLATES, RecentQuestionTemplate
from quiz_engine.templates import TEMPLATES


def test_question_bank_has_ten_distinct_new_comparisons() -> None:
    assert len(RECENT_TEMPLATES) == 10
    assert len({t.id for t in RECENT_TEMPLATES}) == 10
    assert len({(t.rule, t.parameter_label, t.lowest) for t in RECENT_TEMPLATES}) == 10
    assert not {t.id for t in RECENT_TEMPLATES}.intersection(t.id for t in TEMPLATES)
    assert not {t.rule for t in RECENT_TEMPLATES}.intersection(t.rule for t in TEMPLATES)


@pytest.mark.parametrize("template", RECENT_TEMPLATES, ids=lambda t: t.id)
def test_definition_has_complete_wording_and_required_stats(
    template: RecentQuestionTemplate,
) -> None:
    assert template.last_n == 10
    assert template.scopes == ("all",)
    assert template.game == "valorant"
    assert template.queues == ("competitive", "unrated")
    assert template.stats
    context = {
        "last_n": 10,
        "parameter": "Jett" if template.parameter_label == "agent" else "Ascent",
        "name": "Alice",
        "value": "2",
        "numerator": "20",
        "denominator": "10",
        "percent": "20",
    }
    fields = {
        name
        for text in (template.question, template.explanation)
        for _, name, _, _ in Formatter().parse(text)
        if name
    }
    assert fields <= context.keys()
    assert "10" in template.question.format(**context)
    assert "Alice" in template.explanation.format(**context)
    if template.parameter_label:
        assert f"labels.{template.parameter_label}" in template.stats
        assert "{parameter}" in template.question
    assert template.lowest == (template.rule == "lowest_adr")


def test_definitions_are_not_registered_until_generator_owner_integrates_them() -> None:
    assert len(TEMPLATES) == 3
    assert not any(t in TEMPLATES for t in RECENT_TEMPLATES)
