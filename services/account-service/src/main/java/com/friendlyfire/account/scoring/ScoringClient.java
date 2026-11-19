package com.friendlyfire.account.scoring;

import java.util.UUID;

/** What Account asks Scoring. Throws a gRPC UNAVAILABLE status when Scoring doesn't answer. */
public interface ScoringClient {

    /** "Deleted player" in every past quiz of a user who deleted their account (ADR-027). */
    void anonymizePlayer(UUID userId);
}
