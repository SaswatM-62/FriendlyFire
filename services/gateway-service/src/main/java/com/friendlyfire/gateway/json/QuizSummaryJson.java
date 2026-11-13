package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.scoring.v1.QuizSummary;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoreboardEntry;
import java.util.List;

/** GET /api/quizzes/{quizId}, and each quiz of GET /api/history. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record QuizSummaryJson(
        String quizId,
        String startedAt,
        String finishedAt,
        int totalRounds,
        int roundsPlayed,
        String endReason,
        List<ScoreboardEntryJson> finalScoreboard) {

    public static QuizSummaryJson from(QuizSummary quiz) {
        return new QuizSummaryJson(
                quiz.getQuizId(),
                ProtoJson.timestamp(quiz.hasStartedAt(), quiz.getStartedAt()),
                ProtoJson.timestamp(quiz.hasFinishedAt(), quiz.getFinishedAt()),
                quiz.getTotalRounds(),
                quiz.getRoundsPlayed(),
                ProtoJson.enumName(quiz.getEndReason(), "END_REASON_"),
                quiz.getFinalScoreboardList().stream().map(ScoreboardEntryJson::from).toList());
    }

    public record ScoreboardEntryJson(String userId, String displayName, int totalPoints, int correctAnswers, int rank) {

        static ScoreboardEntryJson from(ScoreboardEntry entry) {
            return new ScoreboardEntryJson(
                    entry.getUserId(),
                    entry.getDisplayName(),
                    entry.getTotalPoints(),
                    entry.getCorrectAnswers(),
                    entry.getRank());
        }
    }
}
