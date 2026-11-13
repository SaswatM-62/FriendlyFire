package com.friendlyfire.gateway.web;

/**
 * The error codes the frontend reacts to by name (ERROR_CODES in frontend/src/lib/errors.ts).
 * Every other error's code is the gRPC status name (NOT_FOUND, FAILED_PRECONDITION...).
 */
public final class ErrorCodes {

    /** No valid sign-in. */
    public static final String UNAUTHENTICATED = "UNAUTHENTICATED";
    /** Email/password accounts must verify their email first (FF-103). */
    public static final String EMAIL_NOT_VERIFIED = "EMAIL_NOT_VERIFIED";
    /** Production lets only invited testers in until Riot approves us (FF-618). */
    public static final String INVITE_ONLY = "INVITE_ONLY";
    /** One live WebSocket per player: a newer connection replaced this one (FF-104, ADR-017). */
    public static final String ALREADY_CONNECTED = "ALREADY_CONNECTED";
    /** The player deleted their account, so their WebSocket was closed (ADR-022). */
    public static final String ACCOUNT_DELETED = "ACCOUNT_DELETED";
    public static final String ROOM_NOT_FOUND = "ROOM_NOT_FOUND";
    public static final String ROOM_FULL = "ROOM_FULL";
    public static final String QUIZ_ALREADY_STARTED = "QUIZ_ALREADY_STARTED";
    public static final String NOT_IN_ROOM = "NOT_IN_ROOM";
    public static final String RIOT_ALREADY_LINKED = "RIOT_ALREADY_LINKED";
    public static final String INVALID_LINK_STATE = "INVALID_LINK_STATE";
    /** Too many of one action in a short time (FF-601). */
    public static final String RATE_LIMITED = "RATE_LIMITED";

    private ErrorCodes() {
    }
}
