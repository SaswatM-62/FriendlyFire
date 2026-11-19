package com.friendlyfire.account.datastatus;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.account.v1.DataStatus;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncStatus;
import com.google.protobuf.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class DataStatusesTest {

    private static final Instant NOW = Instant.parse("2026-11-16T12:00:00Z");

    @Test
    void aPlayerWithoutARiotLinkIsNotLinked() {
        assertThat(DataStatuses.notLinked("u1").getStatus()).isEqualTo(DataStatus.DATA_STATUS_NOT_LINKED);
    }

    @Test
    void aLinkedPlayerMatchDataDoesntKnowIsNotSynced() {
        assertThat(DataStatuses.linked("u1", Optional.empty(), NOW).getStatus())
                .isEqualTo(DataStatus.DATA_STATUS_NOT_SYNCED);
        assertThat(DataStatuses.linked("u1", Optional.of(raw(SyncStatus.SYNC_STATUS_NOT_LINKED)), NOW).getStatus())
                .isEqualTo(DataStatus.DATA_STATUS_NOT_SYNCED);
    }

    @Test
    void aSyncUnder24HoursOldIsSyncedAndOlderIsNot() {
        PlayerSyncStatus recent = raw(SyncStatus.SYNC_STATUS_COMPLETE).toBuilder()
                .setLastSyncedAt(at(NOW.minus(Duration.ofHours(23)))).setMatchesStored(41).build();
        PlayerSyncStatus old = recent.toBuilder().setLastSyncedAt(at(NOW.minus(Duration.ofHours(24)))).build();

        UserDataStatus synced = DataStatuses.linked("u1", Optional.of(recent), NOW);
        assertThat(synced.getStatus()).isEqualTo(DataStatus.DATA_STATUS_SYNCED);
        assertThat(synced.getMatchesStored()).isEqualTo(41);
        assertThat(synced.getLastSyncedAt()).isEqualTo(recent.getLastSyncedAt());
        assertThat(DataStatuses.linked("u1", Optional.of(old), NOW).getStatus())
                .isEqualTo(DataStatus.DATA_STATUS_NOT_SYNCED);
    }

    @Test
    void aCompleteSyncWithNoTimeIsNotSynced() {
        assertThat(DataStatuses.linked("u1", Optional.of(raw(SyncStatus.SYNC_STATUS_COMPLETE)), NOW).getStatus())
                .isEqualTo(DataStatus.DATA_STATUS_NOT_SYNCED);
    }

    @Test
    void aRunningSyncIsSyncingWithItsEstimate() {
        UserDataStatus syncing = DataStatuses.linked("u1", Optional.of(
                raw(SyncStatus.SYNC_STATUS_IN_PROGRESS).toBuilder().setEstimatedSeconds(120).build()), NOW);

        assertThat(syncing.getStatus()).isEqualTo(DataStatus.DATA_STATUS_SYNCING);
        assertThat(syncing.getEstimatedSeconds()).isEqualTo(120);
    }

    @Test
    void aFailedSyncIsFailedWithItsReason() {
        UserDataStatus failed = DataStatuses.linked("u1", Optional.of(
                raw(SyncStatus.SYNC_STATUS_FAILED).toBuilder().setErrorMessage("HenrikDev is down").build()), NOW);

        assertThat(failed.getStatus()).isEqualTo(DataStatus.DATA_STATUS_FAILED);
        assertThat(failed.getErrorMessage()).isEqualTo("HenrikDev is down");
    }

    private static PlayerSyncStatus raw(SyncStatus status) {
        return PlayerSyncStatus.newBuilder().setUserId("u1").setStatus(status).build();
    }

    private static Timestamp at(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }
}
