package com.friendlyfire.gateway.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import io.grpc.Status;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class RoomCodesTest {

    @Test
    void acceptsSixCharactersFromTheAlphabet() {
        assertThat(RoomCodes.isValid("K7QM2X")).isTrue();
        assertThat(RoomCodes.isValid("K7QM2")).isFalse();
        assertThat(RoomCodes.isValid("K7QM2O")).isFalse();
        assertThat(RoomCodes.isValid("k7qm2x")).isFalse();
        assertThat(RoomCodes.isValid(null)).isFalse();
    }

    @Test
    void requireUpperCasesOrRefusesWith404() {
        assertThat(RoomCodes.require(" k7qm2x ")).isEqualTo("K7QM2X");

        ApiException error = catchThrowableOfType(ApiException.class, () -> RoomCodes.require("../etc"));
        assertThat(error.status()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(error.code()).isEqualTo("ROOM_NOT_FOUND");
    }

    @Test
    void grpcErrorsCanBeRenamedPerCall() {
        ApiException error = catchThrowableOfType(ApiException.class, () -> GrpcErrors.renaming(
                Map.of(Status.Code.RESOURCE_EXHAUSTED, "ROOM_FULL"),
                () -> {
                    throw Status.RESOURCE_EXHAUSTED.withDescription("8 players already.").asRuntimeException();
                }));

        assertThat(error.status()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(error.code()).isEqualTo("ROOM_FULL");
        assertThat(error.getMessage()).isEqualTo("8 players already.");
    }

    @Test
    void errorsNotRenamedPassThrough() {
        Throwable error = catchThrowableOfType(io.grpc.StatusRuntimeException.class, () -> GrpcErrors.renaming(
                Map.of(Status.Code.RESOURCE_EXHAUSTED, "ROOM_FULL"),
                () -> {
                    throw Status.UNAVAILABLE.asRuntimeException();
                }));

        assertThat(error).isNotNull();
    }
}
