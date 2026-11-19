package com.friendlyfire.account.rso;

/**
 * Riot Sign-On, or mock-rso-service until Riot approves the app. RsoConfig picks the
 * implementation.
 *
 * <p>Failures are thrown as gRPC statuses, ready for CompleteRiotLink to return:
 * INVALID_ARGUMENT for a code Riot rejects, UNAVAILABLE when Riot can't be reached.
 */
public interface RsoClient {

    /** Where to send the browser to sign in to Riot, carrying the one-time state. */
    String authorizeUrl(String state, String redirectUri);

    /**
     * Swaps a code from the authorize step for the player's Riot account. The access
     * token is used once here and never kept or logged.
     */
    RiotAccount account(String code, String redirectUri);
}
