package com.friendlyfire.gateway.testsupport;

import static org.assertj.core.api.Assertions.fail;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.messaging.converter.CompositeMessageConverter;
import org.springframework.messaging.converter.JacksonJsonMessageConverter;
import org.springframework.messaging.converter.StringMessageConverter;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

/** A browser's STOMP connection to the Gateway, for tests. Messages arrive as JSON maps. */
public final class StompTestClient implements AutoCloseable {

    private static final long WAIT_SECONDS = 5;
    private static final AtomicLong PINGS = new AtomicLong();

    private final WebSocketStompClient client;
    private final StompSession session;
    private final BlockingQueue<String> errorFrames;
    private final BlockingQueue<Map<String, Object>> pongs;

    private StompTestClient(WebSocketStompClient client, StompSession session, BlockingQueue<String> errorFrames) {
        this.client = client;
        this.session = session;
        this.errorFrames = errorFrames;
        this.pongs = subscribe("/user/queue/pong");
    }

    /** Connects with this token (null: none) and fails the test if the Gateway refuses. */
    public static StompTestClient connect(int port, String token) throws Exception {
        WebSocketStompClient client = newClient();
        BlockingQueue<String> errorFrames = new LinkedBlockingQueue<>();
        StompSession session = client.connectAsync(url(port), new WebSocketHttpHeaders(), connectHeaders(token),
                new StompSessionHandlerAdapter() {
                    @Override
                    public Type getPayloadType(StompHeaders headers) {
                        return String.class;
                    }

                    @Override
                    public void handleFrame(StompHeaders headers, Object payload) {
                        errorFrames.add(String.valueOf(headers.getFirst("message")));
                    }
                }).get(WAIT_SECONDS, TimeUnit.SECONDS);
        return new StompTestClient(client, session, errorFrames);
    }

    /** The message of the next STOMP ERROR frame the Gateway sends, or fails the test. */
    public String nextErrorFrame() throws InterruptedException {
        String message = errorFrames.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        if (message == null) {
            fail("No ERROR frame within " + WAIT_SECONDS + " seconds");
        }
        return message;
    }

    public boolean isConnected() {
        return session.isConnected();
    }

    /** Connects expecting the Gateway to refuse; returns the ERROR frame's message header. */
    public static String refusal(int port, String token) throws Exception {
        WebSocketStompClient client = newClient();
        CompletableFuture<String> message = new CompletableFuture<>();
        client.connectAsync(url(port), new WebSocketHttpHeaders(), connectHeaders(token), new StompSessionHandlerAdapter() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return String.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                message.complete(String.valueOf(headers.getFirst("message")));
            }

            @Override
            public void afterConnected(StompSession session, StompHeaders headers) {
                message.completeExceptionally(new AssertionError("The Gateway accepted the connection"));
            }
        });
        try {
            return message.get(WAIT_SECONDS, TimeUnit.SECONDS);
        } finally {
            client.stop();
        }
    }

    public BlockingQueue<Map<String, Object>> subscribe(String destination) {
        BlockingQueue<Map<String, Object>> received = new LinkedBlockingQueue<>();
        session.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return Map.class;
            }

            @Override
            @SuppressWarnings("unchecked")
            public void handleFrame(StompHeaders headers, Object payload) {
                received.add((Map<String, Object>) payload);
            }
        });
        return received;
    }

    public void send(String destination, Object body) {
        session.send(destination, body);
    }

    /**
     * Waits until the Gateway has handled every frame sent so far: it handles a session's
     * frames in order, so once a ping sent now is answered, everything before it is done.
     */
    public void sync() throws InterruptedException {
        long t = PINGS.incrementAndGet();
        session.send("/app/ping", Map.of("t", t));
        while (true) {
            Map<String, Object> pong = next(pongs);
            if (((Number) pong.get("t")).longValue() == t) {
                return;
            }
        }
    }

    /** The next message, or fails the test after a few seconds. */
    public static Map<String, Object> next(BlockingQueue<Map<String, Object>> queue) throws InterruptedException {
        Map<String, Object> message = queue.poll(WAIT_SECONDS, TimeUnit.SECONDS);
        if (message == null) {
            fail("Nothing arrived within " + WAIT_SECONDS + " seconds");
        }
        return message;
    }

    @Override
    public void close() {
        if (session.isConnected()) {
            session.disconnect();
        }
        client.stop();
    }

    private static WebSocketStompClient newClient() {
        WebSocketStompClient client = new WebSocketStompClient(new StandardWebSocketClient());
        client.setMessageConverter(
                new CompositeMessageConverter(List.of(new StringMessageConverter(), new JacksonJsonMessageConverter())));
        return client;
    }

    private static String url(int port) {
        return "ws://localhost:" + port + "/ws";
    }

    private static StompHeaders connectHeaders(String token) {
        StompHeaders headers = new StompHeaders();
        if (token != null) {
            headers.add("Authorization", FakeTokens.bearer(token));
        }
        return headers;
    }
}
