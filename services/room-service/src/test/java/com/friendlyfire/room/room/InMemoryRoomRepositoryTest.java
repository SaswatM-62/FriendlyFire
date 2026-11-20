package com.friendlyfire.room.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class InMemoryRoomRepositoryTest {

    @Test
    void savesAndFindsRoomByCode() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room room = new Room("ABC123", Instant.EPOCH);

        assertThat(repository.save(room)).isTrue();

        assertThat(repository.findByCode("ABC123")).containsSame(room);
    }

    @Test
    void saveKeepsTheExistingRoomWhenTheCodeIsTaken() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room first = new Room("ABC123", Instant.EPOCH);
        Room second = new Room("ABC123", Instant.EPOCH);

        repository.save(first);

        assertThat(repository.save(second)).isFalse();
        assertThat(repository.findByCode("ABC123")).containsSame(first);
    }

    @Test
    void deletesRoomByCode() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        Room room = new Room("ABC123", Instant.EPOCH);

        repository.save(room);
        repository.deleteByCode("ABC123");

        assertThat(repository.findByCode("ABC123")).isEmpty();
    }

    @Test
    void returnsEmptyWhenRoomDoesNotExist() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();

        assertThat(repository.findByCode("ABC123")).isEmpty();
    }

    @Test
    void listsEveryOpenRoom() {
        InMemoryRoomRepository repository = new InMemoryRoomRepository();
        repository.save(new Room("ABC123", Instant.EPOCH));
        repository.save(new Room("XYZ789", Instant.EPOCH));

        assertThat(repository.findAll()).extracting(Room::getCode).containsExactlyInAnyOrder("ABC123", "XYZ789");
    }
}
