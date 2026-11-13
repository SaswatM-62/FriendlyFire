package com.friendlyfire.room.room;

import java.util.Optional;

/** Where rooms live. */
public interface RoomRepository {

    Optional<Room> findByCode(String code);

    /**
     * Saves a new room. Returns false, and keeps the existing room, if another room
     * already has this code: generate a new code and try again.
     */
    boolean save(Room room);

    void deleteByCode(String code);
}
