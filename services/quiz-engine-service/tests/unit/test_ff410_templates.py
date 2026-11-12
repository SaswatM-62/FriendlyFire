from quiz_engine.templates import TEMPLATES

FF410_IDS = [
    "most-competitive-matches-act-v1",
    "highest-competitive-win-rate-v1",
    "most-kills-single-match-v1",
    "highest-recent-acs-v1",
    "most-competitive-wins-v1",
    "highest-map-acs-v1",
    "most-assists-single-match-v1",
    "most-recent-deaths-v1",
    "most-recent-first-bloods-v1",
    "most-weapon-kills-v1",
]


def test_ff410_has_ten_questions():
    questions = [t for t in TEMPLATES if t.id in FF410_IDS]
    assert len(questions) == 10


def test_ff410_question_ids_are_unique():
    ids = [t.id for t in TEMPLATES]
    assert len(ids) == len(set(ids))


def test_ff410_questions_have_required_fields():
    for template in TEMPLATES:
        if template.id in FF410_IDS:
            assert template.question
            assert template.explanation
            assert template.stats
            assert template.rule in (
                "pooled_ratio",
                "max_in_match",
                "count",
            )


def test_ff410_questions_have_valid_windows():
    for template in TEMPLATES:
        if template.id in FF410_IDS:
            assert template.last_n > 0


def test_ff410_questions_have_valid_queues():
    for template in TEMPLATES:
        if template.id in FF410_IDS:
            assert template.queues
            assert all(
                queue in ("competitive", "unrated")
                for queue in template.queues
            )