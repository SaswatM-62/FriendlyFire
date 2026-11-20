package com.friendlyfire.room.room;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.quiz.v1.AnswerOption;
import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.room.room.Room.Member;
import com.friendlyfire.room.room.Room.Quiz;
import com.friendlyfire.room.testsupport.RoomFixture;
import io.grpc.StatusRuntimeException;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class RoomTest {

    private static final Question QUESTION = Question.newBuilder()
            .setQuestionId("q1")
            .setText("Who?")
            .addOptions(AnswerOption.newBuilder().setOptionId("a").setLabel("Alice"))
            .addOptions(AnswerOption.newBuilder().setOptionId("b").setLabel("Bob"))
            .setCorrectOptionId("b")
            .setExplanation("Bob, obviously.")
            .build();

    private static Room roomWithQuestion(RoomPhase phase) {
        Room room = new Room("ABC123", Instant.EPOCH);
        room.members.put("a", new Member("a", "Alice", ""));
        room.members.put("b", new Member("b", "Bob", ""));
        room.hostUserId = "a";
        room.quiz = new Quiz("quiz", List.of("a", "b"), Instant.EPOCH, Instant.EPOCH, 180);
        room.currentQuestion = QUESTION;
        room.phase = phase;
        return room;
    }

    @Test
    void concurrentJoinsNeverExceedEightPeople() throws InterruptedException {
        RoomFixture f = new RoomFixture();
        String code = f.create("host");
        AtomicInteger joined = new AtomicInteger();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService executor = Executors.newFixedThreadPool(20);
        for (int i = 0; i < 20; i++) {
            String user = "player" + i;
            executor.submit(() -> {
                try {
                    start.await();
                    f.join(code, user);
                    joined.incrementAndGet();
                } catch (StatusRuntimeException full) {
                    // RESOURCE_EXHAUSTED: the room filled up first.
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        }

        start.countDown();
        executor.shutdown();

        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(joined.get()).isEqualTo(7);
        assertThat(f.snapshot(code).getPlayersCount()).isEqualTo(8);
    }

    @Test
    void theAnswerIsHiddenWhileTheQuestionIsOpen() {
        var view = roomWithQuestion(RoomPhase.ROOM_PHASE_QUESTION).snapshot().getCurrentQuestion();

        assertThat(view.getQuestionId()).isEqualTo("q1");
        assertThat(view.getOptionsCount()).isEqualTo(2);
        assertThat(view.hasReveal()).isFalse();
    }

    @Test
    void theAnswerShowsDuringTheReveal() {
        var view = roomWithQuestion(RoomPhase.ROOM_PHASE_REVEAL).snapshot().getCurrentQuestion();

        assertThat(view.getReveal().getCorrectOptionId()).isEqualTo("b");
        assertThat(view.getReveal().getExplanation()).isEqualTo("Bob, obviously.");
    }

    @Test
    void noQuestionOutsideQuestionAndReveal() {
        for (RoomPhase phase : List.of(RoomPhase.ROOM_PHASE_STARTING, RoomPhase.ROOM_PHASE_VOTE,
                RoomPhase.ROOM_PHASE_FINISHED, RoomPhase.ROOM_PHASE_LOBBY)) {
            assertThat(roomWithQuestion(phase).snapshot().hasCurrentQuestion()).as(phase.name()).isFalse();
        }
    }

    @Test
    void theLobbyHasNoQuizFields() {
        Room room = new Room("ABC123", Instant.EPOCH);
        room.members.put("a", new Member("a", "Alice", ""));
        room.hostUserId = "a";

        var snapshot = room.snapshot();

        assertThat(snapshot.getQuizId()).isEmpty();
        assertThat(snapshot.getScoreboardCount()).isZero();
        assertThat(snapshot.getPauseSecondsLeft()).isZero();
        assertThat(snapshot.hasPhaseDeadline()).isFalse();
    }
}
