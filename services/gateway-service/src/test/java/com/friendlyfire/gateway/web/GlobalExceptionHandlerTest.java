package com.friendlyfire.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler =
            new GlobalExceptionHandler();

    void mapsNotFoundTo404() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.NOT_FOUND);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void mapsPermissionDeniedTo403() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.PERMISSION_DENIED);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void mapsInvalidArgumentTo400() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.INVALID_ARGUMENT);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void mapsAlreadyExistsTo409() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.ALREADY_EXISTS);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void mapsFailedPreconditionTo409() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.FAILED_PRECONDITION);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void mapsResourceExhaustedTo409() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.RESOURCE_EXHAUSTED);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void mapsUnavailableTo503() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.UNAVAILABLE);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void mapsDeadlineExceededTo503() {
        StatusRuntimeException ex =
                new StatusRuntimeException(Status.DEADLINE_EXCEEDED);

        ResponseEntity<ErrorResponse> response =
                handler.handleGrpcError(ex);

        assertThat(response.getStatusCode())
                .isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
    }
}
