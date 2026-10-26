package com.friendlyfire.account;

import org.junit.jupiter.api.Test;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@AutoConfigureTestGrpcTransport
class AccountServiceApplicationTest {

    @Test
    void contextLoads() {
        // Starts even though Postgres, the mock RSO and match-data-service aren't running.
    }
}
