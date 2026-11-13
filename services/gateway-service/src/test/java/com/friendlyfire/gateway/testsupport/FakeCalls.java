package com.friendlyfire.gateway.testsupport;

import com.google.protobuf.Message;
import io.grpc.Context;
import io.grpc.Deadline;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import io.grpc.stub.StreamObserver;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * What a fake gRPC service received, and how it answers: a default answer per RPC, which a
 * test can replace with its own answer or a failure.
 */
public final class FakeCalls {

    private final List<Message> received = new CopyOnWriteArrayList<>();
    private final Map<Class<?>, Function<Message, Message>> answers = new ConcurrentHashMap<>();
    private final Map<Class<?>, Long> deadlineMillis = new ConcurrentHashMap<>();

    /** Answers requests of this type with {@code answer} (which may throw a StatusRuntimeException). */
    @SuppressWarnings("unchecked")
    public <Q extends Message, R extends Message> void answer(Class<Q> request, Function<Q, R> answer) {
        answers.put(request, message -> answer.apply((Q) message));
    }

    /** Fails requests of this type with {@code status}. */
    public void fail(Class<? extends Message> request, Status status) {
        answers.put(request, message -> {
            throw status.asRuntimeException();
        });
    }

    /** Every request of this type received so far, oldest first. */
    public <Q extends Message> List<Q> received(Class<Q> type) {
        return received.stream().filter(type::isInstance).map(type::cast).toList();
    }

    /** The last request of this type, or null. */
    public <Q extends Message> Q last(Class<Q> type) {
        List<Q> all = received(type);
        return all.isEmpty() ? null : all.getLast();
    }

    /** How long the caller gave the last request of this type, in ms (null: no deadline). */
    public Long deadlineMillis(Class<? extends Message> type) {
        return deadlineMillis.get(type);
    }

    void record(Message request) {
        received.add(request);
        Deadline deadline = Context.current().getDeadline();
        if (deadline != null) {
            deadlineMillis.put(request.getClass(), deadline.timeRemaining(TimeUnit.MILLISECONDS));
        }
    }

    @SuppressWarnings("unchecked")
    <R extends Message> void handle(Message request, R defaultAnswer, StreamObserver<R> observer) {
        record(request);
        Function<Message, Message> answer = answers.get(request.getClass());
        try {
            observer.onNext(answer == null ? defaultAnswer : (R) answer.apply(request));
            observer.onCompleted();
        } catch (StatusRuntimeException e) {
            observer.onError(e);
        }
    }

    void reset() {
        received.clear();
        answers.clear();
        deadlineMillis.clear();
    }
}
