package com.friendlyfire.gateway.realtime;

import com.friendlyfire.gateway.security.AuthSettings;
import com.friendlyfire.gateway.security.SignInRules;
import com.friendlyfire.gateway.security.UserResolver;
import com.friendlyfire.gateway.web.ApiError;
import com.friendlyfire.gateway.web.ApiErrors;
import com.friendlyfire.gateway.web.ApiException;
import com.friendlyfire.gateway.web.ErrorCodes;
import com.friendlyfire.gateway.web.RoomCodes;
import com.friendlyfire.proto.friendlyfire.account.v1.User;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

/**
 * Guards every frame a browser sends over the WebSocket (FF-104, FF-305).
 *
 * <ul>
 *   <li>CONNECT: checks the Auth0 token in the Authorization header and remembers the
 *       player on the session (handlers read the user_id from there, never from a body).
 *       The token is checked only here: a live connection stays open after it expires
 *       (ADR-022). One live connection per player, and the newest wins (ADR-017): the one
 *       it replaces gets ERROR ALREADY_CONNECTED. Outside production a connection without
 *       a token is allowed, but it may only ping (the status page). A refused CONNECT gets
 *       a STOMP ERROR frame whose message starts with the error code, e.g.
 *       EMAIL_NOT_VERIFIED, and the connection closes.</li>
 *   <li>SUBSCRIBE: /user/queue/pong and /user/queue/errors for anyone;
 *       /topic/room/{code} only for players in that room (Room says), which also starts
 *       relaying that room's events here. Nothing else.</li>
 *   <li>SEND: /app/ping for anyone; /app/room/{code}/... only from a session watching
 *       that room; anything else only when signed in.</li>
 * </ul>
 * A refused SUBSCRIBE or SEND is dropped, and the player gets the error on
 * /user/queue/errors.
 */
public class ConnectInterceptor implements ChannelInterceptor {

    private static final Set<String> OPEN_QUEUES = Set.of("/user/queue/pong", "/user/queue/errors");
    private static final String PING = "/app/ping";
    /** The WebSocket session attribute WebSocketConfig keeps the browser's IP address in. */
    public static final String REMOTE_IP_ATTRIBUTE = "friendlyfire.remoteIp";

    private final AuthSettings settings;
    private final JwtDecoder tokens;
    private final SignInRules rules;
    private final UserResolver users;
    private final RoomServiceBlockingStub rooms;
    private final SessionRegistry sessions;
    private final RoomEventRelay relay;
    private final RealtimeErrors errors;
    private final SessionCloser closer;

