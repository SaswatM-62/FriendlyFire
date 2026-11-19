package com.friendlyfire.account.matchdata;

import com.friendlyfire.proto.friendlyfire.matchdata.v1.DeletePlayerDataRequest;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.GetSyncStatusRequest;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.MatchDataServiceGrpc.MatchDataServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.SyncPlayerRequest;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.springframework.stereotype.Component;

/** MatchDataClient over gRPC (MATCHDATA_GRPC_ADDR). */
@Component
public class GrpcMatchDataClient implements MatchDataClient {

    // Short: EnsureUser waits for these on every sign-in and app load.
    static final Duration QUICK = Duration.ofSeconds(2);
    // Deleting can take longer: Match Data stops a running sync first.
    static final Duration DELETE = Duration.ofSeconds(10);

    private final MatchDataServiceBlockingStub matchData;

    public GrpcMatchDataClient(MatchDataServiceBlockingStub matchData) {
        this.matchData = matchData;
    }

    @Override
    public void sync(UUID userId, String puuid, String region) {
        call(() -> within(QUICK).syncPlayer(SyncPlayerRequest.newBuilder()
                .setUserId(userId.toString())
                .setGame("valorant")
                .setExternalPlayerId(puuid)
                .setRegion(region)
                .build()));
    }

    @Override
    public Map<UUID, PlayerSyncStatus> statuses(Collection<UUID> userIds) {
        var request = GetSyncStatusRequest.newBuilder();
        userIds.forEach(id -> request.addUserIds(id.toString()));
        var response = call(() -> within(QUICK).getSyncStatus(request.build()));
        Map<UUID, PlayerSyncStatus> byUser = new HashMap<>();
        for (PlayerSyncStatus status : response.getStatusesList()) {
            try {
                byUser.put(UUID.fromString(status.getUserId()), status);
            } catch (IllegalArgumentException e) {
                // Not one we asked about: ignore it.
            }
        }
        return byUser;
    }

    @Override
    public void deletePlayerData(UUID userId) {
        call(() -> within(DELETE).deletePlayerData(DeletePlayerDataRequest.newBuilder()
                .setUserId(userId.toString())
                .build()));
    }

    private MatchDataServiceBlockingStub within(Duration deadline) {
        return matchData.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }

    private static <T> T call(Supplier<T> rpc) {
        try {
            return rpc.get();
        } catch (StatusRuntimeException e) {
            throw Status.UNAVAILABLE
                    .withDescription("Match data isn't available right now. Try again in a few minutes.")
                    .withCause(e)
                    .asRuntimeException();
        }
    }
}
