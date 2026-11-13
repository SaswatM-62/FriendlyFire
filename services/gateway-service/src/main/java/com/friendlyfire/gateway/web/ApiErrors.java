package com.friendlyfire.gateway.web;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

/**
 * What browsers see when something fails (FF-006), for REST (GlobalExceptionHandler) and
 * STOMP (StompExceptionHandler) alike: {"error": {"code": "...", "message": "..."}}.
 * The message is shown to the player, so it never contains stack traces, hosts or
 * service names.
 */
public final class ApiErrors {

    static final String INTERNAL_MESSAGE = "Something went wrong. Try again in a moment.";
    static final String UNAVAILABLE_MESSAGE = "FriendlyFire is having trouble right now. Try again in a moment.";
    static final String NOT_IMPLEMENTED_MESSAGE = "That part of FriendlyFire isn't ready yet.";

    private static final Map<HttpStatus, String> CLIENT_ERROR_MESSAGES = Map.of(
            HttpStatus.BAD_REQUEST, "That request isn't valid.",
            HttpStatus.FORBIDDEN, "You can't do that.",
            HttpStatus.NOT_FOUND, "Not found.",
            HttpStatus.CONFLICT, "That can't be done right now.");

    private ApiErrors() {
    }

    /** The HTTP status for a gRPC status code. */
    public static HttpStatus httpStatus(Status.Code code) {
        return switch (code) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS, FAILED_PRECONDITION, RESOURCE_EXHAUSTED -> HttpStatus.CONFLICT;
            case UNAVAILABLE, DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
            // Every RPC that isn't built yet answers UNIMPLEMENTED; the frontend knows this code.
            case UNIMPLEMENTED -> HttpStatus.NOT_IMPLEMENTED;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }

    /**
     * The body for a failed gRPC call. A 4xx keeps the service's description, which our
     * services write for players (e.g. "Maya is still syncing."). A 5xx never does: its
     * description can name hosts and services ("io exception: room-service:9092").
     */
    public static ErrorResponse fromGrpc(StatusRuntimeException error) {
        Status status = error.getStatus();
        HttpStatus httpStatus = httpStatus(status.getCode());
        if (httpStatus == HttpStatus.SERVICE_UNAVAILABLE) {
            return of(status.getCode().name(), UNAVAILABLE_MESSAGE);
        }
        if (httpStatus.is5xxServerError()) {
            return forHttpStatus(httpStatus);
        }
        String description = status.getDescription();
        String message = description == null || description.isBlank() ? clientErrorMessage(httpStatus) : description;
        return of(status.getCode().name(), message);
    }

    /** The body for an error that has only an HTTP status (Spring's own errors, 501 stubs). */
    public static ErrorResponse forHttpStatus(HttpStatusCode statusCode) {
        HttpStatus status = HttpStatus.resolve(statusCode.value());
        if (status == HttpStatus.NOT_IMPLEMENTED) {
            return of("NOT_IMPLEMENTED", NOT_IMPLEMENTED_MESSAGE);
        }
        if (status == HttpStatus.SERVICE_UNAVAILABLE) {
            return of("UNAVAILABLE", UNAVAILABLE_MESSAGE);
        }
        if (status == null || status.is5xxServerError()) {
            return internal();
        }
        return of(status.name(), clientErrorMessage(status));
    }

    /** The body for an unexpected failure. */
    public static ErrorResponse internal() {
        return of("INTERNAL", INTERNAL_MESSAGE);
    }

    private static String clientErrorMessage(HttpStatus status) {
        return CLIENT_ERROR_MESSAGES.getOrDefault(status, CLIENT_ERROR_MESSAGES.get(HttpStatus.BAD_REQUEST));
    }

    private static ErrorResponse of(String code, String message) {
        return new ErrorResponse(new ApiError(code, message));
    }
}
