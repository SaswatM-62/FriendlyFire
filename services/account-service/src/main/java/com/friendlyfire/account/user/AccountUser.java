package com.friendlyfire.account.user;

import java.time.Instant;
import java.util.UUID;

/**
 * A FriendlyFire user stored by AccountService. {@code onboardedAt} is null until they
 * finish the new-player tutorial.
 */
public record AccountUser(
        UUID userId,
        String authSubject,
        String email,
        String displayName,
        Instant createdAt,
        Instant onboardedAt) {}
