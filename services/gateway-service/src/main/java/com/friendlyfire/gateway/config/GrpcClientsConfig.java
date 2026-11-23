package com.friendlyfire.gateway.config;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc.DependencyHealthServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceStub;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * gRPC clients for the services the Gateway calls. One channel per service (address
 * from spring.grpc.client.channel.*.target, i.e. the *_GRPC_ADDR env vars); the
 * feature stub and the health stub share it.
 *
 * <p>Every call gets a 3-second deadline unless it sets its own, so a slow service can't
 * hang a request (FF-006). A call that needs longer sets it on the call, e.g.
 * {@code rooms.withDeadlineAfter(15, TimeUnit.SECONDS).startQuiz(...)}. The one exception
 * is the room event stream (roomEvents), which stays open as long as the room does.
 *
 * <p>Match Data isn't here: Account owns players' sync status (ADR-010), so the Gateway
 * asks Account.
 */
@Configuration(proxyBeanMethods = false)
public class GrpcClientsConfig {

    static final Duration DEFAULT_DEADLINE = Duration.ofSeconds(3);

    private final ClientInterceptor defaultDeadline = new DefaultDeadlineInterceptor(DEFAULT_DEADLINE);

    @Bean
    ManagedChannel accountChannel(GrpcChannelFactory channels) {
        return channels.createChannel("account");
    }

    @Bean
    AccountServiceBlockingStub accountService(@Qualifier("accountChannel") ManagedChannel channel) {
        return AccountServiceGrpc.newBlockingStub(channel).withInterceptors(defaultDeadline);
    }

    @Bean
    DependencyHealthServiceBlockingStub accountHealth(@Qualifier("accountChannel") ManagedChannel channel) {
        return DependencyHealthServiceGrpc.newBlockingStub(channel).withInterceptors(defaultDeadline);
    }

    @Bean
    ManagedChannel roomChannel(GrpcChannelFactory channels) {
        return channels.createChannel("room");
    }

    @Bean
    RoomServiceBlockingStub roomService(@Qualifier("roomChannel") ManagedChannel channel) {
        return RoomServiceGrpc.newBlockingStub(channel).withInterceptors(defaultDeadline);
    }

    /** For StreamRoomEvents (RoomEventRelay): async, and no default deadline. */
    @Bean
    RoomServiceStub roomEvents(@Qualifier("roomChannel") ManagedChannel channel) {
        return RoomServiceGrpc.newStub(channel);
    }

    @Bean
    DependencyHealthServiceBlockingStub roomHealth(@Qualifier("roomChannel") ManagedChannel channel) {
        return DependencyHealthServiceGrpc.newBlockingStub(channel).withInterceptors(defaultDeadline);
    }

    @Bean
    ManagedChannel scoringChannel(GrpcChannelFactory channels) {
        return channels.createChannel("scoring");
    }

    @Bean
    ScoringServiceBlockingStub scoringService(@Qualifier("scoringChannel") ManagedChannel channel) {
        return ScoringServiceGrpc.newBlockingStub(channel).withInterceptors(defaultDeadline);
    }

    @Bean
    DependencyHealthServiceBlockingStub scoringHealth(@Qualifier("scoringChannel") ManagedChannel channel) {
        return DependencyHealthServiceGrpc.newBlockingStub(channel).withInterceptors(defaultDeadline);
    }
}
