package com.friendlyfire.account.health;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.net.http.HttpTimeoutException;
import java.sql.SQLTransientConnectionException;
import java.util.Locale;
import org.springframework.web.client.RestClientResponseException;

/** Turns a failed dependency check into the short error shown in the health tree. */
final class HealthErrors {

    private static final int MAX_LENGTH = 120;

    private HealthErrors() {
    }

    /** E.g. "connection refused", "unknown host", "deadline exceeded", "HTTP 500". */
    static String describe(Throwable error) {
        for (Throwable cause = error; cause != null; cause = cause.getCause()) {
            if (cause instanceof StatusRuntimeException grpcError) {
                Status.Code code = grpcError.getStatus().getCode();
                if (code == Status.Code.DEADLINE_EXCEEDED) {
                    return "deadline exceeded";
                }
                if (code != Status.Code.UNAVAILABLE) {
                    return code.name().toLowerCase(Locale.ROOT).replace('_', ' ');
                }
                // UNAVAILABLE: the cause below usually says why.
            }
            if (cause instanceof ConnectException) {
                return "connection refused";
            }
            if (cause instanceof UnknownHostException) {
                return "unknown host";
            }
            if (cause instanceof SocketTimeoutException || cause instanceof HttpTimeoutException) {
                return "deadline exceeded";
            }
            if (cause instanceof RestClientResponseException httpError) {
                return "HTTP " + httpError.getStatusCode().value();
            }
        }
        if (error instanceof SQLTransientConnectionException) {
            // The pool timed out waiting for a connection, without saying why.
            return "no database connection";
        }
        if (error instanceof StatusRuntimeException grpcError) {
            String description = grpcError.getStatus().getDescription();
            return shorten("unavailable" + (description == null ? "" : ": " + description));
        }
        return shorten(error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
    }

    private static String shorten(String message) {
        return message.length() <= MAX_LENGTH ? message : message.substring(0, MAX_LENGTH - 3) + "...";
    }
}
