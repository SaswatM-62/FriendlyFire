package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.account.v1.Look;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerLook;

/**
 * A character (FF-519): the favourite look on GET and PATCH /api/me, and each player's
 * look in a room snapshot, where {@code random} says their favourite icon or colour was
 * taken. Left out of /api/me until the player picks one.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record LookJson(String icon, String colour, String expression, Boolean random) {

    public static LookJson from(Look look) {
        return new LookJson(look.getIcon(), look.getColour(), look.getExpression(), null);
    }

    public static LookJson from(PlayerLook look) {
        return new LookJson(look.getIcon(), look.getColour(), look.getExpression(), look.getRandom());
    }
}
