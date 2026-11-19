package com.friendlyfire.account.riotlink;

import com.friendlyfire.account.rso.RiotAccount;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

/**
 * Linked Riot accounts in the `account` schema (table created in V3): one link per user,
 * and a Riot account (PUUID) can belong to only one user. The PUUID never leaves
 * AccountService and MatchDataService.
 */
@Repository
public class RiotLinkRepository {

    private static final String COLUMNS = "user_id, puuid, game_name, tag_line, region, linked_at";
    private static final RowMapper<RiotAccountLink> ROW_MAPPER = RiotLinkRepository::mapRiotLink;

    private final JdbcClient jdbcClient;

    public RiotLinkRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<RiotAccountLink> findByUserId(UUID userId) {
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM account.riot_links WHERE user_id = :userId")
                .param("userId", userId)
                .query(ROW_MAPPER)
                .optional();
    }

    public List<RiotAccountLink> findByUserIds(Collection<UUID> userIds) {
        if (userIds.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql("SELECT " + COLUMNS + " FROM account.riot_links WHERE user_id IN (:userIds)")
                .param("userIds", userIds)
                .query(ROW_MAPPER)
                .list();
    }

    /**
     * Links the user to this Riot account, replacing any link they had. Throws Spring's
     * DuplicateKeyException if the Riot account is already linked to another user.
     */
    public RiotAccountLink save(UUID userId, RiotAccount account, Instant linkedAt) {
        return jdbcClient.sql("""
                        INSERT INTO account.riot_links (user_id, puuid, game_name, tag_line, region, linked_at)
                        VALUES (:userId, :puuid, :gameName, :tagLine, :region, :linkedAt)
                        ON CONFLICT (user_id) DO UPDATE SET
                            puuid = EXCLUDED.puuid,
                            game_name = EXCLUDED.game_name,
                            tag_line = EXCLUDED.tag_line,
                            region = EXCLUDED.region,
                            linked_at = EXCLUDED.linked_at
                        RETURNING %s
                        """.formatted(COLUMNS))
                .param("userId", userId)
                .param("puuid", account.puuid())
                .param("gameName", account.gameName())
                .param("tagLine", account.tagLine())
                .param("region", account.region())
                .param("linkedAt", Timestamp.from(linkedAt))
                .query(ROW_MAPPER)
                .single();
    }

    /** Removes the user's link. False if they had none. */
    public boolean deleteByUserId(UUID userId) {
        return jdbcClient.sql("DELETE FROM account.riot_links WHERE user_id = :userId")
                        .param("userId", userId)
                        .update()
                == 1;
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
