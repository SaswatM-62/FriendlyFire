package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.realtime.ConnectInterceptor;
import com.friendlyfire.gateway.realtime.RealtimeErrors;
import com.friendlyfire.gateway.realtime.RoomEventRelay;
import com.friendlyfire.gateway.realtime.SessionCloser;
import com.friendlyfire.gateway.realtime.SessionEventsListener;
import com.friendlyfire.gateway.realtime.SessionRegistry;
import com.friendlyfire.gateway.security.AuthSettings;
import com.friendlyfire.gateway.security.SignInRules;
import com.friendlyfire.gateway.security.UserResolver;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceStub;
import java.time.Clock;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/** The WebSocket side's parts (WebSocketConfig registers the interceptor and endpoint). */
@Configuration(proxyBeanMethods = false)
public class RealtimeConfig {

    /** Waits before reopening a dropped room event stream, attempt after attempt. */
    static final List<Duration> RELAY_BACKOFF =
            List.of(Duration.ofSeconds(1), Duration.ofSeconds(2), Duration.ofSeconds(5), Duration.ofSeconds(10));

    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistry();
    }

    @Bean
    RealtimeErrors realtimeErrors(ObjectProvider<SimpMessagingTemplate> messaging) {
        return new RealtimeErrors(messaging);
    }

    @Bean
    RoomEventRelay roomEventRelay(
            @Qualifier("roomEvents") RoomServiceStub roomEvents, ObjectProvider<SimpMessagingTemplate> messaging,
            Clock clock) {
        return new RoomEventRelay(roomEvents, messaging, Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "room-event-relay");
            thread.setDaemon(true);
            return thread;
        }), clock, RELAY_BACKOFF);
    }

    @Bean
    SessionCloser sessionCloser(
            @Qualifier("clientOutboundChannel") ObjectProvider<MessageChannel> clientOutbound, SessionRegistry sessions) {
        return new SessionCloser(clientOutbound, sessions);
    }

    @Bean
    ConnectInterceptor connectInterceptor(
            AuthSettings settings, JwtDecoder jwtDecoder, SignInRules rules, UserResolver users,
            RoomServiceBlockingStub rooms, SessionRegistry sessions, RoomEventRelay relay, RealtimeErrors errors,
            SessionCloser closer) {
        return new ConnectInterceptor(settings, jwtDecoder, rules, users, rooms, sessions, relay, errors, closer);
    }

    @Bean
    SessionEventsListener sessionEventsListener(
            RoomServiceBlockingStub rooms, SessionRegistry sessions, RoomEventRelay relay) {
        return new SessionEventsListener(rooms, sessions, relay);
    }
}
