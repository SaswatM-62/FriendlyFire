package com.friendlyfire.room.room;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryRoomRepositoryTest {

    @Test
    void savesAndFindsRoomByCode() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room room = new Room("ABC123", "host", "host");

        repository.save(room);

        assertSame(room, repository.findByCode("ABC123").orElseThrow());
    }

    @Test
    void deletesRoomByCode() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room room = new Room("ABC123", "host", "host");

        repository.save(room);
        repository.deleteByCode("ABC123");

        assertTrue(repository.findByCode("ABC123").isEmpty());
    }

    @Test
    void returnsEmptyWhenRoomDoesNotExist() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();

        assertTrue(repository.findByCode("ABC123").isEmpty());
    }
}