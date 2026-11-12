from quiz_engine.templates import TEMPLATES

FF410_IDS = [
    "most-recent-competitive-games-v1",
    "most-recent-competitive-wins-v1",
    "most-recent-kills-v1",
    "most-recent-assists-v1",
    "most-recent-deaths-v1",
    "most-recent-first-bloods-v1",
    "most-recent-plants-v1",
    "most-recent-defuses-v1",
    "highest-recent-kda-v1",
    "most-recent-first-deaths-v1",
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