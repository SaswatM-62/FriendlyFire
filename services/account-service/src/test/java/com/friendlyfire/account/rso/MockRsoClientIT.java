package com.friendlyfire.account.rso;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.containing;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

/** MockRsoClient against a WireMock server standing in for mock-rso-service. */
class MockRsoClientIT {

    private static final String CALLBACK = "http://localhost:3000/link/riot/callback";

    private WireMockServer server;
    private MockRsoClient rso;

    @BeforeEach
    void start() {
        server = new WireMockServer(options().dynamicPort());
        server.start();
        rso = new MockRsoClient(RestClient.builder().baseUrl(server.baseUrl()).build(), "http://localhost:8090");
    }

    @AfterEach
    void stop() {
        server.stop();
    }

    @Test
    void swapsTheCodeForATokenAndTheTokenForTheRiotAccount() {
        server.stubFor(post("/token")
                .withRequestBody(containing("grant_type=authorization_code"))
                .withRequestBody(containing("code=good-code"))
                .withRequestBody(containing("redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Flink%2Friot%2Fcallback"))
                .willReturn(okJson("{\"access_token\": \"token-1\", \"token_type\": \"Bearer\", \"expires_in\": 300}")));
        server.stubFor(get("/riot/account/v1/accounts/me")
                .withHeader("Authorization", equalTo("Bearer token-1"))
                .willReturn(okJson("{\"puuid\": \"puuid-1\", \"gameName\": \"Sam\", \"tagLine\": \"NA1\", \"region\": \"na\"}")));

        RiotAccount account = rso.account("good-code", CALLBACK);

        assertThat(account).isEqualTo(new RiotAccount("puuid-1", "Sam", "NA1", "na"));
    }

    @Test
    void aCodeTheMockRejectsIsInvalidArgument() {
        server.stubFor(post("/token").willReturn(aResponse().withStatus(400)
                .withHeader("Content-Type", "application/json").withBody("{\"error\": \"invalid_grant\"}")));

        assertStatus(() -> rso.account("used-code", CALLBACK), Status.Code.INVALID_ARGUMENT);
    }

    @Test
    void aBrokenMockIsUnavailable() {
        server.stubFor(post("/token").willReturn(serverError()));

        assertStatus(() -> rso.account("good-code", CALLBACK), Status.Code.UNAVAILABLE);
    }

    @Test
    void aRefusedTokenIsUnavailable() {
        server.stubFor(post("/token").willReturn(okJson("{\"access_token\": \"token-1\"}")));
        server.stubFor(get("/riot/account/v1/accounts/me").willReturn(aResponse().withStatus(401)));

        assertStatus(() -> rso.account("good-code", CALLBACK), Status.Code.UNAVAILABLE);
    }

    @Test
    void anIncompleteAccountIsUnavailable() {
        server.stubFor(post("/token").willReturn(okJson("{\"access_token\": \"token-1\"}")));
        server.stubFor(get("/riot/account/v1/accounts/me")
                .willReturn(okJson("{\"puuid\": \"puuid-1\", \"gameName\": \"Sam\", \"tagLine\": \"NA1\"}")));

        assertStatus(() -> rso.account("good-code", CALLBACK), Status.Code.UNAVAILABLE);
    }

    @Test
    void aMockThatIsntRunningIsUnavailable() {
        MockRsoClient nobody = new MockRsoClient(RestClient.create("http://localhost:1"), "http://localhost:8090");

        assertStatus(() -> nobody.account("good-code", CALLBACK), Status.Code.UNAVAILABLE);
    }

    private static void assertStatus(Runnable call, Status.Code code) {
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(call::run)
                .satisfies(e -> assertThat(e.getStatus().getCode()).isEqualTo(code));
    }
}
