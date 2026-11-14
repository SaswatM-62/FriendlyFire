package com.friendlyfire.account.user;

import java.time.Instant;
import java.util.UUID;

/** A FriendlyFire user stored by AccountService. */
public record AccountUser(
        UUID userId,
        String authSubject,
        String email,
        String displayName,
        Instant createdAt) {}
