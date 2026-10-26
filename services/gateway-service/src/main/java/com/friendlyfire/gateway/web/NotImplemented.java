package com.friendlyfire.gateway.web;

import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** What every unfinished endpoint answers: 501 Not Implemented. */
final class NotImplemented {

    private NotImplemented() {
    }

    static ResponseEntity<Map<String, String>> response() {
        return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(Map.of("error", "not implemented yet"));
    }
}
