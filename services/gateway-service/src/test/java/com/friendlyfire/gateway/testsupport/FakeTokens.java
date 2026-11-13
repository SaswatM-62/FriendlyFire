package com.friendlyfire.gateway.testsupport;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;

/**
 * Stands in for Auth0: "tokens" are readable strings that the fake decoder turns into the
 * claims a real Auth0 token for that player would carry ("player.maya": only characters a
 * bearer token may contain). {@code token("maya")} is a
 * verified player whose subject is "auth0|maya" and whose user (FakeAccountService) is
 * "user-maya"; anything the decoder can't read is refused like a bad signature.
 */
public final class FakeTokens {

    /** The default claim namespace (application.yml). */
    public static final String NAMESPACE = "https://friendlyfire.dev/";
    /** A token Auth0 would refuse (bad signature, expired, wrong audience). */
    public static final String INVALID = "invalid";

    private FakeTokens() {
    }

    public static JwtDecoder decoder() {
        return FakeTokens::decode;
    }

    public static String token(String name) {
        return "player." + name;
    }

    public static String unverified(String name) {
        return "unverified." + name;
    }

    public static String admin(String name) {
        return "admin." + name;
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }

    public static String subject(String name) {
        return "auth0|" + name;
    }

    public static String userId(String name) {
        return "user-" + name;
    }

    /** The name claim: "maya" -> "Maya". */
    public static String displayName(String name) {
        return name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);
    }

    private static Jwt decode(String token) {
        int dot = token.indexOf('.');
        String kind = dot < 0 ? "" : token.substring(0, dot);
        if (!List.of("player", "unverified", "admin").contains(kind)) {
            throw new BadJwtException("Unreadable token");
        }
        String name = token.substring(dot + 1);
        Instant now = Instant.now();
        Jwt.Builder jwt = Jwt.withTokenValue(token)
                .header("alg", "RS256")
                .subject(subject(name))
                .audience(List.of("https://api.friendlyfire.dev"))
                .issuedAt(now)
                .expiresAt(now.plus(1, ChronoUnit.HOURS))
                .claim(NAMESPACE + "email", name + "@example.com")
                .claim(NAMESPACE + "email_verified", !kind.equals("unverified"))
                .claim(NAMESPACE + "name", displayName(name));
        if (kind.equals("admin")) {
            jwt.claim(NAMESPACE + "roles", List.of("admin"));
        }
        return jwt.build();
    }
}
