package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.account.v1.User;

/** GET and PATCH /api/me: the signed-in user. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record MeJson(
        String userId,
        String displayName,
        String email,
        String createdAt,
        /** Left out until the player links a Riot account. */
        RiotLinkJson riotLink) {

    public static MeJson from(User user) {
        return new MeJson(
                user.getUserId(),
                user.getDisplayName(),
                user.getEmail(),
                ProtoJson.timestamp(user.hasCreatedAt(), user.getCreatedAt()),
                user.hasRiotLink() ? RiotLinkJson.from(user.getRiotLink()) : null);
    }
}
