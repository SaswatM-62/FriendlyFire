package com.friendlyfire.gateway.web;

import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Gives some gRPC errors of one call the codes the frontend reacts to, e.g. NOT_FOUND from
 * JoinRoom becomes ROOM_NOT_FOUND. The HTTP status still comes from ApiErrors, and the
 * service's description stays the message. Errors not in the map propagate unchanged.
 */
public final class GrpcErrors {

    private GrpcErrors() {
    }

    public static <T> T renaming(Map<Status.Code, String> codes, Supplier<T> call) {
        try {
            return call.get();
        } catch (StatusRuntimeException e) {
            String code = codes.get(e.getStatus().getCode());
            if (code == null) {
                throw e;
            }
            ErrorResponse standard = ApiErrors.fromGrpc(e);
            throw new ApiException(ApiErrors.httpStatus(e.getStatus().getCode()), code, standard.error().message());
        }
    }
}
