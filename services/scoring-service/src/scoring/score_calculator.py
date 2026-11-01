"""Points per answer."""


class ScoreCalculator:

    def calculate_points(self, is_correct: bool, time_left: float, time_limit: float) -> int:
        """Computes points for an individual's answer submission"""
        if not is_correct or time_left < 0:  
            return 0
        
        score = 500 + 500 * (time_left/time_limit)
        return round(score)

    def calculate_ranks(self, player_scores: dict[str, int]) -> dict[str, int]:
        """Computes ranks based off of score, giving players of the same score the same rank"""
        if len(player_scores) == 0:
            return {}

        sorted_players = sorted(player_scores.items(), key=lambda item: item[1], reverse=True)
        player_ranks = {}

        # Tracks the first player's rank details manually for comparison
        current_rank = 1
        last_score = sorted_players[0][1]
        for index, (key, score) in enumerate(sorted_players):
            # If score drops, the rank updates to reflect the new ranking the player is at
            if score != last_score:
                current_rank = index + 1
                last_score = score

            player_ranks[key] = current_rank

        return player_ranks
