package com.friendlyfire.gateway.security;

import com.friendlyfire.gateway.web.ApiException;
import com.friendlyfire.gateway.web.ErrorCodes;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Who may use FriendlyFire once their token checks out: REST requests (SignInRulesFilter)
 * and WebSocket connections (ConnectInterceptor) both apply these rules.
 */
public final class SignInRules {

    private final AuthSettings settings;
    private final TokenClaims claims;

    public SignInRules(AuthSettings settings, TokenClaims claims) {
        this.settings = settings;
        this.claims = claims;
    }

    /**
     * Throws a 403 ApiException if this player may not use the app: their email isn't
     * verified (FF-103), or in production they aren't on the invite list and aren't an
     * admin (FF-618). The frontend sends them to /verify-email and /invite-only.
     */
    public void check(Jwt jwt) {
        if (!claims.emailVerified(jwt)) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.EMAIL_NOT_VERIFIED,
                    "Verify your email address, then sign in again.");
        }
        if (settings.inviteListApplies() && !claims.isAdmin(jwt)
                && !settings.invitedEmails().contains(claims.email(jwt).toLowerCase(Locale.ROOT))) {
            throw new ApiException(HttpStatus.FORBIDDEN, ErrorCodes.INVITE_ONLY, "FriendlyFire is invite-only for now.");
        }
    }
}
