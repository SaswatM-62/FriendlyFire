package com.friendlyfire.account.config;

import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc.DependencyHealthServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceGrpc;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceGrpc.MatchDataServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import io.grpc.ManagedChannel;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.grpc.client.GrpcChannelFactory;

/**
 * gRPC clients for the services account-service calls. One channel per service
 * (address from spring.grpc.client.channel.*.target, i.e. the *_GRPC_ADDR env vars);
 * the feature stub and the health stub share it.
 */
@Configuration(proxyBeanMethods = false)
public class GrpcClientsConfig {

    @Bean
    ManagedChannel matchDataChannel(GrpcChannelFactory channels) {
        return channels.createChannel("matchdata");
    }

    @Bean
    MatchDataServiceBlockingStub matchDataService(@Qualifier("matchDataChannel") ManagedChannel channel) {
        return MatchDataServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    DependencyHealthServiceBlockingStub matchDataHealth(@Qualifier("matchDataChannel") ManagedChannel channel) {
        return DependencyHealthServiceGrpc.newBlockingStub(channel);
    }

    // DeleteAccount asks Scoring to show the player as "Deleted player" (ADR-027).
    @Bean
    ManagedChannel scoringChannel(GrpcChannelFactory channels) {
        return channels.createChannel("scoring");
    }

    @Bean
    ScoringServiceBlockingStub scoringService(@Qualifier("scoringChannel") ManagedChannel channel) {
        return ScoringServiceGrpc.newBlockingStub(channel);
    }

    @Bean
    DependencyHealthServiceBlockingStub scoringHealth(@Qualifier("scoringChannel") ManagedChannel channel) {
        return DependencyHealthServiceGrpc.newBlockingStub(channel);
    }
}
