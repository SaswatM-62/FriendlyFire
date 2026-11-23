package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;

/**
 * A player's match data, as the lobby shows it.
 *
 * @param notEnoughMatches synced, but fewer than 5 Competitive or Unrated matches: they
 *                         play, but no question is about them
 */
public record PlayerData(PlayerDataStatus status, int estimatedSeconds, boolean notEnoughMatches) {

    /** Before Account has answered for this player. */
    public static final PlayerData UNKNOWN =
            new PlayerData(PlayerDataStatus.PLAYER_DATA_STATUS_UNSPECIFIED, 0, false);
}
