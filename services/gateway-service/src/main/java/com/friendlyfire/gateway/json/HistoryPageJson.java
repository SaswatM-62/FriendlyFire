package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse;
import java.util.List;

/** GET /api/history: one page of the player's past quizzes, newest first. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HistoryPageJson(
        List<QuizSummaryJson> quizzes,
        /** Left out when there are no more pages. */
        String nextPageToken) {

    public static HistoryPageJson from(GetPlayerHistoryResponse page) {
        return new HistoryPageJson(
                page.getQuizzesList().stream().map(QuizSummaryJson::from).toList(),
                ProtoJson.text(page.getNextPageToken()));
    }
}
