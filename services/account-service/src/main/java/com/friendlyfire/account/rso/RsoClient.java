package com.friendlyfire.account.rso;

public interface RsoClient {
    // TODO(ff): talk to Riot Sign-On (or mock-rso-service until Riot approves the app)
    //   - build the authorize URL for a state and redirect_uri
    //   - swap an authorization code for a token (/token)
    //   - look up the player's PUUID and Riot ID with that token
    //     (/riot/account/v1/accounts/me)
    //   - pick MockRsoClient or RiotRsoClient from configuration
}
