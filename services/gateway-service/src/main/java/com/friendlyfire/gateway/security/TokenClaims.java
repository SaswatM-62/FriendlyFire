package com.friendlyfire.gateway.security;

import java.util.List;
import java.util.Locale;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Reads who the player is from an Auth0 access token.
 *
 * <p>Access tokens carry only the subject by default. The Auth0 post-login Action in
 * infra/auth0/post-login-action.js adds email, email_verified, name and roles under a
 * namespace (AUTH0_CLAIM_NAMESPACE, as Auth0 recommends for custom claims). The plain OIDC
 * names (email, email_verified, name) are read too, if a token has them.
 */
public final class TokenClaims {

    static final String ADMIN_ROLE = "admin";

    private final String namespace;

    public TokenClaims(String namespace) {
        this.namespace = namespace;
    }

    /** Empty if the token doesn't say. */
    public String email(Jwt jwt) {
        return string(jwt, "email");
    }

    /** Only a verified email counts: an unverified or missing claim is false. */
    public boolean emailVerified(Jwt jwt) {
        Object value = claim(jwt, "email_verified");
        return Boolean.TRUE.equals(value) || "true".equals(value);
    }

    /** A name to suggest when the user is created; empty if the token has none. */
    public String name(Jwt jwt) {
        return string(jwt, "name");
    }

    /** The Auth0 "admin" role, given to the project managers (FF-602). */
    public boolean isAdmin(Jwt jwt) {
        Object roles = jwt.getClaims().get(namespace + "roles");
        if (!(roles instanceof List<?> list)) {
            return false;
        }
        return list.stream().anyMatch(role -> role instanceof String name && name.toLowerCase(Locale.ROOT).equals(ADMIN_ROLE));
    }

    private String string(Jwt jwt, String name) {
        Object value = claim(jwt, name);
        return value instanceof String text ? text.strip() : "";
    }

    private Object claim(Jwt jwt, String name) {
        Object namespaced = jwt.getClaims().get(namespace + name);
        return namespaced != null ? namespaced : jwt.getClaims().get(name);
    }
}
