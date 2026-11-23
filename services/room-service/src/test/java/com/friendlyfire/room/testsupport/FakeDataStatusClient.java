package com.friendlyfire.room.testsupport;

import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;
import com.friendlyfire.room.clients.DataStatusClient;
import com.friendlyfire.room.clients.PlayerData;
import io.grpc.Status;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/** Account's data status: everyone SYNCED with enough matches unless a test says otherwise. */
public final class FakeDataStatusClient implements DataStatusClient {

    public static final PlayerData SYNCED = new PlayerData(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED, 0, false);
    public static final PlayerData FEW_MATCHES = new PlayerData(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED, 0, true);

    public static PlayerData syncing(int seconds) {
        return new PlayerData(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCING, seconds, false);
    }

    private final Map<String, PlayerData> statuses = new ConcurrentHashMap<>();
    private volatile boolean down;
    private volatile int calls;

    public void set(String userId, PlayerData data) {
        statuses.put(userId, data);
    }

    public void down(boolean down) {
        this.down = down;
    }

    public int calls() {
        return calls;
    }

    @Override
    public Map<String, PlayerData> statuses(Collection<String> userIds) {
        calls++;
        if (down) {
            throw Status.UNAVAILABLE.withDescription("account is down").asRuntimeException();
        }
        Map<String, PlayerData> result = new LinkedHashMap<>();
        for (String id : userIds) {
            result.put(id, statuses.getOrDefault(id, SYNCED));
        }
        return result;
    }
}
