package com.friendlyfire.gateway.web;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** What every unfinished endpoint answers: 501 with the NOT_IMPLEMENTED error. */
final class NotImplemented {

    private NotImplemented() {
    }

    static ResponseEntity<ErrorResponse> response() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED)
                .body(ApiErrors.forHttpStatus(HttpStatus.NOT_IMPLEMENTED));
    }
}
