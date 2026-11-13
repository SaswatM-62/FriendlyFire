package com.friendlyfire.gateway.web;

import static com.friendlyfire.gateway.testsupport.FakeServices.ACCOUNT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.testsupport.FakeAccountService;
import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.StompTestClient;
import com.friendlyfire.gateway.testsupport.TestNames;
import com.friendlyfire.proto.friendlyfire.account.v1.DeleteAccountRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.UpdateDisplayNameRequest;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

class MeControllerTest extends GatewayTestBase {

    @Autowired
    private MockMvc mvc;

    @LocalServerPort
    private int port;

    @Test
    void withoutATokenIs401() throws Exception {
        mvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void aTokenAuth0WouldRefuseIs401() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.INVALID)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
    }

    @Test
    void returnsTheUserCreatedFromTheToken() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").value("user-maya"))
                .andExpect(jsonPath("$.displayName").value("Maya"))
                .andExpect(jsonPath("$.email").value("maya@example.com"))
                .andExpect(jsonPath("$.createdAt").value("2025-11-01T12:26:40Z"))
                .andExpect(jsonPath("$.riotLink").doesNotExist());

        EnsureUserRequest request = ACCOUNT.calls.last(EnsureUserRequest.class);
        assertThat(request.getAuthSubject()).isEqualTo("auth0|maya");
        assertThat(request.getEmail()).isEqualTo("maya@example.com");
        assertThat(request.getSuggestedDisplayName()).isEqualTo("Maya");
    }

    @Test
    void asksAccountOnEveryLoadSoStaleMatchDataGetsSynced() throws Exception {
        for (int i = 0; i < 2; i++) {
            mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("noor"))))
                    .andExpect(status().isOk());
        }

        assertThat(ACCOUNT.calls.received(EnsureUserRequest.class)).hasSize(2);
    }

    @Test
    void showsTheRiotLinkButNeverThePuuid() throws Exception {
        ACCOUNT.calls.answer(EnsureUserRequest.class, request -> EnsureUserResponse.newBuilder()
                .setUser(FakeAccountService.user("maya", "Maya").toBuilder().setRiotLink(FakeAccountService.riotLink()))
                .build());

        String body = mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.riotLink.gameName").value("Maya"))
                .andExpect(jsonPath("$.riotLink.tagLine").value("NA1"))
                .andExpect(jsonPath("$.riotLink.region").value("na"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body.toLowerCase()).doesNotContain("puuid");
    }

    @Test
    void unverifiedEmailIs403WithItsOwnCode() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.unverified("pat"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("EMAIL_NOT_VERIFIED"));

        assertThat(ACCOUNT.calls.received(EnsureUserRequest.class)).isEmpty();
    }

    @Test
    void accountServiceDownIs503() throws Exception {
        ACCOUNT.calls.fail(EnsureUserRequest.class, Status.UNAVAILABLE);

        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya"))))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void changesTheDisplayName() throws Exception {
        mvc.perform(patch("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"Maya B\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.displayName").value("Maya B"));

        UpdateDisplayNameRequest request = ACCOUNT.calls.last(UpdateDisplayNameRequest.class);
        assertThat(request.getUserId()).isEqualTo("user-maya");
        assertThat(request.getDisplayName()).isEqualTo("Maya B");
    }

    @Test
    void invalidDisplayNameIs400WithAccountsMessage() throws Exception {
        ACCOUNT.calls.fail(UpdateDisplayNameRequest.class,
                Status.INVALID_ARGUMENT.withDescription("Your name needs 1 to 24 characters."));

        mvc.perform(patch("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"displayName\": \"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error.code").value("INVALID_ARGUMENT"))
                .andExpect(jsonPath("$.error.message").value("Your name needs 1 to 24 characters."));
    }

    @Test
    void deletesTheAccount() throws Exception {
        mvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya"))))
                .andExpect(status().isNoContent());

        assertThat(ACCOUNT.calls.last(DeleteAccountRequest.class).getUserId()).isEqualTo("user-maya");
    }

    @Test
    void deletingTheAccountClosesItsLiveConnection() throws Exception {
        String player = TestNames.player();
        try (StompTestClient otherDevice = StompTestClient.connect(port, FakeTokens.token(player))) {
            otherDevice.sync();

            mvc.perform(delete("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token(player))))
                    .andExpect(status().isNoContent());

            assertThat(otherDevice.nextErrorFrame()).startsWith("ACCOUNT_DELETED");
        }
    }
}
