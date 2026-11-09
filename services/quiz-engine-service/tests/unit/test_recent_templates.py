"""Specification checks only; generator integration is deliberately pending."""

from string import Formatter

import pytest

from quiz_engine.templates import TEMPLATES, QuestionTemplate


def test_question_bank_has_ten_distinct_new_comparisons() -> None:
    assert len(TEMPLATES) == 10
    assert len({t.id for t in TEMPLATES}) == 10
    assert len({(t.rule, t.parameter_label, t.lowest) for t in TEMPLATES}) == 10


@pytest.mark.parametrize("template", TEMPLATES, ids=lambda t: t.id)
def test_definition_has_complete_wording_and_required_stats(
    template: QuestionTemplate,
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
