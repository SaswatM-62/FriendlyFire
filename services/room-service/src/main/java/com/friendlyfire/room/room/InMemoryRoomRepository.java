package com.friendlyfire.room.room;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRoomRepository implements RoomRepository {
    // TODO(ff): keep rooms in a ConcurrentHashMap keyed by room code
    //   - changes to one room must not race (lock per room, or one thread per room)
    //   - remove a room when it closes
}
