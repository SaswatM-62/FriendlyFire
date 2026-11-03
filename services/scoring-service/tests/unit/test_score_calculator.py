import unittest

from scoring.score_calculator import ScoreCalculator


class TestScoreCalculator(unittest.TestCase):
    def setUp(self):
        """Initializes the pure class before each individual test case runs."""
        self.calculator = ScoreCalculator()

    def test_calculate_points_table(self):
        """Table-driven test suite checking speed scoring boundaries."""
        # Format: (name, is_correct, time_left, time_limit, expected_score)
        test_cases = [
            ("Answering instantly", True, 15.0, 15.0, 1000),
            ("Answering halfway", True, 7.5, 15.0, 750),
            ("Answering exactly at deadline", True, 0.0, 15.0, 500),
            ("Wrong answer gets zero", False, 12.0, 15.0, 0),
            ("No answer", False, 0.0, 15.0, 0),
        ]

        for name, is_correct, time_left, time_limit, expected in test_cases:
            with self.subTest(msg=name):
                actual = self.calculator.calculate_points(is_correct, time_left, time_limit)
                self.assertEqual(actual, expected)

    def test_calculate_ranks_table(self):
        """Table-driven test suite confirming competition ties (1, 1, 3)."""
        # Format: (name, input_scores, expected_rankings)
        test_cases = [
            (
                "Standard strict hierarchy",
                {"p1": 900, "p2": 800, "p3": 700},
                {"p1": 1, "p2": 2, "p3": 3},
            ),
            (
                "Two-way tie for first place",
                {"p1": 850, "p2": 850, "p3": 600},
                {"p1": 1, "p2": 1, "p3": 3},
            ),
            (
                "Three-way tie for all places",
                {"p1": 500, "p2": 500, "p3": 500},
                {"p1": 1, "p2": 1, "p3": 1},
            ),
            ("Empty room handling", {}, {}),
        ]

        for name, input_scores, expected_rankings in test_cases:
            with self.subTest(msg=name):
                actual_rankings = self.calculator.calculate_ranks(input_scores)
                self.assertEqual(actual_rankings, expected_rankings)


if __name__ == "__main__":
    unittest.main()
