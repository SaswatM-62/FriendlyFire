package com.friendlyfire.room.room;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Random;

import org.junit.jupiter.api.Test;

class RoomCodeGeneratorTest {

    private static final String CHARACTERS = "ABCDEFGHJKMNPQRTUVWXY346789";

    @Test
    void generatedCodeHasSixAllowedCharacters() {
        RoomRepository repository = new InMemoryRoomRepository();
        RoomCodeGenerator generator =
                new RoomCodeGenerator(repository, new Random(42));

        String code = generator.generateCode();

        assertEquals(6, code.length());

        for (char character : code.toCharArray()) {
            assertTrue(CHARACTERS.indexOf(character) >= 0);
        }
    }

    @Test
    void retriesWhenGeneratedCodeIsAlreadyTaken() {
        RoomRepository repository = new InMemoryRoomRepository();
        Random expectedRandom = new Random(42);

        String takenCode = generateExpectedCode(expectedRandom);
        String nextCode = generateExpectedCode(expectedRandom);

        repository.save(new Room(takenCode, "host", "Host"));

        RoomCodeGenerator generator =
                new RoomCodeGenerator(repository, new Random(42));

        String generatedCode = generator.generateCode();

        assertEquals(nextCode, generatedCode);
        assertNotEquals(takenCode, generatedCode);
    }

    private String generateExpectedCode(Random random) {
        StringBuilder code = new StringBuilder();

        for (int i = 0; i < 6; i++) {
            code.append(CHARACTERS.charAt(
                    random.nextInt(CHARACTERS.length())
            ));
        }

        return code.toString();
    }
}