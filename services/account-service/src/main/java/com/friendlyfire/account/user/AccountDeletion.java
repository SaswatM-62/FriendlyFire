package com.friendlyfire.account.user;

import com.friendlyfire.account.matchdata.MatchDataClient;
import com.friendlyfire.account.riotlink.LinkStateStore;
import com.friendlyfire.account.riotlink.RiotLinkRepository;
import com.friendlyfire.account.scoring.ScoringClient;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Deleting an account (ADR-027). The other services go first, so that if one can't be
 * reached this throws UNAVAILABLE with the user still here, and the player can try again:
 * nothing about them is left behind in a service that was skipped.
 *
 * <p>The Gateway has already taken them out of any room (RoomService.RemovePlayer). Account
 * never calls Room: Room calls Account, and a cycle would break the health checks.
 */
@Component
public class AccountDeletion {

    private final UserRepository users;
    private final RiotLinkRepository riotLinks;
    private final LinkStateStore linkStates;
    private final MatchDataClient matchData;
    private final ScoringClient scoring;

    public AccountDeletion(
            UserRepository users,
            RiotLinkRepository riotLinks,
            LinkStateStore linkStates,
            MatchDataClient matchData,
            ScoringClient scoring) {
        this.users = users;
        this.riotLinks = riotLinks;
        this.linkStates = linkStates;
        this.matchData = matchData;
        this.scoring = scoring;
    }

    /** Deletes the user everywhere. Nothing to do if they don't exist. */
    public void delete(UUID userId) {
        if (users.findById(userId).isEmpty()) {
            return;
        }
        if (riotLinks.findByUserId(userId).isPresent()) {
            // Match Data only holds data for linked players (unlinking deletes it first).
            matchData.deletePlayerData(userId);
        }
        scoring.anonymizePlayer(userId);
        linkStates.forget(userId);
        // Deleting the user also deletes their link (ON DELETE CASCADE).
        users.delete(userId);
    }
}
