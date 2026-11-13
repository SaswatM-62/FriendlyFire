package com.friendlyfire.gateway.realtime;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.VoteOption;
import org.junit.jupiter.api.Test;

class RoomDestinationsTest {

    @Test
    void readsTheRoomFromTheTopic() {
        assertThat(RoomDestinations.topicRoom("/topic/room/K7QM2X")).isEqualTo("K7QM2X");
        assertThat(RoomDestinations.topicRoom("/topic/other")).isNull();
        assertThat(RoomDestinations.topicRoom(null)).isNull();
    }

    @Test
    void readsTheRoomFromAnAction() {
        assertThat(RoomDestinations.appRoom("/app/room/K7QM2X/answer")).isEqualTo("K7QM2X");
        assertThat(RoomDestinations.appRoom("/app/ping")).isNull();
    }

    @Test
    void mapsTheFrontendsVoteNamesToTheContract() {
        assertThat(RoomSocketController.voteOption("PAUSE")).isEqualTo(VoteOption.VOTE_OPTION_WAIT);
        assertThat(RoomSocketController.voteOption("WAIT")).isEqualTo(VoteOption.VOTE_OPTION_WAIT);
        assertThat(RoomSocketController.voteOption("CONTINUE_WITHOUT_PLAYER"))
                .isEqualTo(VoteOption.VOTE_OPTION_CONTINUE_WITHOUT_PLAYER);
        assertThat(RoomSocketController.voteOption("END_QUIZ")).isEqualTo(VoteOption.VOTE_OPTION_END_QUIZ);
    }

    @Test
    void mapsConnectionQuality() {
        assertThat(RoomSocketController.quality("GOOD")).isEqualTo(ConnectionQuality.CONNECTION_QUALITY_GOOD);
        assertThat(RoomSocketController.quality("POOR")).isEqualTo(ConnectionQuality.CONNECTION_QUALITY_POOR);
    }
}
