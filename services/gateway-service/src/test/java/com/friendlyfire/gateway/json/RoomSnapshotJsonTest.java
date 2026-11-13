package com.friendlyfire.gateway.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerResult;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionOption;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionView;
import com.friendlyfire.proto.friendlyfire.room.v1.Reveal;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEvent;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.ScoreEntry;
import com.friendlyfire.proto.friendlyfire.room.v1.Vote;
import com.google.protobuf.Timestamp;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

/** The room JSON matches docs/realtime-messages.md and frontend/src/lib/api-types.ts. */
class RoomSnapshotJsonTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Timestamp DEADLINE = Timestamp.newBuilder().setSeconds(1_762_700_000L).setNanos(120_000_000).build();

    @Test
    void aLobbyLeavesOutWhatIsntSet() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setRoomCode("K7QM2X")
                .setHostUserId("user-maya")
                .setPhase(RoomPhase.ROOM_PHASE_LOBBY)
                .setSettings(RoomSettings.newBuilder().setQuestionCount(10).setSecondsPerQuestion(20))
                .addPlayers(Player.newBuilder()
                        .setUserId("user-maya")
                        .setDisplayName("Maya")
                        .setStatus(PlayerStatus.PLAYER_STATUS_CONNECTED))
                .build());

        assertThat(json).containsOnlyKeys("roomCode", "hostUserId", "phase", "settings", "players", "roundNumber", "scoreboard");
        assertThat(json).containsEntry("phase", "LOBBY").containsEntry("roundNumber", 0).containsEntry("scoreboard", List.of());
        assertThat(json.get("settings")).isEqualTo(Map.of("questionCount", 10, "secondsPerQuestion", 20));
        assertThat(json.get("players")).isEqualTo(List.of(Map.of(
                "userId", "user-maya",
                "displayName", "Maya",
                "status", "CONNECTED",
                "matchDataReady", false,
                "answeredCurrentQuestion", false)));
    }

    @Test
    void aQuestionHidesTheAnswerUntilTheReveal() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setRoomCode("K7QM2X")
                .setPhase(RoomPhase.ROOM_PHASE_QUESTION)
                .setQuizId("quiz-1")
                .setRoundNumber(3)
                .setPhaseDeadline(DEADLINE)
                .addPlayers(Player.newBuilder().setUserId("user-sam").setConnectionQuality(ConnectionQuality.CONNECTION_QUALITY_GOOD)
                        .setAnsweredCurrentQuestion(true))
                .setCurrentQuestion(QuestionView.newBuilder()
                        .setQuestionId("q-3")
                        .setText("Who has the most headshots?")
                        .addOptions(QuestionOption.newBuilder().setOptionId("user-sam").setLabel("Sam")))
                .addScoreboard(ScoreEntry.newBuilder()
                        .setUserId("user-sam").setDisplayName("Sam").setTotalPoints(740).setRank(1).setPointsLastRound(740))
                .build());

        assertThat(json).containsEntry("phase", "QUESTION")
                .containsEntry("quizId", "quiz-1")
                .containsEntry("roundNumber", 3)
                .containsEntry("phaseDeadline", "2025-11-09T14:53:20.120Z");
        assertThat(json.get("currentQuestion")).isEqualTo(Map.of(
                "questionId", "q-3",
                "text", "Who has the most headshots?",
                "options", List.of(Map.of("optionId", "user-sam", "label", "Sam"))));
        // No status set: left out, like every UNSPECIFIED enum.
        assertThat(json.get("players")).isEqualTo(List.of(Map.of(
                "userId", "user-sam",
                "displayName", "",
                "matchDataReady", false,
                "connectionQuality", "GOOD",
                "answeredCurrentQuestion", true)));
        assertThat(json.get("scoreboard")).isEqualTo(List.of(Map.of(
                "userId", "user-sam", "displayName", "Sam", "totalPoints", 740, "rank", 1, "pointsLastRound", 740)));
    }

    @Test
    void theRevealShowsTheAnswerAndWhoPickedWhat() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_REVEAL)
                .setCurrentQuestion(QuestionView.newBuilder()
                        .setQuestionId("q-3")
                        .setReveal(Reveal.newBuilder()
                                .setCorrectOptionId("user-sam")
                                .setExplanation("Sam hit 41% headshots.")
                                .addResults(PlayerResult.newBuilder().setUserId("user-sam").setChosenOptionId("user-sam")
                                        .setCorrect(true).setPoints(740))
                                .addResults(PlayerResult.newBuilder().setUserId("user-maya"))))
                .build());

        @SuppressWarnings("unchecked")
        Map<String, Object> question = (Map<String, Object>) json.get("currentQuestion");
        assertThat(question.get("reveal")).isEqualTo(Map.of(
                "correctOptionId", "user-sam",
                "explanation", "Sam hit 41% headshots.",
                "results", List.of(
                        Map.of("userId", "user-sam", "chosenOptionId", "user-sam", "correct", true, "points", 740),
                        Map.of("userId", "user-maya", "correct", false, "points", 0))));
    }

    @Test
    void theVoteCallsWaitVotesPauseVotesLikeTheFrontend() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_VOTE)
                .setActiveVote(Vote.newBuilder()
                        .setAboutUserId("user-sam")
                        .setDeadline(DEADLINE)
                        .setWaitVotes(2)
                        .setContinueVotes(1)
                        .addVotedUserIds("user-maya"))
                .build());

        assertThat(json.get("activeVote")).isEqualTo(Map.of(
                "aboutUserId", "user-sam",
                "deadline", "2025-11-09T14:53:20.120Z",
                "pauseVotes", 2,
                "continueVotes", 1,
                "endVotes", 0,
                "votedUserIds", List.of("user-maya")));
    }

    @Test
    void anEventCarriesItsTypeWithoutThePrefix() throws Exception {
        RoomEventJson event = RoomEventJson.from(RoomEvent.newBuilder()
                .setType(RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED)
                .setSubjectUserId("user-sam")
                .setOccurredAt(DEADLINE)
                .setSnapshot(RoomSnapshot.newBuilder().setRoomCode("K7QM2X"))
                .build());

        assertThat(event.type()).isEqualTo("PLAYER_JOINED");
        assertThat(event.subjectUserId()).isEqualTo("user-sam");
        assertThat(event.occurredAt()).isEqualTo("2025-11-09T14:53:20.120Z");
        assertThat(event.snapshot().roomCode()).isEqualTo("K7QM2X");
    }

    @Test
    void anUnspecifiedEnumIsLeftOut() {
        assertThat(json(RoomSnapshot.newBuilder().setRoomCode("K7QM2X").build())).doesNotContainKey("phase");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> json(RoomSnapshot room) {
        return JSON.readValue(JSON.writeValueAsString(RoomSnapshotJson.from(room)), Map.class);
    }
}
