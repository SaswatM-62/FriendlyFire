package com.friendlyfire.gateway.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.gateway.realtime.StompExceptionHandler.RealtimeError;
import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.gateway.testsupport.TestNames;
import com.friendlyfire.gateway.web.ApiError;
import com.friendlyfire.gateway.web.ApiException;
import io.grpc.Status;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.stereotype.Controller;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** A real STOMP client against the running Gateway, with a test controller that fails. */
class StompExceptionHandlerTest extends GatewayTestBase {

    @LocalServerPort
    private int port;

    private WebSocketStompClient client;
    private StompSession session;

    @BeforeEach
    void connect() throws Exception {
        client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(new JacksonJsonMessageConverter());
        StompHeaders signedIn = new StompHeaders();
        signedIn.add("Authorization", FakeTokens.bearer(FakeTokens.token(TestNames.player())));
        session = client.connectAsync("ws://localhost:" + port + "/ws", new WebSocketHttpHeaders(), signedIn,
                new StompSessionHandlerAdapter() {
                }).get(3, TimeUnit.SECONDS);
    }

    @AfterEach
    void disconnect() {
        session.disconnect();
        client.stop();
    }

    @Test
    void failedGrpcCallIsSentToThePlayerWithTheDestination() throws Exception {
        RealtimeError error = sendAndAwaitError("/app/test/fail");

        assertThat(error).isEqualTo(new RealtimeError(
                new ApiError("FAILED_PRECONDITION", "Maya is still syncing."), "/app/test/fail"));
    }

    @Test
    void anErrorWithOurOwnCodeKeepsIt() throws Exception {
        RealtimeError error = sendAndAwaitError("/app/test/rate-limited");

        assertThat(error.error()).isEqualTo(new ApiError("RATE_LIMITED", "Slow down."));
    }

    @Test
    void aBodyThatIsntValidJsonIsInvalid() throws Exception {
        RealtimeError error = sendAndAwaitError("/app/test/body", "not json".getBytes());

        assertThat(error.error().code()).isEqualTo("INVALID_ARGUMENT");
    }

    @Test
    void unexpectedErrorIsSentWithoutTheException() throws Exception {
        RealtimeError error = sendAndAwaitError("/app/test/boom");

        assertThat(error.destination()).isEqualTo("/app/test/boom");
        assertThat(error.error().code()).isEqualTo("INTERNAL");
        assertThat(error.error().message()).doesNotContain("secret internals");
    }

    private RealtimeError sendAndAwaitError(String destination) throws Exception {
        return sendAndAwaitError(destination, Map.of());
    }

    private RealtimeError sendAndAwaitError(String destination, Object body) throws Exception {
        CompletableFuture<RealtimeError> received = new CompletableFuture<>();
        session.subscribe("/user/queue/errors", new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return RealtimeError.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                received.complete((RealtimeError) payload);
            }
        });
        if (body instanceof byte[] bytes) {
            StompHeaders headers = new StompHeaders();
            headers.setDestination(destination);
            headers.setContentType(MimeTypeUtils.APPLICATION_JSON);
            session.send(headers, bytes);
        } else {
            session.send(destination, body);
        }
        return received.get(3, TimeUnit.SECONDS);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingActions {

        @Bean
        FailingController failingStompController() {
            return new FailingController();
        }
    }

    /** Another controller's actions, to show the advice isn't limited to one controller. */
    @Controller
    static class FailingController {

        @MessageMapping("/test/fail")
        void fail() {
            throw Status.FAILED_PRECONDITION.withDescription("Maya is still syncing.").asRuntimeException();
        }

        @MessageMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("secret internals");
        }

        @MessageMapping("/test/rate-limited")
        void rateLimited() {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", "Slow down.");
        }

        @MessageMapping("/test/body")
        void body(@Payload Map<String, Object> body) {
        }
    }
}
