package com.friendlyfire.gateway.security;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.EnsureUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.Ticker;
import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Turns the Auth0 subject into our own user, with AccountService.EnsureUser (which also
 * creates them on their first visit). Users are cached per subject for a few minutes, so
 * most requests don't call Account; whatever changes the user (a new name, a Riot link,
 * deleting the account) updates or drops the cached copy.
 */
public class UserResolver {

    private final AccountServiceBlockingStub accounts;
    private final TokenClaims claims;
    private final Cache<String, User> users;

    public UserResolver(AccountServiceBlockingStub accounts, TokenClaims claims, Duration ttl, Clock clock) {
        this.accounts = accounts;
        this.claims = claims;
        Ticker ticker = () -> TimeUnit.MILLISECONDS.toNanos(clock.millis());
        this.users = Caffeine.newBuilder().expireAfterWrite(ttl).maximumSize(50_000).ticker(ticker).build();
    }

    /** The user for this token, from the cache when it's there. gRPC errors propagate. */
    public User resolve(Jwt jwt) {
        return users.get(jwt.getSubject(), subject -> ensure(jwt));
    }

    /**
     * Always asks Account (GET /api/me, on every sign-in and app load), so Account can start
     * a sync for stale match data, and refreshes the cache.
     */
    public User refresh(Jwt jwt) {
        User user = ensure(jwt);
        users.put(jwt.getSubject(), user);
        return user;
    }

    public void update(String subject, User user) {
        users.put(subject, user);
    }

    public void evict(String subject) {
        users.invalidate(subject);
    }

    private User ensure(Jwt jwt) {
        return accounts.ensureUser(EnsureUserRequest.newBuilder()
                        .setAuthSubject(jwt.getSubject())
                        .setEmail(claims.email(jwt))
                        .setSuggestedDisplayName(claims.name(jwt))
                        .build())
                .getUser();
    }
}
