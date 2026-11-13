package com.friendlyfire.gateway.web;

import com.friendlyfire.gateway.json.RoomSnapshotJson;
import com.friendlyfire.gateway.protection.RateLimit;
import com.friendlyfire.gateway.protection.RateLimiter;
import com.friendlyfire.gateway.security.SignedInUser;
import com.friendlyfire.proto.friendlyfire.room.v1.CreateRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.GetRoomSnapshotRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.JoinRoomRequest;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import io.grpc.Status;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Creating and joining rooms (FF-303). Everything after that happens over STOMP
 * (RoomSocketController).
 */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private static final Map<Status.Code, String> JOIN_ERRORS = Map.of(
            Status.Code.NOT_FOUND, ErrorCodes.ROOM_NOT_FOUND,
            Status.Code.RESOURCE_EXHAUSTED, ErrorCodes.ROOM_FULL,
            Status.Code.FAILED_PRECONDITION, ErrorCodes.QUIZ_ALREADY_STARTED);

    private static final Map<Status.Code, String> SNAPSHOT_ERRORS = Map.of(
            Status.Code.NOT_FOUND, ErrorCodes.ROOM_NOT_FOUND,
            Status.Code.PERMISSION_DENIED, ErrorCodes.NOT_IN_ROOM);

    private final RoomServiceBlockingStub rooms;
    private final RateLimiter rateLimiter;

    public RoomController(RoomServiceBlockingStub rooms, RateLimiter rateLimiter) {
        this.rooms = rooms;
        this.rateLimiter = rateLimiter;
    }

    /** A new room with the signed-in user as host. 201 with the room. */
    @PostMapping
    public ResponseEntity<RoomSnapshotJson> create(SignedInUser me, HttpServletRequest request) {
        rateLimiter.check(RateLimit.CREATE_ROOM, me.userId(), request.getRemoteAddr());
        RoomSnapshot room = rooms.createRoom(CreateRoomRequest.newBuilder()
                        .setHostUserId(me.userId())
                        .setHostDisplayName(me.displayName())
                        .build())
                .getSnapshot();
        return ResponseEntity.status(HttpStatus.CREATED).body(RoomSnapshotJson.from(room));
    }

    /**
     * Joins a room, or rejoins it after a refresh or a dropped connection. 404
     * ROOM_NOT_FOUND, 409 ROOM_FULL (8 players), 409 QUIZ_ALREADY_STARTED.
     */
    @PostMapping("/{code}/join")
    public RoomSnapshotJson join(@PathVariable String code, SignedInUser me, HttpServletRequest request) {
        String roomCode = RoomCodes.require(code);
        rateLimiter.check(RateLimit.JOIN_ROOM, me.userId(), request.getRemoteAddr());
        // TODO(ff): pass the player's Riot tag line, so two players with the same name can
        //   be told apart (FF-303)
        //   - JoinRoomRequest gets the field from FF-202; the tag line comes from the
        //     cached user's Riot link (UserResolver)
        RoomSnapshot room = GrpcErrors.renaming(JOIN_ERRORS, () -> rooms.joinRoom(JoinRoomRequest.newBuilder()
                        .setRoomCode(roomCode)
                        .setUserId(me.userId())
                        .setDisplayName(me.displayName())
                        .build()))
                .getSnapshot();
        return RoomSnapshotJson.from(room);
    }

    /** The room's current state, for a page load. 404 ROOM_NOT_FOUND, 403 NOT_IN_ROOM. */
    @GetMapping("/{code}")
    public RoomSnapshotJson snapshot(@PathVariable String code, SignedInUser me) {
        String roomCode = RoomCodes.require(code);
        RoomSnapshot room = GrpcErrors.renaming(SNAPSHOT_ERRORS, () -> rooms.getRoomSnapshot(GetRoomSnapshotRequest.newBuilder()
                        .setRoomCode(roomCode)
                        .setUserId(me.userId())
                        .build()))
                .getSnapshot();
        return RoomSnapshotJson.from(room);
    }
}
