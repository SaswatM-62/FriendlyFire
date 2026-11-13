package com.friendlyfire.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotResponse;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import io.grpc.ManagedChannel;
import io.grpc.Server;
import io.grpc.Status;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.stub.StreamObserver;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * The real advice, stubs and deadline, behind test-only endpoints that fail on purpose.
 * room-service's channel points at an in-process fake that never answers.
 */
@SpringBootTest
@AutoConfigureMockMvc
class GlobalExceptionHandlerTest {

    private static final String NEVER_ANSWERS = "room-never-answers";

    private static Server roomThatNeverAnswers;

    @TestBean
    private ManagedChannel roomChannel;

    @Autowired
    private MockMvc mvc;

    static ManagedChannel roomChannel() {
        return InProcessChannelBuilder.forName(NEVER_ANSWERS).build();
    }

    @BeforeAll
    static void startRoomThatNeverAnswers() throws IOException {
        var service = new RoomServiceGrpc.RoomServiceImplBase() {
            @Override
            public void getRoomSnapshot(
                    GetRoomSnapshotRequest request, StreamObserver<GetRoomSnapshotResponse> responseObserver) {
                // Never answers.
            }
        };
        roomThatNeverAnswers = InProcessServerBuilder.forName(NEVER_ANSWERS).addService(service).build().start();
    }

    @AfterAll
    static void stopRoomThatNeverAnswers() {
        roomThatNeverAnswers.shutdownNow();
    }

    @Test
    void clientErrorFromAServiceKeepsItsMessage() throws Exception {
        mvc.perform(get("/test/grpc/FAILED_PRECONDITION"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error.code").value("FAILED_PRECONDITION"))
                .andExpect(jsonPath("$.error.message").value("Maya is still syncing."));
    }

    @Test
    void unavailableServiceIs503WithoutItsDetails() throws Exception {
        mvc.perform(get("/test/grpc/UNAVAILABLE"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("UNAVAILABLE"))
                .andExpect(jsonPath("$.error.message").value(ApiErrors.UNAVAILABLE_MESSAGE));
    }

    @Test
    void serviceThatNeverAnswersIs503WithinAboutThreeSeconds() throws Exception {
        long start = System.nanoTime();

        mvc.perform(get("/test/room"))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.error.code").value("DEADLINE_EXCEEDED"));

        assertThat(Duration.ofNanos(System.nanoTime() - start)).isLessThan(Duration.ofSeconds(5));
    }

    @Test
    void unexpectedErrorIs500WithoutTheException() throws Exception {
        String body = mvc.perform(get("/test/boom"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error.code").value("INTERNAL"))
                .andReturn().getResponse().getContentAsString();

        assertThat(body).doesNotContain("secret internals", "IllegalStateException");
    }

    @Test
    void unknownPathIs404InTheSameFormat() throws Exception {
        mvc.perform(get("/api/no-such-thing"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("NOT_FOUND"));
    }

    @Test
    void unfinishedEndpointIs501NotImplemented() throws Exception {
        mvc.perform(get("/api/rooms/K7QM2X"))
                .andExpect(status().isNotImplemented())
                .andExpect(jsonPath("$.error.code").value("NOT_IMPLEMENTED"));
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FailingEndpoints {

        @Bean
        FailingController failingController(RoomServiceBlockingStub rooms) {
            return new FailingController(rooms);
        }
    }

    @RestController
    static class FailingController {

        private final RoomServiceBlockingStub rooms;

        FailingController(RoomServiceBlockingStub rooms) {
            this.rooms = rooms;
        }

        @GetMapping("/test/grpc/{code}")
        void grpc(@PathVariable Status.Code code) {
            throw Status.fromCode(code).withDescription("Maya is still syncing.").asRuntimeException();
        }

        @GetMapping("/test/room")
        void room() {
            rooms.getRoomSnapshot(GetRoomSnapshotRequest.getDefaultInstance());
        }

        @GetMapping("/test/boom")
        void boom() {
            throw new IllegalStateException("secret internals");
        }
    }
}
