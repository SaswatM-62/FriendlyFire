package com.friendlyfire.account.testsupport;

import com.friendlyfire.account.scoring.ScoringClient;
import io.grpc.Status;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/** A Scoring that remembers who it anonymized, or is down (UNAVAILABLE). */
public final class FakeScoringClient implements ScoringClient {

    public final List<UUID> anonymized = new CopyOnWriteArrayList<>();
    public volatile boolean down;

    public void reset() {
        anonymized.clear();
        down = false;
    }

    @Override
    public void anonymizePlayer(UUID userId) {
        if (down) {
            throw Status.UNAVAILABLE.withDescription("Quiz history isn't available right now.").asRuntimeException();
        }
        anonymized.add(userId);
    }
}
