package com.friendlyfire.gateway.health;

import static org.hamcrest.Matchers.contains;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesRequest;
import com.friendlyfire.proto.friendlyfire.health.v1.CheckDependenciesResponse;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import com.friendlyfire.proto.friendlyfire.health.v1.DependencyHealthServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The real stubs, HealthConfig and controller, with each service's channel pointed at a
 * fake gRPC service running in-process. scoring-service has no fake, so it's unreachable.
 */
@SpringBootTest
@AutoConfigureMockMvc
class DeepHealthGrpcTest {

    private static List<Server> fakes;

    @TestBean
    private ManagedChannel accountChannel;

    @TestBean
    private ManagedChannel roomChannel;

    @TestBean
    private ManagedChannel scoringChannel;

    @TestBean
    private ManagedChannel matchDataChannel;

    @Autowired
    private MockMvc mvc;

    static ManagedChannel accountChannel() {
        return InProcessChannelBuilder.forName("fake-account").build();
    }

    static ManagedChannel roomChannel() {
        return InProcessChannelBuilder.forName("fake-room").build();
    }

    static ManagedChannel scoringChannel() {
        return InProcessChannelBuilder.forName("fake-scoring").build();
    }

    static ManagedChannel matchDataChannel() {
        return InProcessChannelBuilder.forName("fake-matchdata").build();
    }

    @BeforeAll
    static void startFakeServices() throws IOException {
        fakes = List.of(
                fake("fake-account", node("account-service", node("postgres"))),
                fake("fake-room", node("room-service")),
                fake("fake-matchdata", node("match-data-service", node("postgres"))));
    }

    @AfterAll
    static void stopFakeServices() {
        fakes.forEach(Server::shutdownNow);
    }

    @Test
    void showsEveryServiceAndStaysHttp200WhenOneIsDown() throws Exception {
        mvc.perform(get("/api/health/deep"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DEGRADED"))
                .andExpect(jsonPath("$.dependencies[*].name")
                        .value(contains("account-service", "room-service", "scoring-service", "match-data-service")))
                .andExpect(jsonPath("$.dependencies[*].status").value(contains("UP", "UP", "DOWN", "UP")))
                .andExpect(jsonPath("$.dependencies[0].dependencies[0].name").value("postgres"))
                .andExpect(jsonPath("$.dependencies[2].error").exists());
    }

    private static ComponentHealth node(String name, ComponentHealth... dependencies) {
        return ComponentHealth.newBuilder()
                .setName(name)
                .setStatus(ComponentStatus.COMPONENT_STATUS_UP)
                .addAllDependencies(List.of(dependencies))
                .build();
    }

    private static Server fake(String serverName, ComponentHealth tree) throws IOException {
        var service = new DependencyHealthServiceGrpc.DependencyHealthServiceImplBase() {
            @Override
            public void checkDependencies(
                    CheckDependenciesRequest request, StreamObserver<CheckDependenciesResponse> responseObserver) {
                responseObserver.onNext(CheckDependenciesResponse.newBuilder().setComponent(tree).build());
                responseObserver.onCompleted();
            }
        };
        return InProcessServerBuilder.forName(serverName).addService(service).directExecutor().build().start();
    }
}
