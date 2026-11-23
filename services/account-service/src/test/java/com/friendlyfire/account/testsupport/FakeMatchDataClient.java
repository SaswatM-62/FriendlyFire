package com.friendlyfire.account.testsupport;

import com.friendlyfire.account.matchdata.MatchDataClient;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import io.grpc.Status;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** A Match Data that remembers what it was asked, or is down (UNAVAILABLE, like the real client). */
public final class FakeMatchDataClient implements MatchDataClient {

    public final Map<UUID, PlayerSyncStatus> statuses = new ConcurrentHashMap<>();
    public final List<UUID> syncsStarted = new CopyOnWriteArrayList<>();
    public final List<UUID> deleted = new CopyOnWriteArrayList<>();
    public volatile boolean down;

    public void reset() {
        statuses.clear();
        syncsStarted.clear();
        deleted.clear();
        down = false;
    }

    @Override
    public void sync(UUID userId, String puuid, String region) {
        failIfDown();
        syncsStarted.add(userId);
    }

    @Override
    public Map<UUID, PlayerSyncStatus> statuses(Collection<UUID> userIds) {
        failIfDown();
        Map<UUID, PlayerSyncStatus> known = new ConcurrentHashMap<>();
        userIds.forEach(id -> {
            if (statuses.containsKey(id)) {
                known.put(id, statuses.get(id));
            }
        });
        return known;
    }

    @Override
    public void deletePlayerData(UUID userId) {
        failIfDown();
        deleted.add(userId);
    }

    private void failIfDown() {
        if (down) {
            throw Status.UNAVAILABLE.withDescription("Match data isn't available right now.").asRuntimeException();
        }
    }
}
