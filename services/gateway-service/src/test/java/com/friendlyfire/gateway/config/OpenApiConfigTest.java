package com.friendlyfire.gateway.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

/** /api/docs lists every REST endpoint with its bodies (FF-005). */
class OpenApiConfigTest extends GatewayTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void listsEveryEndpointFromTheGatewayControllers() throws Exception {
        String docs = mvc.perform(get("/api/docs/openapi.json"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();

        assertThat(docs).contains(
                "\"/api/me\"",
                "\"/api/link/riot/start\"",
                "\"/api/link/riot/complete\"",
                "\"/api/link/riot\"",
                "\"/api/link/riot/status\"",
                "\"/api/rooms\"",
                "\"/api/rooms/{code}/join\"",
                "\"/api/rooms/{code}\"",
                "\"/api/history\"",
                "\"/api/quizzes/{quizId}\"",
                "\"/api/health/deep\"",
                "\"RoomSnapshotJson\"",
                "\"MeJson\"");
    }

    @Test
    void theSignedInUserIsNotAParameter() throws Exception {
        String docs = mvc.perform(get("/api/docs/openapi.json")).andReturn().getResponse().getContentAsString();

        assertThat(docs).doesNotContain("SignedInUser", "\"name\":\"me\"");
    }

    @Test
    void theDocsPageIsServed() throws Exception {
        mvc.perform(get("/api/docs")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/api/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
