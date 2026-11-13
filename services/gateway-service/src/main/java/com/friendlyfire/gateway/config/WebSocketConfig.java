package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.realtime.ConnectInterceptor;
import java.net.InetSocketAddress;
import java.util.Map;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketTransportRegistration;
import org.springframework.web.socket.server.HandshakeInterceptor;

/**
 * STOMP over WebSocket at /ws. Browsers send to /app/..., subscribe to /topic/...
 * (everyone in a room) and /user/queue/... (only them). ConnectInterceptor checks every
 * frame; heartbeats every 10 seconds both ways close dead connections.
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    /** Server and browser heartbeats, in ms (the frontend uses the same). */
    static final long[] HEARTBEAT_MS = {10_000, 10_000};
    /** Every message a browser sends is a few small JSON fields (FF-601). */
    static final int MESSAGE_SIZE_LIMIT = 16 * 1024;

    private final String frontendOrigin;
    private final ConnectInterceptor connectInterceptor;
    private final TaskScheduler heartbeats;

    public WebSocketConfig(
            @Value("${friendlyfire.frontend-origin}") String frontendOrigin,
            ConnectInterceptor connectInterceptor,
            // Spring's own broker scheduler, created by this same configuration: hence @Lazy.
            @Lazy @Qualifier("messageBrokerTaskScheduler") TaskScheduler heartbeats) {
        this.frontendOrigin = frontendOrigin;
        this.connectInterceptor = connectInterceptor;
        this.heartbeats = heartbeats;
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Never "*": only the frontend may open a socket.
        registry.addEndpoint("/ws").setAllowedOrigins(frontendOrigin).addInterceptors(new RemoteAddress());
        // Handle each player's messages in the order they sent them.
        registry.setPreserveReceiveOrder(true);
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.setApplicationDestinationPrefixes("/app");
        registry.setUserDestinationPrefix("/user");
        registry.enableSimpleBroker("/topic", "/queue").setHeartbeatValue(HEARTBEAT_MS).setTaskScheduler(heartbeats);
    }

    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(connectInterceptor);
    }

    @Override
    public void configureWebSocketTransport(WebSocketTransportRegistration registration) {
        registration.setMessageSizeLimit(MESSAGE_SIZE_LIMIT);
    }

    /** Keeps the browser's IP address on the session, for per-IP rate limits. */
    private static final class RemoteAddress implements HandshakeInterceptor {

        @Override
        public boolean beforeHandshake(
                ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler,
                Map<String, Object> attributes) {
            InetSocketAddress address = request.getRemoteAddress();
            if (address != null && address.getAddress() != null) {
                attributes.put(ConnectInterceptor.REMOTE_IP_ATTRIBUTE, address.getAddress().getHostAddress());
            }
            return true;
        }

        @Override
        public void afterHandshake(
                ServerHttpRequest request, ServerHttpResponse response, WebSocketHandler handler, Exception exception) {
        }
    }
}
