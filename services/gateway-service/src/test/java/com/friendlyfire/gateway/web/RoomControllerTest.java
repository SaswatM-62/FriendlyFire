package com.friendlyfire.gateway.web;

import static com.friendlyfire.gateway.testsupport.FakeServices.ROOM;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.friendlyfire.gateway.testsupport.FakeTokens;
import com.friendlyfire.gateway.testsupport.GatewayTestBase;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest;
import io.grpc.Status;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MockMvc;

class RoomControllerTest extends GatewayTestBase {

    @Autowired
    private MockMvc mvc;

    @Test
    void createsARoomWithThePlayerAsHost() throws Exception {
        mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer("riley")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.roomCode").value("K7QM2X"))
                .andExpect(jsonPath("$.hostUserId").value("user-riley"))
                .andExpect(jsonPath("$.phase").value("LOBBY"))
                .andExpect(jsonPath("$.settings.questionCount").value(10))
                .andExpect(jsonPath("$.settings.secondsPerQuestion").value(20))
                .andExpect(jsonPath("$.players[0].displayName").value("Riley"))
                .andExpect(jsonPath("$.players[0].status").value("CONNECTED"))
                .andExpect(jsonPath("$.players[0].matchDataReady").value(false))
                .andExpect(jsonPath("$.roundNumber").value(0))
                .andExpect(jsonPath("$.scoreboard").isEmpty())
                .andExpect(jsonPath("$.quizId").doesNotExist())
                .andExpect(jsonPath("$.phaseDeadline").doesNotExist());

        CreateRoomRequest request = ROOM.calls.last(CreateRoomRequest.class);
        assertThat(request.getHostUserId()).isEqualTo("user-riley");
        assertThat(request.getHostDisplayName()).isEqualTo("Riley");
    }

    @Test
    void joinsWithTheSignedInPlayersIdAndName() throws Exception {
        mvc.perform(post("/api/rooms/P4CWRT/join").header(HttpHeaders.AUTHORIZATION, bearer("riley")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("P4CWRT"))
                .andExpect(jsonPath("$.players[1].userId").value("user-riley"));

        JoinRoomRequest request = ROOM.calls.last(JoinRoomRequest.class);
        assertThat(request.getRoomCode()).isEqualTo("P4CWRT");
        assertThat(request.getUserId()).isEqualTo("user-riley");
        assertThat(request.getDisplayName()).isEqualTo("Riley");
    }

    @Test
    void lowerCaseCodesWork() throws Exception {
        mvc.perform(post("/api/rooms/p4cwrt/join").header(HttpHeaders.AUTHORIZATION, bearer("riley")))
                .andExpect(status().isOk());

        assertThat(ROOM.calls.last(JoinRoomRequest.class).getRoomCode()).isEqualTo("P4CWRT");
    }

    @Test
    void somethingThatCantBeARoomCodeIs404WithoutAskingRoom() throws Exception {
        mvc.perform(post("/api/rooms/NOPE!!/join").header(HttpHeaders.AUTHORIZATION, bearer("riley")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error.code").value("ROOM_NOT_FOUND"));

        assertThat(ROOM.calls.received(JoinRoomRequest.class)).isEmpty();
    }

    @Test
    void joinErrorsGetTheCodesTheFrontendKnows() throws Exception {
        expectJoinError(Status.NOT_FOUND, 404, "ROOM_NOT_FOUND");
        expectJoinError(Status.RESOURCE_EXHAUSTED, 409, "ROOM_FULL");
        expectJoinError(Status.FAILED_PRECONDITION, 409, "QUIZ_ALREADY_STARTED");
    }

    @Test
    void snapshotForAMember() throws Exception {
        mvc.perform(get("/api/rooms/P4CWRT").header(HttpHeaders.AUTHORIZATION, bearer("riley")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.roomCode").value("P4CWRT"));

        GetRoomSnapshotRequest request = ROOM.calls.last(GetRoomSnapshotRequest.class);
        assertThat(request.getUserId()).isEqualTo("user-riley");
    }

    @Test
    void snapshotForSomeoneNotInTheRoomIs403NotInRoom() throws Exception {
        ROOM.calls.fail(GetRoomSnapshotRequest.class, Status.PERMISSION_DENIED);

        mvc.perform(get("/api/rooms/P4CWRT").header(HttpHeaders.AUTHORIZATION, bearer("riley")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error.code").value("NOT_IN_ROOM"));
    }

    @Test
    void creatingTooManyRoomsIs429() throws Exception {
        for (int i = 0; i < 10; i++) {
            mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer("speedy")))
                    .andExpect(status().isCreated());
        }

        mvc.perform(post("/api/rooms").header(HttpHeaders.AUTHORIZATION, bearer("speedy")))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
    }

    private void expectJoinError(Status status, int httpStatus, String code) throws Exception {
        ROOM.calls.fail(JoinRoomRequest.class, status);
        mvc.perform(post("/api/rooms/P4CWRT/join").header(HttpHeaders.AUTHORIZATION, bearer("jordan")))
                .andExpect(status().is(httpStatus))
                .andExpect(jsonPath("$.error.code").value(code));
    }

    private static String bearer(String name) {
        return FakeTokens.bearer(FakeTokens.token(name));
    }
}
