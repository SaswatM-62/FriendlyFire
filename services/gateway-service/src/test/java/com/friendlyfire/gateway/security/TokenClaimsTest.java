package com.friendlyfire.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class TokenClaimsTest {

    private static final String NS = "https://friendlyfire.dev/";
    private final TokenClaims claims = new TokenClaims(NS);

    @Test
    void readsTheNamespacedClaimsAnAuth0ActionAdds() {
        Jwt jwt = jwt(Map.of(NS + "email", " maya@example.com ", NS + "email_verified", true, NS + "name", "Maya"));

        assertThat(claims.email(jwt)).isEqualTo("maya@example.com");
        assertThat(claims.emailVerified(jwt)).isTrue();
        assertThat(claims.name(jwt)).isEqualTo("Maya");
    }

    @Test
    void fallsBackToThePlainOidcClaims() {
        Jwt jwt = jwt(Map.of("email", "maya@example.com", "email_verified", "true", "name", "Maya"));

        assertThat(claims.email(jwt)).isEqualTo("maya@example.com");
        assertThat(claims.emailVerified(jwt)).isTrue();
        assertThat(claims.name(jwt)).isEqualTo("Maya");
    }

    @Test
    void aMissingOrFalseVerifiedClaimIsUnverified() {
        assertThat(claims.emailVerified(jwt(Map.of("x", 1)))).isFalse();
        assertThat(claims.emailVerified(jwt(Map.of(NS + "email_verified", false)))).isFalse();
    }

    @Test
    void missingTextClaimsAreEmpty() {
        assertThat(claims.email(jwt(Map.of("x", 1)))).isEmpty();
        assertThat(claims.name(jwt(Map.of("x", 1)))).isEmpty();
    }

    @Test
    void adminsHaveTheAdminRole() {
        assertThat(claims.isAdmin(jwt(Map.of(NS + "roles", List.of("Admin"))))).isTrue();
        assertThat(claims.isAdmin(jwt(Map.of(NS + "roles", List.of("tester"))))).isFalse();
        assertThat(claims.isAdmin(jwt(Map.of("roles", List.of("admin"))))).isFalse();
    }

    private static Jwt jwt(Map<String, Object> extra) {
        Jwt.Builder jwt = Jwt.withTokenValue("t").header("alg", "RS256").subject("auth0|maya").issuedAt(Instant.now());
        extra.forEach(jwt::claim);
        return jwt.build();
    }
}
