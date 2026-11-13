package com.friendlyfire.gateway.realtime;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The WebSocket sessions on this Gateway: who each one belongs to, which rooms it
 * watches, and each player's one live connection (FF-104, ADR-017: the newest connection
 * wins). Keyed by the STOMP session id.
 */
public class SessionRegistry {

    private final Map<String, Session> sessions = new ConcurrentHashMap<>();
    private final Map<String, String> currentSessionByUser = new ConcurrentHashMap<>();

    /** What a closed session left behind. {@code wasCurrent}: no newer connection replaced it. */
    public record Ended(String userId, boolean wasCurrent, Set<String> rooms) {
    }

    /**
     * Records a STOMP CONNECT. A signed-in player ({@code userId} not null) has one live
     * connection: this one becomes it, and the session it replaced (to be closed) is
     * returned, or null.
     */
    public String connect(String sessionId, String userId, String remoteIp) {
        Session session = session(sessionId);
        session.remoteIp = remoteIp;
        if (userId == null) {
            return null;
        }
        session.userId = userId;
        String previous = currentSessionByUser.put(userId, sessionId);
        return sessionId.equals(previous) ? null : previous;
    }

    /** The player's live connection on this Gateway, or null. */
    public String currentSessionOf(String userId) {
        return currentSessionByUser.get(userId);
    }

    /** The signed-in player of a session, or null (signed out, or unknown session). */
    public String userOf(String sessionId) {
        Session session = sessions.get(sessionId);
        return session == null ? null : session.userId;
    }

    /** The browser's IP address, from the WebSocket handshake; null if unknown. */
    public String remoteIp(String sessionId) {
        Session session = sessions.get(sessionId);
        return session == null ? null : session.remoteIp;
    }

    public void subscribed(String sessionId, String subscriptionId, String roomCode) {
        if (subscriptionId != null) {
            session(sessionId).roomBySubscription.put(subscriptionId, roomCode);
        }
    }

    /** The room the subscription was for, or null if it wasn't a room subscription. */
    public String unsubscribed(String sessionId, String subscriptionId) {
        Session session = sessions.get(sessionId);
        return session == null || subscriptionId == null ? null : session.roomBySubscription.remove(subscriptionId);
    }

    public boolean isSubscribedTo(String sessionId, String roomCode) {
        Session session = sessions.get(sessionId);
        return session != null && session.roomBySubscription.containsValue(roomCode);
    }

    /**
     * Forgets a closed session. Null if it was already forgotten (Spring may report a
     * session's end more than once).
     */
    public Ended end(String sessionId) {
        Session session = sessions.remove(sessionId);
        if (session == null) {
            return null;
        }
        String userId = session.userId;
        boolean wasCurrent = userId != null && currentSessionByUser.remove(userId, sessionId);
        return new Ended(userId, wasCurrent, Set.copyOf(session.roomBySubscription.values()));
    }

    private Session session(String sessionId) {
        return sessions.computeIfAbsent(sessionId, id -> new Session());
    }

    private static final class Session {

        private volatile String userId;
        private volatile String remoteIp;
        private final Map<String, String> roomBySubscription = new ConcurrentHashMap<>();
    }
}
