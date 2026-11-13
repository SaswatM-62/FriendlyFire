package com.friendlyfire.gateway.web;

import com.friendlyfire.gateway.json.HistoryPageJson;
import com.friendlyfire.gateway.json.QuizSummaryJson;
import com.friendlyfire.gateway.security.SignedInUser;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.QuizSummary;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Past quizzes (FF-415). */
@RestController
public class HistoryController {

    static final int DEFAULT_PAGE_SIZE = 20;
    static final int MAX_PAGE_SIZE = 50;

    private final ScoringServiceBlockingStub scoring;

    public HistoryController(ScoringServiceBlockingStub scoring) {
        this.scoring = scoring;
    }

    /** The signed-in player's past quizzes, newest first. {@code pageSize} 1–50, default 20. */
    @GetMapping("/api/history")
    public HistoryPageJson history(
            SignedInUser me,
            @RequestParam(defaultValue = "0") int pageSize,
            @RequestParam(defaultValue = "") String pageToken) {
        int size = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return HistoryPageJson.from(scoring.getPlayerHistory(GetPlayerHistoryRequest.newBuilder()
                .setUserId(me.userId())
                .setPageSize(size)
                .setPageToken(pageToken)
                .build()));
    }

    /** One finished quiz with its final scoreboard. 404 if it doesn't exist, 403 if you didn't play in it. */
    @GetMapping("/api/quizzes/{quizId}")
    public QuizSummaryJson quiz(@PathVariable String quizId, SignedInUser me) {
        QuizSummary quiz = scoring.getQuizResult(GetQuizResultRequest.newBuilder().setQuizId(quizId).build()).getSummary();
        boolean played = quiz.getFinalScoreboardList().stream().anyMatch(entry -> entry.getUserId().equals(me.userId()));
        if (!played) {
            throw new ApiException(HttpStatus.FORBIDDEN, "PERMISSION_DENIED", "You didn't play in that quiz.");
        }
        return QuizSummaryJson.from(quiz);
    }
}
