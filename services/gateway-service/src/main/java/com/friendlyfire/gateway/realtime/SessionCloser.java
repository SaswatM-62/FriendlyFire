package com.friendlyfire.gateway.realtime;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;

/**
 * Ends WebSocket sessions with a STOMP ERROR frame whose message starts with an error
 * code (e.g. "ALREADY_CONNECTED: ..."); Spring closes the connection after sending it.
 * The frontend stops reconnecting on ALREADY_CONNECTED and ACCOUNT_DELETED.
 */
public class SessionCloser {

    private final ObjectProvider<MessageChannel> clientOutbound;
    private final SessionRegistry sessions;

    /**
     * @param clientOutbound Spring's clientOutboundChannel. A provider, because the broker
     *                       configuration that builds it needs ConnectInterceptor first.
     */
    public SessionCloser(ObjectProvider<MessageChannel> clientOutbound, SessionRegistry sessions) {
        this.clientOutbound = clientOutbound;
        this.sessions = sessions;
    }

    public void close(String sessionId, String code, String message) {
        StompHeaderAccessor error = StompHeaderAccessor.create(StompCommand.ERROR);
        error.setMessage(code + ": " + message);
        error.setSessionId(sessionId);
        clientOutbound.getObject().send(MessageBuilder.createMessage(new byte[0], error.getMessageHeaders()));
    }

    /** Closes the player's live connection on this Gateway, if they have one. */
    public void closeUser(String userId, String code, String message) {
        String sessionId = sessions.currentSessionOf(userId);
        if (sessionId != null) {
            close(sessionId, code, message);
        }
    }
}
