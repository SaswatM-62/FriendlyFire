package com.friendlyfire.gateway.realtime;

import io.grpc.StatusRuntimeException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.stereotype.Controller;

@Controller
public class StompExceptionHandler {

    @MessageExceptionHandler(StatusRuntimeException.class)
    public void handleGrpcError(StatusRuntimeException ex) {
        // TODO(ff-006):
        // Send structured errors to:
        // /user/queue/errors
    }
}
