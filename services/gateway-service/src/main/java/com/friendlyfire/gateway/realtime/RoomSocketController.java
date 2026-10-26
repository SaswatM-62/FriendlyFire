package com.friendlyfire.gateway.realtime;

import com.friendlyfire.proto.friendlyfire.room.v1.RoomServiceGrpc.RoomServiceBlockingStub;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

/**
 * A player's actions in a room, sent over STOMP to /app/room/{code}/... Room updates go
 * back to everyone on /topic/room/{code} (RoomEventRelay). These do nothing yet.
 */
@Controller
public class RoomSocketController {

    private final RoomServiceBlockingStub rooms;

    public RoomSocketController(RoomServiceBlockingStub rooms) {
        this.rooms = rooms;
    }

    @MessageMapping("/room/{code}/leave")
    public void leave(@DestinationVariable String code) {
        // TODO(ff): the player leaves the room for good
        //   - calls RoomService.LeaveRoom with the session's user_id (never from the body)
        //   - send errors to the player on /user/queue/errors
        //   - uses: rooms
    }

    @MessageMapping("/room/{code}/kick")
    public void kick(@DestinationVariable String code) {
        // TODO(ff): host only: remove a player from the lobby
        //   - body {"targetUserId": "..."}; calls RoomService.KickPlayer
        //   - uses: rooms
    }

    @MessageMapping("/room/{code}/settings")
    public void settings(@DestinationVariable String code) {
        // TODO(ff): host only: change the question count and time per question
        //   - body {"questionCount": 10, "secondsPerQuestion": 20}; calls
        //     RoomService.UpdateSettings
        //   - uses: rooms
    }

    @MessageMapping("/room/{code}/start")
    public void start(@DestinationVariable String code) {
        // TODO(ff): host only: start the quiz
        //   - calls RoomService.StartQuiz; FAILED_PRECONDITION's message goes to the host
        //   - uses: rooms
    }

    @MessageMapping("/room/{code}/answer")
    public void answer(@DestinationVariable String code) {
        // TODO(ff): a player's answer to the current question
        //   - body {"questionId": "...", "optionId": "..."}; calls RoomService.SubmitAnswer
        //   - uses: rooms
    }

    @MessageMapping("/room/{code}/vote")
    public void vote(@DestinationVariable String code) {
        // TODO(ff): a player's vote about a disconnected player
        //   - body {"option": "WAIT" | "CONTINUE_WITHOUT_PLAYER" | "END_QUIZ"}; calls
        //     RoomService.CastVote
        //   - uses: rooms
    }

    @MessageMapping("/room/{code}/connection-quality")
    public void connectionQuality(@DestinationVariable String code) {
        // TODO(ff): the player's connection quality, measured from their pings
        //   - body {"quality": "GOOD" | "FAIR" | "POOR"}; calls
        //     RoomService.ReportConnectionQuality
        //   - uses: rooms
    }
}
