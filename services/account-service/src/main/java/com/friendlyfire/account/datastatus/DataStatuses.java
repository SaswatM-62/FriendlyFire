package com.friendlyfire.account.datastatus;

import com.friendlyfire.proto.friendlyfire.account.v1.DataStatus;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import com.google.protobuf.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

/** Match Data's raw sync status to the data status the app shows (ADR-010). */
public final class DataStatuses {

    /** Data synced longer ago than this isn't fresh: the next sign-in syncs again. */
    public static final Duration FRESH_FOR = Duration.ofHours(24);

    private DataStatuses() {
    }

    /** A user with no Riot link. */
    public static UserDataStatus notLinked(String userId) {
        return UserDataStatus.newBuilder().setUserId(userId).setStatus(DataStatus.DATA_STATUS_NOT_LINKED).build();
    }

    /**
     * A linked user. {@code sync} is Match Data's status for them, if it knows them; a
     * linked player Match Data has never heard of is NOT_SYNCED.
     */
    public static UserDataStatus linked(String userId, Optional<PlayerSyncStatus> sync, Instant now) {
        UserDataStatus.Builder status = UserDataStatus.newBuilder().setUserId(userId);
        if (sync.isEmpty()) {
            return status.setStatus(DataStatus.DATA_STATUS_NOT_SYNCED).build();
        }
        PlayerSyncStatus raw = sync.get();
        status.setMatchesStored(raw.getMatchesStored());
        if (raw.hasLastSyncedAt()) {
            status.setLastSyncedAt(raw.getLastSyncedAt());
        }
        return switch (raw.getStatus()) {
            case SYNC_STATUS_IN_PROGRESS -> status
                    .setStatus(DataStatus.DATA_STATUS_SYNCING)
                    .setEstimatedSeconds(raw.getEstimatedSeconds())
                    .build();
            case SYNC_STATUS_FAILED -> status
                    .setStatus(DataStatus.DATA_STATUS_FAILED)
                    .setErrorMessage(raw.getErrorMessage())
                    .build();
            case SYNC_STATUS_COMPLETE -> status
                    .setStatus(fresh(raw, now) ? DataStatus.DATA_STATUS_SYNCED : DataStatus.DATA_STATUS_NOT_SYNCED)
                    .build();
            default -> status.setStatus(DataStatus.DATA_STATUS_NOT_SYNCED).build();
        };
    }

    private static boolean fresh(PlayerSyncStatus raw, Instant now) {
        if (!raw.hasLastSyncedAt()) {
            return false;
        }
        Timestamp at = raw.getLastSyncedAt();
        Instant syncedAt = Instant.ofEpochSecond(at.getSeconds(), at.getNanos());
        return syncedAt.isAfter(now.minus(FRESH_FOR));
    }
}
