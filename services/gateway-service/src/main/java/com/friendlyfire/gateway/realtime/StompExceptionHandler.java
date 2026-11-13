package com.friendlyfire.gateway.realtime;

import com.friendlyfire.gateway.web.ApiError;
import com.friendlyfire.gateway.web.ApiErrors;
import com.friendlyfire.gateway.web.ApiException;
import io.grpc.StatusRuntimeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.converter.MessageConversionException;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.web.bind.annotation.ControllerAdvice;

/**
 * A STOMP action that fails is answered on /user/queue/errors, only to the session that
 * sent it, in the REST error format plus the destination that failed (FF-006, see
 * docs/realtime-messages.md). A @ControllerAdvice, so it covers every STOMP controller.
 */
@ControllerAdvice
public class StompExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(StompExceptionHandler.class);

    @MessageExceptionHandler(StatusRuntimeException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public RealtimeError handleGrpcError(
            StatusRuntimeException ex,
            @Header(SimpMessageHeaderAccessor.DESTINATION_HEADER) String destination) {
        if (ApiErrors.httpStatus(ex.getStatus().getCode()).is5xxServerError()) {
            log.warn("gRPC call for {} failed: {}", destination, ex.getStatus());
        }
        return new RealtimeError(ApiErrors.fromGrpc(ex).error(), destination);
    }

    @MessageExceptionHandler(ApiException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public RealtimeError handleApiError(
            ApiException ex, @Header(SimpMessageHeaderAccessor.DESTINATION_HEADER) String destination) {
        return new RealtimeError(ex.body().error(), destination);
    }

    /** A body that isn't the JSON the destination expects. */
    @MessageExceptionHandler(MessageConversionException.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public RealtimeError handleUnreadableBody(
            MessageConversionException ex, @Header(SimpMessageHeaderAccessor.DESTINATION_HEADER) String destination) {
        return new RealtimeError(new ApiError("INVALID_ARGUMENT", "That message isn't valid."), destination);
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser(destinations = "/queue/errors", broadcast = false)
    public RealtimeError handleUnexpected(
            Exception ex, @Header(SimpMessageHeaderAccessor.DESTINATION_HEADER) String destination) {
        log.error("STOMP action {} failed", destination, ex);
        return new RealtimeError(ApiErrors.internal().error(), destination);
    }

    /** {@code destination}: the /app destination that failed, e.g. "/app/room/K7QM2X/start". */
    public record RealtimeError(ApiError error, String destination) {
    }
}
