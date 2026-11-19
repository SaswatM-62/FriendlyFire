package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;

/**
 * GET /api/link/riot/status: the player's match data, for the sync banner and the
 * profile. The same shape as SyncStatusResponse in frontend/src/lib/api-types.ts.
 *
 * @param status           SYNCED, SYNCING, NOT_SYNCED, NOT_LINKED or FAILED
 * @param estimatedSeconds only while SYNCING with an estimate
 * @param lastSyncedAt     when the last successful sync finished; left out if never
 * @param matchesStored    Competitive and Unrated matches from the last 30 days
 * @param errorMessage     only when FAILED: a short reason that's safe to show
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record SyncStatusJson(
        String status,
        Integer estimatedSeconds,
        String lastSyncedAt,
        int matchesStored,
        String errorMessage) {

    public static SyncStatusJson from(UserDataStatus status) {
        return new SyncStatusJson(
                ProtoJson.enumName(status.getStatus(), "DATA_STATUS_"),
                status.getEstimatedSeconds() > 0 ? status.getEstimatedSeconds() : null,
                ProtoJson.timestamp(status.hasLastSyncedAt(), status.getLastSyncedAt()),
                status.getMatchesStored(),
                ProtoJson.text(status.getErrorMessage()));
    }
}
