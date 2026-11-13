package com.friendlyfire.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.StompTestClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

/** APP_ENV=prod: admin-only internals (FF-602), the invite list (FF-618), signed-in sockets (FF-104). */
@TestPropertySource(properties = {"friendlyfire.app-env=prod", "friendlyfire.invited-emails=Sam@example.com, kai@example.com"})
class ProductionSecurityTest extends GatewayTestBase {

    @Autowired
    private MockMvc mvc;

    @LocalServerPort
    private int port;

    @Test
    void healthStaysPublic() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk());
    }

    @Test
    void deepHealthNeedsSignIn() throws Exception {
        mvc.perform(get("/api/health/deep")).andExpect(status().isUnauthorized());
    }

    @Test
    void deepHealthIsForbiddenToPlayers() throws Exception {
        mvc.perform(get("/api/health/deep").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("sam"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void deepHealthIsForAdmins() throws Exception {
        mvc.perform(get("/api/health/deep").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.admin("pm"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("gateway-service"));
    }

    @Test
    void apiDocsAreForAdminsOnly() throws Exception {
        mvc.perform(get("/api/docs/openapi.json").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("sam"))))
                .andExpect(status().isForbidden());
    }

    @Test
    void invitedTestersGetIn() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("sam"))))
                .andExpect(status().isOk());
    }

    @Test
    void everyoneElseSeesTheInviteOnlyPage() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.token("maya"))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("INVITE_ONLY"));
    }

    @Test
    void adminsAlwaysGetIn() throws Exception {
        mvc.perform(get("/api/me").header(HttpHeaders.AUTHORIZATION, FakeTokens.bearer(FakeTokens.admin("pm"))))
                .andExpect(status().isOk());
    }

    @Test
    void everyWebSocketNeedsSignIn() throws Exception {
        assertThat(StompTestClient.refusal(port, null)).startsWith("UNAUTHENTICATED");
    }

    @Test
    void uninvitedPlayersCantConnectEither() throws Exception {
        assertThat(StompTestClient.refusal(port, FakeTokens.token("maya"))).startsWith("INVITE_ONLY");
    }
}