    public ConnectInterceptor(
            AuthSettings settings, JwtDecoder tokens, SignInRules rules, UserResolver users,
            RoomServiceBlockingStub rooms, SessionRegistry sessions, RoomEventRelay relay, RealtimeErrors errors,
            SessionCloser closer) {
        this.settings = settings;
        this.tokens = tokens;
        this.rules = rules;
        this.users = users;
        this.rooms = rooms;
        this.sessions = sessions;
        this.relay = relay;
        this.errors = errors;
        this.closer = closer;
    }

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getSessionId() == null) {
            return message;
        }
        StompCommand command = accessor.getCommand();
        if (command == null) {
            return message;
        }
        return switch (command) {
            case CONNECT, STOMP -> connect(accessor) ? message : null;
            case SUBSCRIBE -> subscribe(accessor) ? message : null;
            case UNSUBSCRIBE -> {
                unsubscribe(accessor);
                yield message;
            }
            case SEND -> send(accessor) ? message : null;
            default -> message;
        };
    }

    /** False if the CONNECT is refused (the ERROR frame is already on its way). */
    private boolean connect(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String remoteIp = remoteIp(accessor);
        String token = bearerToken(accessor.getFirstNativeHeader("Authorization"));
        if (token == null) {
            if (settings.production()) {
                return refuseConnect(sessionId, ErrorCodes.UNAUTHENTICATED, "Sign in first.");
            }
            sessions.connect(sessionId, null, remoteIp);
            return true;
        }
        Jwt jwt;
        try {
            jwt = tokens.decode(token);
        } catch (JwtException e) {
            return refuseConnect(sessionId, ErrorCodes.UNAUTHENTICATED, "Your sign-in has expired. Sign in again.");
        }
        User user;
        try {
            rules.check(jwt);
            user = users.resolve(jwt);
        } catch (ApiException e) {
            return refuseConnect(sessionId, e.code(), e.getMessage());
        } catch (StatusRuntimeException e) {
            ApiError error = ApiErrors.fromGrpc(e).error();
            return refuseConnect(sessionId, error.code(), error.message());
        }
        String replaced = sessions.connect(sessionId, user.getUserId(), remoteIp);
        if (replaced != null) {
            closer.close(replaced, ErrorCodes.ALREADY_CONNECTED, "You opened FriendlyFire in another tab or on another device.");
        }
        accessor.setUser(new GatewayPrincipal(user.getUserId()));
        return true;
    }

    private boolean subscribe(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        if (OPEN_QUEUES.contains(destination)) {
            return true;
        }
        String userId = sessions.userOf(sessionId);
        String roomCode = RoomDestinations.topicRoom(destination);
        if (roomCode == null) {
            return refuse(accessor, userId, ApiErrors.forHttpStatus(HttpStatus.FORBIDDEN).error());
        }
        if (userId == null) {
            return refuse(accessor, null, ApiErrors.unauthenticated().error());
        }
        if (!RoomCodes.isValid(roomCode)) {
            return refuse(accessor, userId, RoomCodes.notFound().body().error());
        }
        try {
            rooms.getRoomSnapshot(GetRoomSnapshotRequest.newBuilder().setRoomCode(roomCode).setUserId(userId).build());
        } catch (StatusRuntimeException e) {
            return refuse(accessor, userId, membershipError(e));
        }
        sessions.subscribed(sessionId, accessor.getSubscriptionId(), roomCode);
        relay.watch(roomCode, sessionId);
        return true;
    }

    private void unsubscribe(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String roomCode = sessions.unsubscribed(sessionId, accessor.getSubscriptionId());
        if (roomCode != null && !sessions.isSubscribedTo(sessionId, roomCode)) {
            relay.unwatch(roomCode, sessionId);
        }
    }

    private boolean send(StompHeaderAccessor accessor) {
        String sessionId = accessor.getSessionId();
        String destination = accessor.getDestination();
        if (PING.equals(destination)) {
            return true;
        }
        String userId = sessions.userOf(sessionId);
        if (userId == null) {
            return refuse(accessor, null, ApiErrors.unauthenticated().error());
        }
        String roomCode = RoomDestinations.appRoom(destination);
        if (roomCode != null && !sessions.isSubscribedTo(sessionId, roomCode)) {
            return refuse(accessor, userId, new ApiError(ErrorCodes.NOT_IN_ROOM, "You're not in this room."));
        }
        return true;
    }

    private static ApiError membershipError(StatusRuntimeException e) {
        return switch (e.getStatus().getCode()) {
            case NOT_FOUND -> RoomCodes.notFound().body().error();
            case PERMISSION_DENIED -> new ApiError(ErrorCodes.NOT_IN_ROOM, "You're not in this room.");
            default -> ApiErrors.fromGrpc(e).error();
        };
    }

    private boolean refuse(StompHeaderAccessor accessor, String userId, ApiError error) {
        errors.send(accessor.getSessionId(), userId, accessor.getDestination(), error);
        return false;
    }

    /**
     * Closes the session with an ERROR frame. Throwing from here wouldn't reach the browser:
     * with ordered receiving, Spring only logs it.
     */
    private boolean refuseConnect(String sessionId, String code, String message) {
        closer.close(sessionId, code, message);
        return false;
    }

    private static String bearerToken(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String token = header.substring(7).strip();
        return token.isEmpty() ? null : token;
    }

    private static String remoteIp(StompHeaderAccessor accessor) {
        Map<String, Object> attributes = accessor.getSessionAttributes();
        return attributes == null ? null : (String) attributes.get(REMOTE_IP_ATTRIBUTE);
    }
}
