package com.friendlyfire.room.testsupport;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.room.room.InMemoryRoomRepository;
import com.friendlyfire.room.room.QuizRunner;
import com.friendlyfire.room.room.RoomCodeGenerator;
import com.friendlyfire.room.room.RoomEventPublisher;
import com.friendlyfire.room.room.RoomJobs;
import com.friendlyfire.room.room.RoomManager;
import com.friendlyfire.room.timing.RoomTimings;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.time.Instant;
import java.util.Random;
import java.util.concurrent.Executor;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;

/**
 * RoomManager and QuizRunner wired with fakes: a manual clock and scheduler (time only
 * moves in {@link #advance}), and background work that runs right away.
 */
public final class RoomFixture {

    public static final Instant START = Instant.parse("2026-11-19T18:00:00Z");

    public final ManualClock clock = new ManualClock(START);
    public final FakeScheduler scheduler = new FakeScheduler(clock);
    public final FakeDataStatusClient data = new FakeDataStatusClient();
    public final FakeQuizClient quiz = new FakeQuizClient();
    public final FakeScoringClient scoring = new FakeScoringClient();
    public final RoomTimings timings = RoomTimings.defaults();
    public final InMemoryRoomRepository rooms = new InMemoryRoomRepository();
    public final RoomEventPublisher events = new RoomEventPublisher(clock);
    public final RoomManager manager;

    public RoomFixture() {
        this(Runnable::run);
    }

    public RoomFixture(Executor background) {
        QuizRunner runner = new QuizRunner(events, quiz, scoring, clock, scheduler, background, timings);
        manager = new RoomManager(rooms, new RoomCodeGenerator(rooms, new Random(7)), events, runner, data, clock,
                background, timings);
        new RoomJobs(manager, scheduler, timings);
    }

    /** A room hosted by {@code host}; returns its code. */
    public String create(String host) {
        return manager.createRoom(host, name(host), "").getRoomCode();
    }

    public RoomSnapshot join(String code, String user) {
        return manager.joinRoom(code, user, name(user), "");
    }

    public RoomSnapshot snapshot(String code) {
        return rooms.findByCode(code).orElseThrow().snapshot();
    }

    public RecordingStream stream(String code) {
        RecordingStream stream = new RecordingStream();
        manager.stream(code, stream);
        return stream;
    }

    public void advance(Duration duration) {
        scheduler.advance(duration);
    }

    public void advanceSeconds(long seconds) {
        advance(Duration.ofSeconds(seconds));
    }

    /** "alice" → "Alice". */
    public static String name(String user) {
        return Character.toUpperCase(user.charAt(0)) + user.substring(1);
    }

    /** Asserts the call fails with {@code code}; returns the message. */
    public static String fails(Status.Code code, ThrowingCallable call) {
        String[] message = new String[1];
        assertThatExceptionOfType(StatusRuntimeException.class)
                .isThrownBy(call)
                .satisfies(e -> {
                    org.assertj.core.api.Assertions.assertThat(e.getStatus().getCode()).isEqualTo(code);
                    message[0] = e.getStatus().getDescription();
                });
        return message[0];
    }
}
