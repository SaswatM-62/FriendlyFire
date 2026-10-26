package com.friendlyfire.account.riotlink;

import org.springframework.stereotype.Repository;

@Repository
public class RiotLinkRepository {
    // TODO(ff): store linked Riot accounts in the `account` schema
    //   - one link per user: PUUID, game name, tag line, region, linked_at
    //   - a PUUID can belong to only one user (ALREADY_EXISTS in CompleteRiotLink)
    //   - the PUUID never leaves AccountService and MatchDataService
    //   - uses: the DataSource (JdbcClient)
}
