package com.friendlyfire.gateway.web;

import org.springframework.http.HttpStatus;

/**
 * An error with our own code, for the cases the frontend reacts to by code (ROOM_FULL,
 * EMAIL_NOT_VERIFIED, RATE_LIMITED...). GlobalExceptionHandler answers REST calls with it
 * and StompExceptionHandler sends it to the player on /user/queue/errors, both in the
 * FF-006 format. The message is shown to the player.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public ApiException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus status() {
        return status;
    }

    public String code() {
        return code;
    }

    public ErrorResponse body() {
        return new ErrorResponse(new ApiError(code, getMessage()));
    }

    /** 401: no valid sign-in. */
    public static ApiException unauthenticated() {
        return new ApiException(HttpStatus.UNAUTHORIZED, ErrorCodes.UNAUTHENTICATED, ApiErrors.UNAUTHENTICATED_MESSAGE);
    }
}
