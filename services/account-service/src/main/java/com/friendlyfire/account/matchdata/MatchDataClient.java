package com.friendlyfire.account.matchdata;

import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;

/**
 * What Account asks Match Data. Every method throws a gRPC UNAVAILABLE status when Match
 * Data doesn't answer (or doesn't implement the call yet), so callers decide whether
 * that should stop them: deleting data always stops, starting a sync never does.
 */
public interface MatchDataClient {

    /** Starts a background sync of the player's matches (does nothing if one is running). */
    void sync(UUID userId, String puuid, String region);

    /** Raw sync status per user ID; users Match Data doesn't know may be missing. */
    Map<UUID, PlayerSyncStatus> statuses(Collection<UUID> userIds);

    /** Deletes everything Match Data stores for the user. */
    void deletePlayerData(UUID userId);
}
