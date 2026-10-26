package com.friendlyfire.room.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.grpc.test.autoconfigure.AutoConfigureTestGrpcTransport;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.convention.TestBean;

/** Shows how tests control time: replace the Clock bean with a fixed clock. */
@SpringBootTest
@AutoConfigureTestGrpcTransport
class ClockConfigTest {

    private static final Instant NOW = Instant.parse("2026-01-02T03:04:05Z");

    @TestBean
    private Clock clock;

    @Autowired
    private Clock injectedClock;

    static Clock clock() {
        return Clock.fixed(NOW, ZoneOffset.UTC);
    }

    @Test
    void everyBeanSeesTheFixedClock() {
        assertThat(injectedClock.instant()).isEqualTo(NOW);
    }

    @Test
    void productionClockIsUtc() {
        assertThat(new ClockConfig().clock().getZone()).isEqualTo(ZoneOffset.UTC);
    }
}
