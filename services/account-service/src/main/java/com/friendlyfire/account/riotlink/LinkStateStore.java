package com.friendlyfire.account.riotlink;

import org.springframework.stereotype.Component;

@Component
public class LinkStateStore {
    // TODO(ff): keep the one-time `state` values of Riot links in progress
    //   - create a random state for a user, valid for 10 minutes
    //   - take it once: CompleteRiotLink checks it belongs to the user and removes it
    //   - uses: an injected java.time.Clock, so tests can control expiry
}
