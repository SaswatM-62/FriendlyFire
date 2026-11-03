package com.friendlyfire.room.room;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Repository;

@Repository
public class InMemoryRoomRepository implements RoomRepository {

    private final ConcurrentHashMap<String, Room> rooms = new ConcurrentHashMap<>();

    @Override
    public Optional<Room> findByCode(String code) {
        return Optional.ofNullable(rooms.get(code));
    }

    @Override
    public void save(Room room) {
        rooms.put(room.getCode(), room);
    }

    @Override
    public void deleteByCode(String code) {
        rooms.remove(code);
    }
}