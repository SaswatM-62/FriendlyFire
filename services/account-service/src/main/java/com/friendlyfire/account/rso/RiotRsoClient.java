package com.friendlyfire.account.rso;

/** Placeholder for the real Riot Sign-On; MockRsoClient is used until then (RsoConfig). */
public class RiotRsoClient {
    // TODO(ff): implement RsoClient for the real Riot Sign-On, once Riot approves the app
    //   - same flow as MockRsoClient: /authorize in the browser, /token (with the client id
    //     and secret as HTTP Basic auth), then account-v1 "me" with the access token
    //   - the region needs Riot's active-shards endpoint (the mock returns it directly)
    //   - client id and secret come from env vars, never from the repo
    //   - errors: INVALID_ARGUMENT for a rejected code, UNAVAILABLE when Riot can't be reached
    //   - switch RsoConfig to it with an env var
}
