package com.friendlyfire.room.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserResponse;
import com.friendlyfire.proto.friendlyfire.account.v1.Look;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class GrpcFavouriteLookClientTest {

    private Server server;
    private ManagedChannel channel;

    /** An Account that knows "sam" (with a look) and "maya" (without one). */
    private GrpcFavouriteLookClient client() throws IOException {
        String name = InProcessServerBuilder.generateName();
        server = InProcessServerBuilder.forName(name).directExecutor().addService(new AccountServiceGrpc.AccountServiceImplBase() {
            @Override
            public void getUser(GetUserRequest request, StreamObserver<GetUserResponse> responses) {
                switch (request.getUserId()) {
                    case "sam" -> reply(responses, User.newBuilder().setUserId("sam")
                            .setLook(Look.newBuilder().setIcon("ghost").setColour("cyan").setExpression("wink")).build());
                    case "maya" -> reply(responses, User.newBuilder().setUserId("maya").build());
                    default -> responses.onError(Status.NOT_FOUND.asRuntimeException());
                }
            }
        }).build().start();
        channel = InProcessChannelBuilder.forName(name).directExecutor().build();
        return new GrpcFavouriteLookClient(AccountServiceGrpc.newBlockingStub(channel));
    }

    private static void reply(StreamObserver<GetUserResponse> responses, User user) {
        responses.onNext(GetUserResponse.newBuilder().setUser(user).build());
        responses.onCompleted();
    }

    @AfterEach
    void stop() {
        channel.shutdownNow();
        server.shutdownNow();
    }

    @Test
    void aSavedLookComesBackAsTheFavourite() throws IOException {
        assertThat(client().favourite("sam")).contains(new FavouriteLook("ghost", "cyan", "wink"));
    }

    @Test
    void aUserWhoHasntPickedOneHasNoFavourite() throws IOException {
        assertThat(client().favourite("maya")).isEmpty();
    }

    @Test
    void anAccountErrorIsThrown() throws IOException {
        GrpcFavouriteLookClient client = client();

        assertThatExceptionOfType(StatusRuntimeException.class).isThrownBy(() -> client.favourite("nobody"));
    }
}
