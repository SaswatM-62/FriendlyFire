package com.friendlyfire.gateway.web;

public record ApiError(
        String code,
        String message
) {
}
