package com.friendlyfire.room.clients;

import java.util.Optional;

/** Players' favourite looks, from AccountService.GetUser (FF-519). */
public interface FavouriteLookClient {

    /**
     * The user's favourite look, or empty if they haven't picked one. Throws a
     * StatusRuntimeException if Account can't answer.
     */
    Optional<FavouriteLook> favourite(String userId);
}
