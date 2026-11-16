package com.friendlyfire.account.riotlink;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/** Linked Riot accounts in the `account` schema (table created in V3, FF-105). */
@Repository
public class RiotLinkRepository {
    // TODO(ff): save and remove links (FF-110)
    //   - save a link (one per user); a PUUID can belong to only one user
    //     (ALREADY_EXISTS in CompleteRiotLink)
    //   - remove a link (UnlinkRiot, DeleteAccount)
    //   - the PUUID never leaves AccountService and MatchDataService

    private static final RowMapper<RiotAccountLink> ROW_MAPPER = RiotLinkRepository::mapRiotLink;

    private final JdbcClient jdbcClient;

    public RiotLinkRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<RiotAccountLink> findByUserId(UUID userId) {
        return jdbcClient.sql("""
                        SELECT user_id, puuid, game_name, tag_line, region, linked_at
                        FROM account.riot_links
                        WHERE user_id = :userId
                        """)
                .param("userId", userId)
                .query(ROW_MAPPER)
                .optional();
    }

    private static RiotAccountLink mapRiotLink(ResultSet resultSet, int rowNumber) throws SQLException {
        return new RiotAccountLink(
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("puuid"),
                resultSet.getString("game_name"),
                resultSet.getString("tag_line"),
                resultSet.getString("region"),
                resultSet.getTimestamp("linked_at").toInstant());
    }
}
