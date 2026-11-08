package com.friendlyfire.account.riotlink;

import java.time.Instant;
import java.util.UUID;

/** A Riot account linked to a FriendlyFire user. */
public record RiotAccountLink(
        UUID userId,
        String puuid,
        String gameName,
        String tagLine,
        String region,
        Instant linkedAt) {}
