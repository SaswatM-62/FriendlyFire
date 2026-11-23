package com.friendlyfire.gateway.web;

import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;

/**
 * Room codes: 6 characters without look-alikes (no 0/O, 1/I/L), the same alphabet as the
 * frontend's normalizeRoomCode. Anything else is refused before it reaches room-service
 * or a STOMP destination.
 */
public final class RoomCodes {

    private static final Pattern CODE = Pattern.compile("[ABCDEFGHJKMNPQRSTUVWXYZ23456789]{6}");

    private RoomCodes() {
    }

    public static boolean isValid(String code) {
        return code != null && CODE.matcher(code).matches();
    }

    /** The code in upper case, or 404 ROOM_NOT_FOUND if it can't be a room code. */
    public static String require(String raw) {
        String code = raw == null ? "" : raw.strip().toUpperCase(Locale.ROOT);
        if (!isValid(code)) {
            throw notFound();
        }
        return code;
    }

    public static ApiException notFound() {
        return new ApiException(HttpStatus.NOT_FOUND, ErrorCodes.ROOM_NOT_FOUND, "Room not found. Check the code and try again.");
    }
}
