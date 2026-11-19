package com.friendlyfire.room.clients;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.account.v1.DataStatus;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;
import org.junit.jupiter.api.Test;

class GrpcDataStatusClientTest {

    private static PlayerData map(DataStatus status, int estimate, int matches) {
        return GrpcDataStatusClient.toPlayerData(UserDataStatus.newBuilder()
                .setUserId("u")
                .setStatus(status)
                .setEstimatedSeconds(estimate)
                .setMatchesStored(matches)
                .build());
    }

    @Test
    void aSyncedPlayerWithFewerThanFiveMatchesHasNotEnough() {
        assertThat(map(DataStatus.DATA_STATUS_SYNCED, 0, 4))
                .isEqualTo(new PlayerData(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED, 0, true));
        assertThat(map(DataStatus.DATA_STATUS_SYNCED, 0, 5).notEnoughMatches()).isFalse();
    }

    @Test
    void theEstimateOnlyCountsWhileSyncing() {
        assertThat(map(DataStatus.DATA_STATUS_SYNCING, 90, 0))
                .isEqualTo(new PlayerData(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCING, 90, false));
        assertThat(map(DataStatus.DATA_STATUS_NOT_SYNCED, 90, 0).estimatedSeconds()).isZero();
    }

    @Test
    void everyStatusMapsAcross() {
        assertThat(map(DataStatus.DATA_STATUS_NOT_LINKED, 0, 0).status())
                .isEqualTo(PlayerDataStatus.PLAYER_DATA_STATUS_NOT_LINKED);
        assertThat(map(DataStatus.DATA_STATUS_FAILED, 0, 0).status())
                .isEqualTo(PlayerDataStatus.PLAYER_DATA_STATUS_FAILED);
        assertThat(map(DataStatus.DATA_STATUS_UNSPECIFIED, 0, 0).status())
                .isEqualTo(PlayerDataStatus.PLAYER_DATA_STATUS_UNSPECIFIED);
    }
}
