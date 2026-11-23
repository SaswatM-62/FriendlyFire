package com.friendlyfire.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class AudienceValidatorTest {

    private final AudienceValidator validator = new AudienceValidator("https://api.friendlyfire.dev");

    @Test
    void acceptsATokenForTheGateway() {
        assertThat(validator.validate(token(List.of("https://api.friendlyfire.dev", "https://ff.auth0.com/userinfo")))
                .hasErrors()).isFalse();
    }

    @Test
    void refusesATokenForAnotherAudience() {
        assertThat(validator.validate(token(List.of("https://someone-else.dev"))).hasErrors()).isTrue();
    }

    @Test
    void refusesATokenWithoutAnAudience() {
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("auth0|maya").issuedAt(Instant.now()).build();

        assertThat(validator.validate(jwt).hasErrors()).isTrue();
    }

    private static Jwt token(List<String> audience) {
        return Jwt.withTokenValue("t").header("alg", "RS256").subject("auth0|maya").audience(audience)
                .issuedAt(Instant.now()).build();
    }
}
