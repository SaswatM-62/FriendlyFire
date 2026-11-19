package com.friendlyfire.account.testsupport;

import java.time.Instant;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * Replaces the clock, Riot Sign-On, Match Data and Scoring with fakes. Import it into a
 * Spring test and autowire the fakes to set them up; reset them before each test.
 */
@TestConfiguration(proxyBeanMethods = false)
public class FakesConfig {

    /** When every test's clock starts. */
    public static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock(NOW);
    }

    @Bean
    @Primary
    FakeRsoClient fakeRso() {
        return new FakeRsoClient();
    }

    @Bean
    @Primary
    FakeMatchDataClient fakeMatchData() {
        return new FakeMatchDataClient();
    }

    @Bean
    @Primary
    FakeScoringClient fakeScoring() {
        return new FakeScoringClient();
    }
}
