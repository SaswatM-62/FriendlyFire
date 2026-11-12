package com.friendlyfire.room.room;


import java.util.random.RandomGenerator;

import org.springframework.stereotype.Component;

@Component
public class RoomCodeGenerator {

    private static final String CHARACTERS = "ABCDEFGHJKMNPQRTUVWXY346789";
    private static final int CODE_LENGTH = 6;

    private final RoomRepository repository;
    private final RandomGenerator random;

    public RoomCodeGenerator(RoomRepository repository, RandomGenerator random) {
        this.repository = repository;
        this.random = random;
    }

    public String generateCode() {
        while (true) {
            StringBuilder code = new StringBuilder();

            for (int i = 0; i < CODE_LENGTH; i++) {
                int index = random.nextInt(CHARACTERS.length());
                code.append(CHARACTERS.charAt(index));
            }

            String roomCode = code.toString();

            if (repository.findByCode(roomCode).isEmpty()) {
                return roomCode;
            }
        }
    }
}