package com.friendlyfire.room.clients;

import java.util.Collection;
import java.util.Map;

/** Players' match data status, from AccountService.GetDataStatus (ADR-010). */
public interface DataStatusClient {

    /**
     * One entry per requested user ID. Throws a StatusRuntimeException if Account can't
     * answer.
     */
    Map<String, PlayerData> statuses(Collection<String> userIds);
}
