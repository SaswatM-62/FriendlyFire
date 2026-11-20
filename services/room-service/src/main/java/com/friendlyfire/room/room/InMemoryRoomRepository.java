package com.friendlyfire.room.room;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/** Rooms in memory: a restart of room-service ends the rooms in progress. */
public class InMemoryRoomRepository implements RoomRepository {

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public Optional<Room> findByCode(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    @Override
    public Collection<Room> findAll() {
        return List.copyOf(rooms.values());
    }

    @Override
    public boolean save(Room room) {
        return rooms.putIfAbsent(room.getCode(), room) == null;
    }

    @Override
    public void deleteByCode(String code) {
        rooms.remove(code);
    }
}
