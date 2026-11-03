package com.friendlyfire.account.user;

import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {
    // TODO(ff): store FriendlyFire users in the `account` schema (users table)
    //   - find by user_id and by Auth0 subject, create, change display name, delete
    //   - user_id is our own UUID; the Auth0 subject is only used to find the user
    //   - uses: the DataSource (JdbcClient); add tables with a Flyway migration in db/migration
}
