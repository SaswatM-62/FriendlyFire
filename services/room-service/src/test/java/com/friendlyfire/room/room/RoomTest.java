package com.friendlyfire.room.room;

import static org.assertj.core.api.Assertions.assertThat;
import com.friendlyfire.room.room.Room;
import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.Future;


import org.junit.jupiter.api.Test;


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

        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
            .isTrue();

        assertThat(successfulJoins.get()).isEqualTo(7);
        assertThat(room.getPlayers()).hasSize(8);
    }

    @Test
    void correctAnswerIsHiddenOutsideReveal() {
        Room room = new Room("ABC123", "host1", "Host");

        Question question = Question.newBuilder()
                .setQuestionId("q1")
                .setText("Which option is correct?")
                .setCorrectOptionId("A")
                .build();

        room.startQuestion(question, null);

        var snapshot = room.buildSnapshot();

        assertThat(snapshot.getPhase())
                .isEqualTo(RoomPhase.ROOM_PHASE_QUESTION);

        assertThat(snapshot.getCurrentQuestion().hasReveal())
                .isFalse();
    }


    @Test
    void correctAnswerIsVisibleDuringReveal() {
        Room room = new Room("ABC123", "host1", "Host");

        Question question = Question.newBuilder()
                .setQuestionId("q1")
                .setText("Which option is correct?")
                .setCorrectOptionId("A")
                .build();

        room.startQuestion(question, null);
        room.reveal(List.of(), null);

        var snapshot = room.buildSnapshot();

        assertThat(snapshot.getCurrentQuestion().hasReveal()).isTrue();

        assertThat(snapshot.getCurrentQuestion()
                .getReveal()
                .getCorrectOptionId()).isEqualTo("A");
    }

    @Test
    void concurrentSnapshotsNeverRevealNextAnswerEarly() throws Exception {
        Room room = new Room("ABC123", "host1", "Host");

        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> updater = executor.submit(() -> {
                for (int i = 0; i < 10000; i++) {
                    Question question = Question.newBuilder()
                            .setQuestionId("q" + i)
                            .setText("Question " + i)
                            .setCorrectOptionId("answer-" + i)
                            .build();

                    room.startQuestion(question, null);
                    room.reveal(java.util.List.of(), null);
                }
            });

            Future<?> reader = executor.submit(() -> {
                for (int i = 0; i < 10000; i++) {
                    var snapshot = room.buildSnapshot();

                    if (snapshot.hasCurrentQuestion()
                            && snapshot.getPhase() != RoomPhase.ROOM_PHASE_REVEAL) {
                        assertThat(snapshot.getCurrentQuestion().hasReveal())
                                .isFalse();
                    }
                }
            });

            updater.get();
            reader.get();

        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .isTrue();
        }
    }

    @Test
    void nextQuestionAnswerIsNeverRevealedEarly() throws Exception {
        Room room = new Room("ABC123", "host1", "Host");
        ExecutorService executor = Executors.newFixedThreadPool(2);

        try {
            Future<?> updater = executor.submit(() -> {
                for (int i = 0; i < 10000; i++) {
                    Question question = Question.newBuilder()
                            .setQuestionId("q" + i)
                            .setText("Question " + i)
                            .setCorrectOptionId("answer-" + i)
                            .build();

                    room.startQuestion(question, null);
                    room.reveal(java.util.List.of(), null);
                }
            });

            Future<?> reader = executor.submit(() -> {
                int highestRevealed = -1;

                for (int i = 0; i < 10000; i++) {
                    var snapshot = room.buildSnapshot();

                    if (!snapshot.hasCurrentQuestion()) {
                        continue;
                    }

                    String questionId =
                            snapshot.getCurrentQuestion().getQuestionId();

                    if (snapshot.getPhase() == RoomPhase.ROOM_PHASE_REVEAL) {
                        assertThat(snapshot.getCurrentQuestion().hasReveal())
                                .isTrue();
                        assertThat(snapshot.getCurrentQuestion()
                            .getReveal()
                            .getCorrectOptionId())
                            .isEqualTo("answer-" + questionId.substring(1));

                        int questionNumber =
                                Integer.parseInt(questionId.substring(1));

                        assertThat(questionNumber)
                                .isGreaterThanOrEqualTo(highestRevealed);

                        highestRevealed = questionNumber;
                    } else {
                        assertThat(snapshot.getCurrentQuestion().hasReveal())
                                .isFalse();
                    }
                }
            });

            updater.get();
            reader.get();

        } finally {
            executor.shutdown();
            assertThat(executor.awaitTermination(10, TimeUnit.SECONDS))
                    .isTrue();
        }
    }

    @Test
    void hostTransfersToNextPlayerWhenHostLeaves() {
        Room room = new Room("ABC123", "host1", "Host");

        Player first = Player.newBuilder()
                .setUserId("player1")
                .setDisplayName("Player 1")
                .build();

        Player second = Player.newBuilder()
                .setUserId("player2")
                .setDisplayName("Player 2")
                .build();

        room.addPlayer(first);
        room.addPlayer(second);

        assertThat(room.removePlayer("host1")).isTrue();
        assertThat(room.getHostUserId()).isEqualTo("player1");

        assertThat(room.removePlayer("player1")).isTrue();
        assertThat(room.getHostUserId()).isEqualTo("player2");
    }
}