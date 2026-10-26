package com.friendlyfire.gateway.web;

import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** Past quizzes. */
@RestController
public class HistoryController {

    private final ScoringServiceBlockingStub scoring;

    public HistoryController(ScoringServiceBlockingStub scoring) {
        this.scoring = scoring;
    }

    @GetMapping("/api/history")
    public ResponseEntity<Map<String, String>> history() {
        // TODO(ff): the signed-in user's past quizzes, newest first
        //   - calls ScoringService.GetPlayerHistory; ?pageSize= (default 20, max 50) and
        //     ?pageToken= pass through; 200 {"quizzes": [...], "nextPageToken": "..."}
        //   - uses: scoring
        return NotImplemented.response();
    }

    @GetMapping("/api/quizzes/{quizId}")
    public ResponseEntity<Map<String, String>> quiz(@PathVariable String quizId) {
        // TODO(ff): one finished quiz with its final scoreboard
        //   - calls ScoringService.GetQuizResult; NOT_FOUND -> 404
        //   - uses: scoring
        return NotImplemented.response();
    }
}
