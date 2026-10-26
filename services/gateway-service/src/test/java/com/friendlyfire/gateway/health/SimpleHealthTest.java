package com.friendlyfire.gateway.health;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.web.client.RestClient;

/** A real HTTP server, because GET /api/health is a forward to Actuator. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SimpleHealthTest {

    @LocalServerPort
    private int port;

    @Test
    void onlySaysTheGatewayIsUp() {
        String body = RestClient.create("http://localhost:" + port).get().uri("/api/health").retrieve().body(String.class);

        assertThat(body).isEqualTo("{\"status\":\"UP\"}");
    }
}
