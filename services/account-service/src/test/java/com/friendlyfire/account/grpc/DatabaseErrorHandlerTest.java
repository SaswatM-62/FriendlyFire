package com.friendlyfire.account.grpc;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.StatusException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.jdbc.CannotGetJdbcConnectionException;

class DatabaseErrorHandlerTest {

    private final DatabaseErrorHandler handler = new DatabaseErrorHandler();

    @Test
    void cantReachTheDatabaseIsUnavailable() {
        StatusException status = handler.handleException(new CannotGetJdbcConnectionException("down"));

        assertThat(status).isNotNull();
        assertThat(status.getStatus().getCode()).isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void aTimeoutOrADroppedConnectionIsUnavailable() {
        assertThat(handler.handleException(new QueryTimeoutException("slow")).getStatus().getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
        assertThat(handler.handleException(new RecoverableDataAccessException("dropped")).getStatus().getCode())
                .isEqualTo(Status.Code.UNAVAILABLE);
    }

    @Test
    void otherErrorsAreLeftAlone() {
        // A broken constraint or a plain bug isn't fixed by trying again: it stays UNKNOWN.
        assertThat(handler.handleException(new DataIntegrityViolationException("duplicate"))).isNull();
        assertThat(handler.handleException(new IllegalStateException("bug"))).isNull();
    }
}
