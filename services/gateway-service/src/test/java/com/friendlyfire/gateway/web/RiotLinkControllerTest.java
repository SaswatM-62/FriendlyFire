package com.friendlyfire.gateway.web;

import static com.friendlyfire.gateway.testsupport.FakeServices.ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.proto.friendlyfire.account.v1.CompleteRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.StartRiotLinkRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UnlinkRiotRequest;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

class RiotLinkControllerTest extends GatewayTestBase {

    private static final String CALLBACK = "http://localhost:3000/link/riot/callback";

    @Autowired
    private MockMvc mvc;

    @Test
    void startReturnsTheSignOnUrlWithTheFrontendsCallback() throws Exception {
        mvc.perform(post("/api/link/riot/start").header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.authorizeUrl").value("http://localhost:8090/authorize?state=s1&redirect_uri=" + CALLBACK));

        StartRiotLinkRequest request = ACCOUNT.calls.last(StartRiotLinkRequest.class);
        assertThat(request.getUserId()).isEqualTo("user-maya");
        assertThat(request.getRedirectUri()).isEqualTo(CALLBACK);
    }

    @Test
    void completeSavesTheLinkForTheSignedInPlayer() throws Exception {
        String body = complete("maya", "{\"code\": \"c1\", \"state\": \"s1\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.gameName").value("Maya"))
                .andExpect(jsonPath("$.tagLine").value("NA1"))
                .andExpect(jsonPath("$.region").value("na"))
                .andExpect(jsonPath("$.linkedAt").value("2025-11-07T07:20:00Z"))
                .andReturn().getResponse().getContentAsString();

        CompleteRiotLinkRequest request = ACCOUNT.calls.last(CompleteRiotLinkRequest.class);
        assertThat(request.getUserId()).isEqualTo("user-maya");
        assertThat(request.getCode()).isEqualTo("c1");
        assertThat(request.getState()).isEqualTo("s1");
        assertThat(request.getRedirectUri()).isEqualTo(CALLBACK);
        assertThat(body.toLowerCase()).doesNotContain("puuid");
    }

    @Test
    void badOrExpiredStateIs400InvalidLinkState() throws Exception {
        ACCOUNT.calls.fail(CompleteRiotLinkRequest.class, Status.INVALID_ARGUMENT.withDescription("That link request expired."));

        complete("maya", "{\"code\": \"c1\", \"state\": \"old\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_LINK_STATE"))
                .andExpect(jsonPath("$.error.message").value("That link request expired."));
    }

    @Test
    void riotAccountLinkedToSomeoneElseIs409() throws Exception {
        ACCOUNT.calls.fail(CompleteRiotLinkRequest.class, Status.ALREADY_EXISTS);

        complete("maya", "{\"code\": \"c1\", \"state\": \"s1\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("RIOT_ALREADY_LINKED"));
    }

    @Test
    void signOnUnreachableIs503() throws Exception {
        ACCOUNT.calls.fail(CompleteRiotLinkRequest.class, Status.UNAVAILABLE.withDescription("mock-rso-service:8090"));

        complete("maya", "{\"code\": \"c1\", \"state\": \"s1\"}")
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.message").value(ApiErrors.UNAVAILABLE_MESSAGE));
    }

    @Test
    void unlinks() throws Exception {
        mvc.perform(delete("/api/link/riot").header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isNoContent());

        assertThat(ACCOUNT.calls.last(UnlinkRiotRequest.class).getUserId()).isEqualTo("user-maya");
    }

    @Test
    void syncStatusWaitsForAccountsNewRpc() throws Exception {
        mvc.perform(get("/api/link/riot/status").header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("NOT_IMPLEMENTED"));
    }

    private ResultActions complete(String name, String body) throws Exception {
        return mvc.perform(post("/api/link/riot/complete")
                .header(HttpHeaders.AUTHORIZATION, bearer(name))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body));
    }

    private static String bearer(String name) {
        return FakeTokens.bearer(FakeTokens.token(name));
    }
}
