package com.friendlyfire.gateway.json;

import com.google.protobuf.ProtocolMessageEnum;
import com.google.protobuf.Timestamp;
import java.time.Instant;

/**
 * The conventions every JSON body follows (docs/realtime-messages.md): enum values drop
 * their proto prefix (ROOM_PHASE_LOBBY -> "LOBBY"), timestamps are ISO-8601 strings in
 * UTC, and a field left at its proto default (empty string, unset message, UNSPECIFIED)
 * is left out.
 */
final class ProtoJson {

    private ProtoJson() {
    }

    /** "2026-11-09T18:20:05.120Z", or null if the timestamp isn't set. */
    static String timestamp(boolean isSet, Timestamp timestamp) {
        if (!isSet) {
            return null;
        }
        return Instant.ofEpochSecond(timestamp.getSeconds(), timestamp.getNanos()).toString();
    }

    /** The enum value without {@code prefix}, or null for UNSPECIFIED and unknown values. */
    static String enumName(ProtocolMessageEnum value, String prefix) {
        if (!(value instanceof Enum<?> constant)) {
            return null;
        }
        String name = constant.name();
        if (name.equals("UNRECOGNIZED") || name.endsWith("_UNSPECIFIED")) {
            return null;
        }
        return name.startsWith(prefix) ? name.substring(prefix.length()) : name;
    }

    /** null instead of an empty string. */
    static String text(String value) {
        return value == null || value.isEmpty() ? null : value;
    }
}
