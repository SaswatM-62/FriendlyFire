package com.friendlyfire.account.datastatus;

import com.friendlyfire.account.matchdata.MatchDataClient;
import com.friendlyfire.account.riotlink.RiotAccountLink;
import com.friendlyfire.account.riotlink.RiotLinkRepository;
import com.friendlyfire.proto.friendlyfire.account.v1.DataStatus;
import com.friendlyfire.proto.friendlyfire.account.v1.UserDataStatus;
import com.friendlyfire.proto.friendlyfire.matchdata.v1.PlayerSyncStatus;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/** Each user's match data status, and the sign-in sync that keeps it fresh. */
@Component
public class DataStatusService {

    private static final Logger log = LoggerFactory.getLogger(DataStatusService.class);

    private final RiotLinkRepository riotLinks;
    private final MatchDataClient matchData;
    private final Clock clock;

    public DataStatusService(RiotLinkRepository riotLinks, MatchDataClient matchData, Clock clock) {
        this.riotLinks = riotLinks;
        this.matchData = matchData;
        this.clock = clock;
    }

    /**
     * One status per requested ID, in the same order. IDs that aren't a known user are
     * NOT_LINKED. Throws UNAVAILABLE if a requested user is linked and Match Data can't
     * say how their sync is going.
     */
    public List<UserDataStatus> forUsers(Collection<String> userIds) {
        Set<UUID> ids = userIds.stream().map(DataStatusService::uuid).flatMap(Optional::stream)
                .collect(Collectors.toSet());
        Map<UUID, RiotAccountLink> links = riotLinks.findByUserIds(ids).stream()
                .collect(Collectors.toMap(RiotAccountLink::userId, Function.identity()));
        Map<UUID, PlayerSyncStatus> syncs = links.isEmpty() ? Map.of() : matchData.statuses(links.keySet());

        List<UserDataStatus> statuses = new ArrayList<>();
        for (String userId : userIds) {
            Optional<UUID> id = uuid(userId).filter(links::containsKey);
            statuses.add(id.isEmpty()
                    ? DataStatuses.notLinked(userId)
                    : DataStatuses.linked(userId, Optional.ofNullable(syncs.get(id.get())), clock.instant()));
        }
        return statuses;
    }

    /**
     * At sign-in: if the player is linked and their data isn't fresh (or the last sync
     * failed), starts a sync. Never throws: a sign-in must not fail because Match Data is
     * down.
     */
    public void syncIfStale(Optional<RiotAccountLink> link) {
        if (link.isEmpty()) {
            return;
        }
        RiotAccountLink riot = link.get();
        try {
            Optional<PlayerSyncStatus> sync = Optional.ofNullable(
                    matchData.statuses(List.of(riot.userId())).get(riot.userId()));
            DataStatus status = DataStatuses.linked(riot.userId().toString(), sync, clock.instant()).getStatus();
            if (status == DataStatus.DATA_STATUS_NOT_SYNCED || status == DataStatus.DATA_STATUS_FAILED) {
                matchData.sync(riot.userId(), riot.puuid(), riot.region());
            }
        } catch (StatusRuntimeException e) {
            log.warn("Couldn't check or start a match sync at sign-in for user {}: {}",
                    riot.userId(), e.getStatus().getDescription());
        }
    }

    private static Optional<UUID> uuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }
}
