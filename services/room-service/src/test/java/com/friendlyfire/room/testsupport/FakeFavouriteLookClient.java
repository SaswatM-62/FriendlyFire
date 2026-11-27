package com.friendlyfire.room.testsupport;

import com.friendlyfire.room.clients.FavouriteLook;
import com.friendlyfire.room.clients.FavouriteLookClient;
import io.grpc.Status;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Account's favourite looks: nobody has picked one unless a test says otherwise. */
public final class FakeFavouriteLookClient implements FavouriteLookClient {

    private final Map<String, FavouriteLook> looks = new ConcurrentHashMap<>();
    private volatile boolean down;

    public void set(String userId, String icon, String colour, String expression) {
        looks.put(userId, new FavouriteLook(icon, colour, expression));
    }

    public void down(boolean down) {
        this.down = down;
    }

    @Override
    public Optional<FavouriteLook> favourite(String userId) {
        if (down) {
            throw Status.UNAVAILABLE.withDescription("account is down").asRuntimeException();
        }
        return Optional.ofNullable(looks.get(userId));
    }
}
