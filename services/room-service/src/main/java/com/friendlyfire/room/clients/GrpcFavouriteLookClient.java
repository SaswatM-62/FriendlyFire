package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.account.v1.GetUserRequest;
import com.friendlyfire.proto.friendlyfire.account.v1.Look;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class GrpcFavouriteLookClient implements FavouriteLookClient {

    private static final Duration DEADLINE = Duration.ofSeconds(2);

    private final AccountServiceBlockingStub accounts;

    public GrpcFavouriteLookClient(AccountServiceBlockingStub accounts) {
        this.accounts = accounts;
    }

    @Override
    public Optional<FavouriteLook> favourite(String userId) {
        var user = accounts.withDeadlineAfter(DEADLINE.toMillis(), TimeUnit.MILLISECONDS)
                .getUser(GetUserRequest.newBuilder().setUserId(userId).build())
                .getUser();
        if (!user.hasLook()) {
            return Optional.empty();
        }
        Look look = user.getLook();
        return Optional.of(new FavouriteLook(look.getIcon(), look.getColour(), look.getExpression()));
    }
}
