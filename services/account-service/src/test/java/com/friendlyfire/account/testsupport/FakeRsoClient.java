package com.friendlyfire.account.testsupport;

import com.friendlyfire.account.rso.RiotAccount;
import com.friendlyfire.account.rso.RsoClient;
import io.grpc.Status;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A Riot Sign-On: {@code codes} says which Riot account each code signs in as. Unknown
 * codes are rejected (INVALID_ARGUMENT); {@code down} makes it unreachable (UNAVAILABLE).
 */
public final class FakeRsoClient implements RsoClient {

    public static final String AUTHORIZE = "https://rso.test/authorize";

    public final Map<String, RiotAccount> codes = new ConcurrentHashMap<>();
    public volatile boolean down;

    public void reset() {
        codes.clear();
        down = false;
    }

    @Override
    public String authorizeUrl(String state, String redirectUri) {
        return AUTHORIZE + "?state=" + state + "&redirect_uri=" + redirectUri;
    }

    @Override
    public RiotAccount account(String code, String redirectUri) {
        if (down) {
            throw Status.UNAVAILABLE.withDescription("Riot Sign-On can't be reached").asRuntimeException();
        }
        RiotAccount account = codes.get(code);
        if (account == null) {
            throw Status.INVALID_ARGUMENT.withDescription("Riot didn't accept that sign-in.").asRuntimeException();
        }
        return account;
    }

    /** The state inside a URL from {@link #authorizeUrl}. */
    public static String stateIn(String authorizeUrl) {
        String afterState = authorizeUrl.substring(authorizeUrl.indexOf("state=") + "state=".length());
        return afterState.substring(0, afterState.indexOf('&'));
    }
}
