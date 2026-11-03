package com.friendlyfire.room.room;

import com.friendlyfire.room.room.Room;
import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RoomTest {
    @Test
    void concurrentJoinsNeverExceedEightPlayers() throws InterruptedException {
        Room room = new Room("ABC123", "host1", "Host");

        int numberOfPlayers = 20;
        AtomicInteger successfulJoins = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);

        ExecutorService executor = Executors.newFixedThreadPool(numberOfPlayers);

        for (int i = 0; i < numberOfPlayers; i++) {
            final int id = i;

            executor.submit(() -> {
                try {
                    start.await();

                    Player player = Player.newBuilder()
                            .setUserId("player" + id)
                            .setDisplayName("Player " + id)
                            .build();

                    if (room.addPlayer(player)) {
                        successfulJoins.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        start.countDown();
        executor.shutdown();

        while (!executor.isTerminated()) {
            Thread.yield();
        }

        assertEquals(7, successfulJoins.get());
        assertEquals(8, room.getPlayers().size());
    }

    @Test
    void correctAnswerIsHiddenBeforeReveal() {
        Room room = new Room("ABC123", "host1", "Host");

        Question question = Question.newBuilder()
                .setQuestionId("q1")
                .setText("Which option is correct?")
                .setCorrectOptionId("A")
                .build();

        room.setCurrentQuestion(question);

        var snapshot = room.buildSnapshot();

        assertFalse(snapshot.getCurrentQuestion().hasReveal());
    }

    @Test
    void correctAnswerIsVisibleDuringReveal() {
        Room room = new Room("ABC123", "host1", "Host");

        Question question = Question.newBuilder()
                .setQuestionId("q1")
                .setText("Which option is correct?")
                .setCorrectOptionId("A")
                .build();

        room.setCurrentQuestion(question);
        room.setPhase(RoomPhase.ROOM_PHASE_REVEAL);

        var snapshot = room.buildSnapshot();

        assertTrue(snapshot.getCurrentQuestion().hasReveal());
        assertEquals(
                "A",
                snapshot.getCurrentQuestion().getReveal().getCorrectOptionId()
        );
    }
}