package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.account.v1.RiotLink;

/** What the app may show about a linked Riot account. Never the PUUID. */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RiotLinkJson(String gameName, String tagLine, String region, String linkedAt) {

    public static RiotLinkJson from(RiotLink link) {
        return new RiotLinkJson(
                link.getGameName(),
                link.getTagLine(),
                link.getRegion(),
                ProtoJson.timestamp(link.hasLinkedAt(), link.getLinkedAt()));
    }
}
