package com.friendlyfire.gateway.security;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sign-in settings, from the environment (application.yml).
 *
 * @param issuerUri      the Auth0 tenant, e.g. "https://friendlyfire-dev.us.auth0.com/" (with
 *                       the trailing slash, exactly as tokens carry it). Blank: sign-in isn't
 *                       set up, so every route that needs it answers 401.
 * @param audience       the Gateway's Auth0 API identifier; tokens for anything else are refused
 * @param claimNamespace prefix of the custom claims an Auth0 Action adds (see TokenClaims)
 * @param production     APP_ENV is "prod"
 * @param inviteOnly     before Riot approves us, production admits only invited emails (FF-618)
 * @param invitedEmails  lower-case
 */
public record AuthSettings(
        String issuerUri,
        String audience,
        String claimNamespace,
        boolean production,
        boolean inviteOnly,
        Set<String> invitedEmails) {

    public AuthSettings {
        invitedEmails = Set.copyOf(invitedEmails);
    }

    public static AuthSettings of(
            String issuerUri, String audience, String claimNamespace, String appEnv, boolean inviteOnly,
            String invitedEmails) {
        Set<String> emails = Arrays.stream(invitedEmails.split(","))
                .map(email -> email.strip().toLowerCase(Locale.ROOT))
                .filter(email -> !email.isEmpty())
                .collect(Collectors.toUnmodifiableSet());
        return new AuthSettings(
                issuerUri.strip(), audience.strip(), claimNamespace.strip(), "prod".equalsIgnoreCase(appEnv.strip()),
                inviteOnly, emails);
    }

    public boolean signInConfigured() {
        return !issuerUri.isEmpty();
    }

    /** The invite list is checked only in production, until one setting turns it off. */
    public boolean inviteListApplies() {
        return production && inviteOnly;
    }
}
