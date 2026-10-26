package com.friendlyfire.gateway.web;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Creating and joining rooms. Everything after that happens over STOMP (RoomSocketController). */
@RestController
@RequestMapping("/api/rooms")
public class RoomController {

    private final RoomServiceBlockingStub rooms;

    public RoomController(RoomServiceBlockingStub rooms) {
        this.rooms = rooms;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> create() {
        // TODO(ff): create a room with the signed-in user as host
        //   - calls RoomService.CreateRoom; 201 with the room snapshot
        //   - uses: rooms
        return NotImplemented.response();
    }

    @PostMapping("/{code}/join")
    public ResponseEntity<Map<String, String>> join(@PathVariable String code) {
        // TODO(ff): join a room (or rejoin after a refresh)
        //   - calls RoomService.JoinRoom; 200 with the room snapshot
        //   - NOT_FOUND -> 404, RESOURCE_EXHAUSTED (8 players) -> 409,
        //     FAILED_PRECONDITION (quiz already started) -> 409
        //   - uses: rooms
        return NotImplemented.response();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, String>> snapshot(@PathVariable String code) {
        // TODO(ff): the room's current state, for a page load
        //   - calls RoomService.GetRoomSnapshot; NOT_FOUND -> 404, PERMISSION_DENIED -> 403
        //   - uses: rooms
        return NotImplemented.response();
    }
}
