package com.friendlyfire.gateway.realtime;

/** Reads the room code out of STOMP destinations. */
final class RoomDestinations {

    static final String TOPIC_PREFIX = "/topic/room/";
    static final String APP_PREFIX = "/app/room/";

    private RoomDestinations() {
    }

    static String topic(String roomCode) {
        return TOPIC_PREFIX + roomCode;
    }

    /** "K7QM2X" for "/topic/room/K7QM2X"; "" for another /topic/room/ destination; null otherwise. */
    static String topicRoom(String destination) {
        if (destination == null || !destination.startsWith(TOPIC_PREFIX)) {
            return null;
        }
        return destination.substring(TOPIC_PREFIX.length());
    }

    /** "K7QM2X" for "/app/room/K7QM2X/answer"; null for any destination outside /app/room/. */
    static String appRoom(String destination) {
        if (destination == null || !destination.startsWith(APP_PREFIX)) {
            return null;
        }
        String rest = destination.substring(APP_PREFIX.length());
        int slash = rest.indexOf('/');
        return slash < 0 ? rest : rest.substring(0, slash);
    }
}
