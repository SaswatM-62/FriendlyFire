package com.friendlyfire.account.grpc;

import io.grpc.Status;
import io.grpc.StatusException;
import org.jspecify.annotations.Nullable;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.grpc.server.exception.GrpcExceptionHandler;
import org.springframework.stereotype.Component;

/**
 * Turns "can't reach the database" in any RPC into UNAVAILABLE, which the Gateway shows
 * as "try again" (503), instead of UNKNOWN (500). Other errors are bugs and stay UNKNOWN.
 */
@Component
public class DatabaseErrorHandler implements GrpcExceptionHandler {

    @Override
    public @Nullable StatusException handleException(Throwable exception) {
        if (exception instanceof DataAccessResourceFailureException
                || exception instanceof TransientDataAccessException
                || exception instanceof RecoverableDataAccessException) {
            return Status.UNAVAILABLE
                    .withDescription("the database is unavailable, try again")
                    .withCause(exception)
                    .asException();
        }
        return null;
    }
}
