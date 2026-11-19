package com.friendlyfire.account.riotlink;

import com.friendlyfire.account.matchdata.MatchDataClient;
import com.friendlyfire.account.rso.RiotAccount;
import com.friendlyfire.account.rso.RsoClient;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Component;

/**
 * Linking and unlinking a Riot account (ADR-007: Riot Sign-On only links accounts, it
 * never signs players in). Errors are thrown as gRPC statuses from the contract.
 */
@Component
public class RiotLinkService {

    private static final Logger log = LoggerFactory.getLogger(RiotLinkService.class);

    private final LinkStateStore linkStates;
    private final RsoClient rso;
    private final RiotLinkRepository riotLinks;
    private final MatchDataClient matchData;
    private final Clock clock;

    public RiotLinkService(
            LinkStateStore linkStates,
            RsoClient rso,
            RiotLinkRepository riotLinks,
            MatchDataClient matchData,
            Clock clock) {
        this.linkStates = linkStates;
        this.rso = rso;
        this.riotLinks = riotLinks;
        this.matchData = matchData;
        this.clock = clock;
    }

    /** Step 1: the Riot Sign-On URL, carrying a new one-time state for this user. */
    public String start(UUID userId, String redirectUri) {
        if (redirectUri == null || redirectUri.isBlank()) {
            throw Status.INVALID_ARGUMENT.withDescription("redirect_uri is required").asRuntimeException();
        }
        return rso.authorizeUrl(linkStates.create(userId, redirectUri), redirectUri);
    }

    /**
     * Step 2, after Riot sends the browser back: checks the state, swaps the code for the
     * player's Riot account, saves the link and starts a match sync.
     */
    public RiotAccountLink complete(UUID userId, String code, String state, String redirectUri) {
        if (!linkStates.take(state, userId, redirectUri)) {
            throw Status.INVALID_ARGUMENT
                    .withDescription("That link request expired or wasn't started here. Start linking again.")
                    .asRuntimeException();
        }
        RiotAccount account = rso.account(code, redirectUri);

        Optional<RiotAccountLink> previous = riotLinks.findByUserId(userId);
        if (previous.isPresent() && !previous.get().puuid().equals(account.puuid())) {
            // A different Riot account replaces the old one: its matches go first.
            matchData.deletePlayerData(userId);
        }
        RiotAccountLink link;
        try {
            link = riotLinks.save(userId, account, clock.instant());
        } catch (DuplicateKeyException e) {
            throw Status.ALREADY_EXISTS
                    .withDescription("That Riot account is already linked to another FriendlyFire account.")
                    .asRuntimeException();
        }
        try {
            matchData.sync(userId, account.puuid(), account.region());
        } catch (StatusRuntimeException e) {
            // The link stands; the next sign-in tries the sync again (DataStatusService).
            log.warn("Linked user {}, but couldn't start their match sync: {}", userId, e.getStatus().getDescription());
        }
        return link;
    }

    /**
     * Removes the link and that player's match data. Match Data deletes first: if it can't,
     * this throws UNAVAILABLE and keeps the link, so the player can try again and no
     * matches are left behind without a link.
     */
    public void unlink(UUID userId) {
        if (riotLinks.findByUserId(userId).isEmpty()) {
            return;
        }
        matchData.deletePlayerData(userId);
        riotLinks.deleteByUserId(userId);
    }
}
