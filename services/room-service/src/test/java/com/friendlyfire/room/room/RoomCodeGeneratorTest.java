
package com.friendlyfire.room.room;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Random;

import org.junit.jupiter.api.Test;

class RoomCodeGeneratorTest {

    @Test
    void generatedCodeHasSixAllowedCharacters() {
        RoomRepository repository = new InMemoryRoomRepository();
        RoomCodeGenerator generator =
                new RoomCodeGenerator(repository, new Random(42));

        String excluded = "0O1IL5S2Z";

        for (int i = 0; i < 1000; i++) {
            String code = generator.generateCode();

            assertThat(code).hasSize(6);

            for (char character : code.toCharArray()) {
                assertThat(excluded.indexOf(character)).isEqualTo(-1);
                assertThat(Character.isLetterOrDigit(character)).isTrue();
            }
        }
    }

    @Test
    void retriesWhenGeneratedCodeIsAlreadyTaken() {
        RoomRepository repository = new InMemoryRoomRepository();

        RoomCodeGenerator firstGenerator =
                new RoomCodeGenerator(repository, new Random(42));

        String takenCode = firstGenerator.generateCode();
        String expectedNextCode = firstGenerator.generateCode();

        repository.save(new Room(takenCode, "host", "Host"));

        RoomCodeGenerator generator =
                new RoomCodeGenerator(repository, new Random(42));

        String generatedCode = generator.generateCode();

        assertThat(generatedCode).isEqualTo(expectedNextCode);
        assertThat(generatedCode).isNotEqualTo(takenCode);
    }
}
