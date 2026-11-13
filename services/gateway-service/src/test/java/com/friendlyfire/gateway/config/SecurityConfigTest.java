package com.friendlyfire.gateway.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.protection.RequestSizeFilter;
import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Outside production (APP_ENV=local, the default). ProductionSecurityTest covers prod. */
class SecurityConfigTest extends GatewayTestBase {

    private static final String FRONTEND = "http://localhost:3000";

    @Autowired
    private MockMvc mvc;

    @Test
    void healthChecksAreOpenOutsideProduction() throws Exception {
        mvc.perform(get("/api/health/deep")).andExpect(status().isOk());
    }

    @Test
    void everythingElseNeedsSignIn() throws Exception {
        mvc.perform(get("/api/no-such-thing"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/actuator/health")).andExpect(status().isUnauthorized());
    }

    @Test
    void theFrontendMayCallFromItsOrigin() throws Exception {
        mvc.perform(options("/api/rooms")
                        .header(HttpHeaders.ORIGIN, FRONTEND)
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS, "authorization,content-type"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN, FRONTEND));
    }

    @Test
    void otherOriginsMayNot() throws Exception {
        mvc.perform(options("/api/rooms")
                        .header(HttpHeaders.ORIGIN, "https://evil.example")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST"))
                .andExpect(status().isForbidden())
                .andExpect(header().doesNotExist(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN));
    }

    @Test
    void securityHeadersArePresent() throws Exception {
        mvc.perform(get("/api/me").secure(true).header(HttpHeaders.AUTHORIZATION, bearer("maya")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Security-Policy", SecurityConfig.API_CSP))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("X-Frame-Options", "DENY"))
                .andExpect(header().string("Referrer-Policy", "no-referrer"))
                .andExpect(header().string("Strict-Transport-Security", "max-age=31536000 ; includeSubDomains"));
    }

    @Test
    void oversizedBodiesAre413() throws Exception {
        mvc.perform(post("/api/rooms")
                        .header(HttpHeaders.AUTHORIZATION, bearer("maya"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(new byte[RequestSizeFilter.MAX_BYTES + 1]))
                .andExpect(status().isContentTooLarge());
    }

    private static String bearer(String name) {
        return FakeTokens.bearer(FakeTokens.token(name));
    }
}
