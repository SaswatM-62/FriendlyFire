package com.friendlyfire.gateway.web;

import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * Every REST error in one format (FF-006): {"error": {"code": "...", "message": "..."}}.
 * Failed gRPC calls map through ApiErrors, Spring's own errors (unknown path, bad JSON)
 * keep their status, and anything else is a 500 that never shows the exception.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(StatusRuntimeException.class)
    public ResponseEntity<ErrorResponse> handleGrpcError(StatusRuntimeException ex) {
        HttpStatus status = ApiErrors.httpStatus(ex.getStatus().getCode());
        if (status.is5xxServerError() && status != HttpStatus.NOT_IMPLEMENTED) {
            // The browser gets a generic message; the details stay in the log.
            log.warn("gRPC call failed: {}", ex.getStatus());
        }
        return ResponseEntity.status(status).body(ApiErrors.fromGrpc(ex));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
        log.error("Request failed", ex);
        return ResponseEntity.internalServerError().body(ApiErrors.internal());
    }

    /** Spring MVC's own errors (404 for an unknown path, 400 for bad JSON...) in our format. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(
            Object body, HttpHeaders headers, HttpStatusCode statusCode, WebRequest request) {
        return new ResponseEntity<>(ApiErrors.forHttpStatus(statusCode), headers, statusCode);
    }
}
