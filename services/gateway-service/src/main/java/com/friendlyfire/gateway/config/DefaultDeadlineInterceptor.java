package com.friendlyfire.gateway.config;

import io.grpc.CallOptions;
import io.grpc.Channel;
import io.grpc.ClientCall;
import io.grpc.ClientInterceptor;
import io.grpc.MethodDescriptor;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

/**
 * Gives every gRPC call a deadline unless the call already set one. The deadline is set
 * per call, so it counts from when the call starts (a deadline set on the stub itself
 * would count from when the stub was created and expire for every later call).
 */
final class DefaultDeadlineInterceptor implements ClientInterceptor {

    private final Duration timeout;

    DefaultDeadlineInterceptor(Duration timeout) {
        this.timeout = timeout;
    }

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method, CallOptions callOptions, Channel next) {
        CallOptions options = callOptions.getDeadline() == null
                ? callOptions.withDeadlineAfter(timeout.toNanos(), TimeUnit.NANOSECONDS)
                : callOptions;
        return next.newCall(method, options);
    }
}
