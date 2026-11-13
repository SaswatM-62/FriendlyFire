package com.friendlyfire.gateway.realtime;

import static com.friendlyfire.gateway.testsupport.StompTestClient.next;
import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.StompTestClient;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.server.LocalServerPort;

/** A real STOMP client against the running Gateway. */
class PingControllerTest extends GatewayTestBase {

    @LocalServerPort
    private int port;

    @Test
    void echoesTheTimestampWithTheServersTime() throws Exception {
        long before = System.currentTimeMillis();
        try (StompTestClient client = StompTestClient.connect(port, null)) {
            BlockingQueue<Map<String, Object>> pongs = client.subscribe("/user/queue/pong");
            client.send("/app/ping", Map.of("t", 1_730_000_000_123L));

            Map<String, Object> pong = next(pongs);
            assertThat(((Number) pong.get("t")).longValue()).isEqualTo(1_730_000_000_123L);
            assertThat(((Number) pong.get("serverTime")).longValue()).isBetween(before, System.currentTimeMillis());
        }
    }
}
