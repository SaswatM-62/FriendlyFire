package com.friendlyfire.room.room;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class InMemoryRoomRepositoryTest {

    @Test
    void savesAndFindsRoomByCode() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room room = new Room("ABC123", "host", "host");

        assertThat(repository.save(room)).isTrue();

        assertThat(repository.findByCode("ABC123")).containsSame(room);
    }

    @Test
    void saveKeepsTheExistingRoomWhenTheCodeIsTaken() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room first = new Room("ABC123", "host1", "Host 1");
        Room second = new Room("ABC123", "host2", "Host 2");

        repository.save(first);

        assertThat(repository.save(second)).isFalse();
        assertThat(repository.findByCode("ABC123")).containsSame(first);
    }

    @Test
    void deletesRoomByCode() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room room = new Room("ABC123", "host", "host");

        repository.save(room);
        repository.deleteByCode("ABC123");

        assertThat(repository.findByCode("ABC123")).isEmpty();
    }

    @Test
    void returnsEmptyWhenRoomDoesNotExist() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();

        assertThat(repository.findByCode("ABC123")).isEmpty();
    }
}
