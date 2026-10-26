package com.friendlyfire.room;

import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureTestGrpcTransport
class RoomServiceApplicationTest {

    @Test
    void contextLoads() {
        // Starts even though quiz-engine, scoring and match-data aren't running.
    }
}
