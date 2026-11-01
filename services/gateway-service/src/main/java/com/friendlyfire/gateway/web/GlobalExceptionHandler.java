package com.friendlyfire.gateway.web;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcError(
            StatusRuntimeException ex) {

        HttpStatus status = mapStatus(ex.getStatus().getCode());

        ErrorResponse response =
                new ErrorResponse(
                        new ApiError(
                                ex.getStatus().getCode().name(),
                                "Request failed"
                        ));

        return ResponseEntity.status(status).body(response);
    }

    private HttpStatus mapStatus(Status.Code code) {
        return switch (code) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case PERMISSION_DENIED -> HttpStatus.FORBIDDEN;
            case INVALID_ARGUMENT -> HttpStatus.BAD_REQUEST;
            case ALREADY_EXISTS,
                 FAILED_PRECONDITION,
                 RESOURCE_EXHAUSTED -> HttpStatus.CONFLICT;
            case UNAVAILABLE,
                 DEADLINE_EXCEEDED -> HttpStatus.SERVICE_UNAVAILABLE;
            default -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
