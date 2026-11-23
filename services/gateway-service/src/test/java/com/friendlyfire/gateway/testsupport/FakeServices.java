package com.friendlyfire.gateway.testsupport;

import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * One fake of each service the Gateway calls, running in-process for the whole test run.
 * GatewayTestBase points the Gateway's channels at them and resets them before each test.
 */
public final class FakeServices {

    public static final FakeAccountService ACCOUNT = new FakeAccountService();
    public static final FakeRoomService ROOM = new FakeRoomService();
    public static final FakeScoringService SCORING = new FakeScoringService();

    private static final String ACCOUNT_NAME = "fake-account-service";
    private static final String ROOM_NAME = "fake-room-service";
    private static final String SCORING_NAME = "fake-scoring-service";

    static {
        start(ACCOUNT_NAME, ACCOUNT);
        start(ROOM_NAME, ROOM);
        start(SCORING_NAME, SCORING);
    }

    private FakeServices() {
    }

    public static ManagedChannel accountChannel() {
        return InProcessChannelBuilder.forName(ACCOUNT_NAME).build();
    }

    public static ManagedChannel roomChannel() {
        return InProcessChannelBuilder.forName(ROOM_NAME).build();
    }

    public static ManagedChannel scoringChannel() {
        return InProcessChannelBuilder.forName(SCORING_NAME).build();
    }

    public static void reset() {
        ACCOUNT.calls.reset();
        ROOM.reset();
        SCORING.calls.reset();
    }

    private static void start(String name, io.grpc.BindableService service) {
        try {
            Server server = InProcessServerBuilder.forName(name).addService(service).build().start();
            Runtime.getRuntime().addShutdownHook(new Thread(server::shutdownNow));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
