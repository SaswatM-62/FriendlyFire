package com.friendlyfire.gateway.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.gateway.realtime.PingController.Ping;
import java.lang.reflect.Type;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** A real STOMP client against the running Gateway. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PingControllerTest {

    @LocalServerPort
    private int port;

    @Test
    void echoesTheTimestampBackToTheSender() throws Exception {
        var client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompSession session = client.connectAsync("ws://localhost:" + port + "/ws", new StompSessionHandlerAdapter() {
        }).get(3, TimeUnit.SECONDS);

        CompletableFuture<Ping> pong = new CompletableFuture<>();
        session.subscribe("/user/queue/pong", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Ping.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                pong.complete((Ping) payload);
            }
        });
        session.send("/app/ping", new Ping(1_730_000_000_123L));

        assertThat(pong.get(3, TimeUnit.SECONDS).t()).isEqualTo(1_730_000_000_123L);
        session.disconnect();
        client.stop();
    }
}
