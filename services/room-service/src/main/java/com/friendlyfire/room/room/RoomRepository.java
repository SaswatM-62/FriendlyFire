package com.friendlyfire.room.room;

import java.util.Optional;

/** Where rooms live. */
public interface RoomRepository {

    Optional<Room> findByCode(String code);

    void save(Room room);

    void deleteByCode(String code);
}