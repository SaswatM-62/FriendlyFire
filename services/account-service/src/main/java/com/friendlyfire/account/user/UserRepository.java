package com.friendlyfire.account.user;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private static final String USER_COLUMNS = "user_id, auth_subject, email, display_name, created_at";

    private static final RowMapper<AccountUser> USER_ROW_MAPPER = UserRepository::mapUser;

    private final JdbcClient jdbcClient;

    public UserRepository(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    public Optional<AccountUser> findById(UUID userId) {
        return jdbcClient.sql("SELECT " + USER_COLUMNS + " FROM account.users WHERE user_id = :userId")
                .param("userId", userId)
                .query(USER_ROW_MAPPER)
                .optional();
    }

    public Optional<AccountUser> findByAuthSubject(String authSubject) {
        return jdbcClient.sql("SELECT " + USER_COLUMNS + " FROM account.users WHERE auth_subject = :authSubject")
                .param("authSubject", authSubject)
                .query(USER_ROW_MAPPER)
                .optional();
    }

    public AccountUser create(String authSubject, String email, String displayName) {
        return jdbcClient.sql("""
                        INSERT INTO account.users (user_id, auth_subject, email, display_name)
                        VALUES (:userId, :authSubject, :email, :displayName)
                        RETURNING %s
                        """.formatted(USER_COLUMNS))
                .param("userId", UUID.randomUUID())
                .param("authSubject", authSubject)
                .param("email", email)
                .param("displayName", displayName)
                .query(USER_ROW_MAPPER)
                .single();
    }

    public EnsuredUser ensure(String authSubject, String email, String displayName) {
        Optional<AccountUser> created = jdbcClient.sql("""
                        INSERT INTO account.users (user_id, auth_subject, email, display_name)
                        VALUES (:userId, :authSubject, :email, :displayName)
                        ON CONFLICT (auth_subject) DO NOTHING
                        RETURNING %s
                        """.formatted(USER_COLUMNS))
                .param("userId", UUID.randomUUID())
                .param("authSubject", authSubject)
                .param("email", email)
                .param("displayName", displayName)
                .query(USER_ROW_MAPPER)
                .optional();

        return created.map(user -> new EnsuredUser(user, true))
                .orElseGet(() -> new EnsuredUser(
                        findByAuthSubject(authSubject)
                                .orElseThrow(() -> new IllegalStateException(
                                        "User was not found after an Auth0 subject conflict")),
                        false));
    }

    public Optional<AccountUser> changeDisplayName(UUID userId, String displayName) {
        return jdbcClient.sql("""
                        UPDATE account.users
                        SET display_name = :displayName
                        WHERE user_id = :userId
                        RETURNING %s
                        """.formatted(USER_COLUMNS))
                .param("userId", userId)
                .param("displayName", displayName)
                .query(USER_ROW_MAPPER)
                .optional();
    }

    public boolean delete(UUID userId) {
        return jdbcClient.sql("DELETE FROM account.users WHERE user_id = :userId")
                        .param("userId", userId)
                        .update()
                == 1;
    }

    private static AccountUser mapUser(ResultSet resultSet, int rowNumber) throws SQLException {
        return new AccountUser(
                resultSet.getObject("user_id", UUID.class),
                resultSet.getString("auth_subject"),
                resultSet.getString("email"),
                resultSet.getString("display_name"),
                resultSet.getTimestamp("created_at").toInstant());
    }
}
