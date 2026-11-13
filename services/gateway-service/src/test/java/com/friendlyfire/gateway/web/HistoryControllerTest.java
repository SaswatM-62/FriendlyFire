package com.friendlyfire.gateway.web;

import static com.friendlyfire.gateway.testsupport.FakeServices.SCORING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

class HistoryControllerTest extends GatewayTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void returnsThePlayersPastQuizzes() throws Exception {
        mvc.perform(get("/api/history").header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizzes[0].quizId").value("quiz-1"))
                .andExpect(jsonPath("$.quizzes[0].startedAt").value("2025-11-09T14:53:20Z"))
                .andExpect(jsonPath("$.quizzes[0].endReason").value("COMPLETED"))
                .andExpect(jsonPath("$.quizzes[0].finalScoreboard[0].displayName").value("Maya"))
                .andExpect(jsonPath("$.quizzes[0].finalScoreboard[0].correctAnswers").value(6))
                .andExpect(jsonPath("$.nextPageToken").doesNotExist());

        GetPlayerHistoryRequest request = SCORING.calls.last(GetPlayerHistoryRequest.class);
        assertThat(request.getUserId()).isEqualTo("user-maya");
        assertThat(request.getPageSize()).isEqualTo(20);
        assertThat(request.getPageToken()).isEmpty();
    }

    @Test
    void passesThePageThroughWithAtMost50() throws Exception {
        mvc.perform(get("/api/history?pageSize=500&pageToken=abc").header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isOk());

        GetPlayerHistoryRequest request = SCORING.calls.last(GetPlayerHistoryRequest.class);
        assertThat(request.getPageSize()).isEqualTo(50);
        assertThat(request.getPageToken()).isEqualTo("abc");
    }

    @Test
    void aQuizYouPlayedIn() throws Exception {
        mvc.perform(get("/api/quizzes/quiz-7").header(HttpHeaders.AUTHORIZATION, bearer("sam")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quizId").value("quiz-7"))
                .andExpect(jsonPath("$.roundsPlayed").value(10));

        assertThat(SCORING.calls.last(GetQuizResultRequest.class).getQuizId()).isEqualTo("quiz-7");
    }

    @Test
    void aQuizYouDidntPlayInIs403() throws Exception {
        mvc.perform(get("/api/quizzes/quiz-7").header(HttpHeaders.AUTHORIZATION, bearer("eve")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aQuizThatDoesntExistIs404() throws Exception {
        SCORING.calls.fail(GetQuizResultRequest.class, Status.NOT_FOUND.withDescription("Quiz not found."));

        mvc.perform(get("/api/quizzes/nope").header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    private static String bearer(String name) {
        return FakeTokens.bearer(FakeTokens.token(name));
    }
}
