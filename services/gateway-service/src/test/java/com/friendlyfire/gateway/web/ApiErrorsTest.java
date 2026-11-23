package com.friendlyfire.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;

import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;

class ApiErrorsTest {

    @ParameterizedTest(name = "{0} -> {1}")
    @CsvSource({
        "NOT_FOUND, 404",
        "PERMISSION_DENIED, 403",
        "INVALID_ARGUMENT, 400",
        "ALREADY_EXISTS, 409",
        "FAILED_PRECONDITION, 409",
        "RESOURCE_EXHAUSTED, 409",
        "UNAVAILABLE, 503",
        "DEADLINE_EXCEEDED, 503",
        "UNIMPLEMENTED, 501",
        "INTERNAL, 500",
        "UNKNOWN, 500",
    })
    void mapsEachGrpcCodeToItsHttpStatus(Status.Code code, int httpStatus) {
        assertThat(ApiErrors.httpStatus(code).value()).isEqualTo(httpStatus);
    }

    @Test
    void clientErrorKeepsTheServicesMessageForThePlayer() {
        var body = ApiErrors.fromGrpc(
                Status.FAILED_PRECONDITION.withDescription("Maya is still syncing.").asRuntimeException());

        assertThat(body.error()).isEqualTo(new ApiError("FAILED_PRECONDITION", "Maya is still syncing."));
    }

    @Test
    void clientErrorWithoutAMessageGetsAGenericOne() {
        var body = ApiErrors.fromGrpc(Status.NOT_FOUND.asRuntimeException());

        assertThat(body.error()).isEqualTo(new ApiError("NOT_FOUND", "Not found."));
    }

    @ParameterizedTest
    @EnumSource(value = Status.Code.class, names = {"UNAVAILABLE", "DEADLINE_EXCEEDED"})
    void unavailableServiceNeverShowsItsMessage(Status.Code code) {
        var body = ApiErrors.fromGrpc(
                Status.fromCode(code).withDescription("io exception: room-service:9092").asRuntimeException());

        assertThat(body.error()).isEqualTo(new ApiError(code.name(), ApiErrors.UNAVAILABLE_MESSAGE));
    }

    @Test
    void unexpectedServerErrorIsInternalWithoutDetails() {
        var body = ApiErrors.fromGrpc(
                Status.INTERNAL.withDescription("NullPointerException at RoomGrpcService.java:42").asRuntimeException());

        assertThat(body.error()).isEqualTo(new ApiError("INTERNAL", ApiErrors.INTERNAL_MESSAGE));
    }

    @Test
    void rpcThatIsNotBuiltYetIsNotImplemented() {
        var body = ApiErrors.fromGrpc(Status.UNIMPLEMENTED.withDescription("not implemented yet").asRuntimeException());

        assertThat(body.error()).isEqualTo(new ApiError("NOT_IMPLEMENTED", ApiErrors.NOT_IMPLEMENTED_MESSAGE));
    }
}
