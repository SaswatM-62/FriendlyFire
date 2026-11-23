package com.friendlyfire.room.room;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;

/** The contract's errors, with messages safe to show the player. */
final class RoomErrors {

    private RoomErrors() {
    }

    static StatusRuntimeException notFound() {
        return Status.NOT_FOUND.withDescription("Room not found.").asRuntimeException();
    }

    static StatusRuntimeException notInRoom() {
        return Status.PERMISSION_DENIED.withDescription("You're not in this room.").asRuntimeException();
    }

    static StatusRuntimeException notHost() {
        return Status.PERMISSION_DENIED.withDescription("Only the host can do that.").asRuntimeException();
    }

    static StatusRuntimeException precondition(String message) {
        return Status.FAILED_PRECONDITION.withDescription(message).asRuntimeException();
    }

    static StatusRuntimeException invalid(String message) {
        return Status.INVALID_ARGUMENT.withDescription(message).asRuntimeException();
    }

    static StatusRuntimeException full() {
        return Status.RESOURCE_EXHAUSTED.withDescription("This room is full.").asRuntimeException();
    }

    static StatusRuntimeException alreadyExists(String message) {
        return Status.ALREADY_EXISTS.withDescription(message).asRuntimeException();
    }

    static StatusRuntimeException unavailable(String message) {
        return Status.UNAVAILABLE.withDescription(message).asRuntimeException();
    }
}
