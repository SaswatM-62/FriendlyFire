package com.friendlyfire.gateway.health;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.grpc.Status;
import java.net.ConnectException;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DeepHealthController.class)
class DeepHealthControllerTest {

    @Autowired
    private MockMvc mvc;

    @TestConfiguration
    static class FakeDownstream {

        @Bean
        DependencyHealthChecker dependencyHealthChecker() {
            return new DependencyHealthChecker("gateway-service", List.of(
                    FakeDependencyCheck.up("account-service"),
                    FakeDependencyCheck.failing("room-service", Status.UNAVAILABLE
                            .withCause(new ConnectException("Connection refused"))
                            .asRuntimeException())),
                    Duration.ofMillis(1500));
        }
    }

    @Test
    void returnsTheTreeWithHttp200EvenWhenADependencyIsDown() throws Exception {
        mvc.perform(get("/api/health/deep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("gateway-service"))
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.latencyMs").value(0))
                .andExpect(jsonPath("$.dependencies[0].name").value("account-service"))
                .andExpect(jsonPath("$.dependencies[0].status").value("UP"))
                .andExpect(jsonPath("$.dependencies[0].error").doesNotExist())
                .andExpect(jsonPath("$.dependencies[0].dependencies").isEmpty())
                .andExpect(jsonPath("$.dependencies[1].name").value("room-service"))
                .andExpect(jsonPath("$.dependencies[1].status").value("DOWN"))
                .andExpect(jsonPath("$.dependencies[1].error").value("connection refused"));
    }

    @Test
    void onlyTheFrontendOriginMayCallIt() throws Exception {
        mvc.perform(get("/api/health/deep").header("Origin", "http://localhost:3000"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"));
        mvc.perform(get("/api/health/deep").header("Origin", "https://evil.example"))
                .andExpect(status().isForbidden());
    }
}
