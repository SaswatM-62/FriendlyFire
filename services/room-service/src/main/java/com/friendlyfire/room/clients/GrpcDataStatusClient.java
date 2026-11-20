package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.GetDataStatusRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;
import java.time.Duration;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class GrpcDataStatusClient implements DataStatusClient {

    /** A player needs this many matches to be in questions (the contract's rule). */
    static final int ENOUGH_MATCHES = 5;
    private static final Duration DEADLINE = Duration.ofSeconds(2);

    private final AccountServiceBlockingStub accounts;

    public GrpcDataStatusClient(AccountServiceBlockingStub accounts) {
        this.accounts = accounts;
    }

    @Override
    public Map<String, PlayerData> statuses(Collection<String> userIds) {
        var response = accounts.withDeadlineAfter(DEADLINE.toMillis(), TimeUnit.MILLISECONDS)
                .getDataStatus(GetDataStatusRequest.newBuilder().addAllUserIds(userIds).build());
        Map<String, PlayerData> result = new LinkedHashMap<>();
        for (UserDataStatus status : response.getStatusesList()) {
            result.put(status.getUserId(), toPlayerData(status));
        }
        return result;
    }

    static PlayerData toPlayerData(UserDataStatus status) {
        PlayerDataStatus mapped = switch (status.getStatus()) {
            case DATA_STATUS_SYNCED -> PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED;
            case DATA_STATUS_SYNCING -> PlayerDataStatus.PLAYER_DATA_STATUS_SYNCING;
            case DATA_STATUS_NOT_SYNCED -> PlayerDataStatus.PLAYER_DATA_STATUS_NOT_SYNCED;
            case DATA_STATUS_NOT_LINKED -> PlayerDataStatus.PLAYER_DATA_STATUS_NOT_LINKED;
            case DATA_STATUS_FAILED -> PlayerDataStatus.PLAYER_DATA_STATUS_FAILED;
            default -> PlayerDataStatus.PLAYER_DATA_STATUS_UNSPECIFIED;
        };
        boolean synced = mapped == PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED;
        int estimate = mapped == PlayerDataStatus.PLAYER_DATA_STATUS_SYNCING ? status.getEstimatedSeconds() : 0;
        return new PlayerData(mapped, estimate, synced && status.getMatchesStored() < ENOUGH_MATCHES);
    }
}
