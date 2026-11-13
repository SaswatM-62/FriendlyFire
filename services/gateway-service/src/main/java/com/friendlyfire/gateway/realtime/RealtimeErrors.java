package com.friendlyfire.gateway.realtime;

import com.friendlyfire.gateway.realtime.StompExceptionHandler.RealtimeError;
import com.friendlyfire.gateway.web.ApiError;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessageType;
import org.springframework.messaging.simp.SimpMessagingTemplate;

/**
 * Sends one session an error on /user/queue/errors, for actions refused before they reach
 * a controller (ConnectInterceptor). Controllers' errors go through StompExceptionHandler.
 */
public class RealtimeErrors {

    // A provider: the messaging template is built by the broker configuration, which
    // itself needs ConnectInterceptor and therefore this class.
    private final ObjectProvider<SimpMessagingTemplate> messaging;

    public RealtimeErrors(ObjectProvider<SimpMessagingTemplate> messaging) {
        this.messaging = messaging;
    }

    /** {@code userId} may be null for a signed-out session: the session id then stands in for it. */
    public void send(String sessionId, String userId, String destination, ApiError error) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.create(SimpMessageType.MESSAGE);
        headers.setSessionId(sessionId);
        headers.setLeaveMutable(true);
        messaging.getObject().convertAndSendToUser(
                userId != null ? userId : sessionId, "/queue/errors", new RealtimeError(error, destination),
                headers.getMessageHeaders());
    }
}
