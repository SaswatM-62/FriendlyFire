package com.friendlyfire.gateway.json;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.room.v1.ChatMessage;
import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.EndReason;
import com.friendlyfire.proto.friendlyfire.room.v1.FastestAnswer;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerLook;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerDataStatus;
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
                "notEnoughMatches", false,
                "answeredCurrentQuestion", false)));
    }

    @Test
    void aPlayerCarriesTheirLook() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .addPlayers(Player.newBuilder()
                        .setUserId("user-maya")
                        .setLook(PlayerLook.newBuilder().setIcon("ghost").setColour("cyan").setExpression("wink").setRandom(true)))
                .build());

        @SuppressWarnings("unchecked")
        Map<String, Object> player = ((List<Map<String, Object>>) json.get("players")).getFirst();
        assertThat(player.get("look")).isEqualTo(Map.of("icon", "ghost", "colour", "cyan", "expression", "wink", "random", true));
    }

    @Test
    void aPlayerCarriesTheirDataStatusAndRiotTag() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_LOBBY)
                .addPlayers(Player.newBuilder()
                        .setUserId("user-sam")
                        .setDisplayName("Sam")
                        .setRiotTagLine("NA1")
                        .setDataStatus(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCING)
                        .setDataEstimatedSeconds(120))
                .addPlayers(Player.newBuilder()
                        .setUserId("user-jordan")
                        .setDisplayName("Jordan")
                        .setDataStatus(PlayerDataStatus.PLAYER_DATA_STATUS_SYNCED)
                        .setNotEnoughMatches(true))
                .build());

        assertThat(json.get("players")).isEqualTo(List.of(
                Map.of(
                        "userId", "user-sam",
                        "displayName", "Sam",
                        "riotTagLine", "NA1",
                        "dataStatus", "SYNCING",
                        "dataEstimatedSeconds", 120,
                        "notEnoughMatches", false,
                        "answeredCurrentQuestion", false),
                Map.of(
                        "userId", "user-jordan",
                        "displayName", "Jordan",
                        "dataStatus", "SYNCED",
                        "notEnoughMatches", true,
                        "answeredCurrentQuestion", false)));
        // Pause time only counts once a quiz has started.
        assertThat(json).doesNotContainKey("pauseSecondsLeft");
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
                "notEnoughMatches", false,
                "connectionQuality", "GOOD",
                "answeredCurrentQuestion", true)));
        assertThat(json.get("scoreboard")).isEqualTo(List.of(Map.of(
                "userId", "user-sam", "displayName", "Sam", "totalPoints", 740, "rank", 1, "pointsLastRound", 740,
                "correctAnswers", 0)));
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
    void theVoteCountsPauseVotesAndSaysWhetherPausingIsStillAllowed() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_VOTE)
                .setQuizId("quiz-1")
                .setPauseSecondsLeft(120)
                .setActiveVote(Vote.newBuilder()
                        .setAboutUserId("user-sam")
                        .setDeadline(DEADLINE)
                        .setPauseVotes(2)
                        .setContinueVotes(1)
                        .addVotedUserIds("user-maya")
                        .setPauseAllowed(true))
                .build());

        assertThat(json.get("activeVote")).isEqualTo(Map.of(
                "aboutUserId", "user-sam",
                "deadline", "2025-11-09T14:53:20.120Z",
                "pauseVotes", 2,
                "continueVotes", 1,
                "endVotes", 0,
                "votedUserIds", List.of("user-maya"),
                "pauseAllowed", true));
        assertThat(json).containsEntry("pauseSecondsLeft", 120);
    }

    @Test
    void aPausedRoomSaysWhoItIsWaitingFor() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_PAUSED)
                .setQuizId("quiz-1")
                .setWaitingForUserId("user-sam")
                .setPauseSecondsLeft(60)
                .setPhaseDeadline(DEADLINE)
                .build());

        assertThat(json).containsEntry("phase", "PAUSED")
                .containsEntry("waitingForUserId", "user-sam")
                .containsEntry("pauseSecondsLeft", 60)
                .containsEntry("phaseDeadline", "2025-11-09T14:53:20.120Z");
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
    void aFailedStartCarriesItsMessageAndOtherEventsLeaveItOut() {
        RoomEventJson failed = RoomEventJson.from(RoomEvent.newBuilder()
                .setType(RoomEventType.ROOM_EVENT_TYPE_QUIZ_START_FAILED)
                .setMessage("Not enough matches for 15 questions. Try 10.")
                .setSnapshot(RoomSnapshot.newBuilder().setPhase(RoomPhase.ROOM_PHASE_LOBBY))
                .build());
        RoomEventJson joined = RoomEventJson.from(RoomEvent.newBuilder()
                .setType(RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED)
                .build());

        assertThat(failed.type()).isEqualTo("QUIZ_START_FAILED");
        assertThat(failed.message()).isEqualTo("Not enough matches for 15 questions. Try 10.");
        assertThat(joined.message()).isNull();
    }

    @Test
    @SuppressWarnings("unchecked")
    void preparingShowsItsMinimumDeadlineAndASpectatorIsMarked() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_PREPARING)
                .setPhaseDeadline(DEADLINE)
                .addPlayers(Player.newBuilder().setUserId("user-ana").setStatus(PlayerStatus.PLAYER_STATUS_SPECTATING))
                .build());

        assertThat(json.get("phase")).isEqualTo("PREPARING");
        assertThat(json.get("phaseDeadline")).isEqualTo("2025-11-09T14:53:20.120Z");
        assertThat((List<Map<String, Object>>) json.get("players"))
                .first().satisfies(player -> assertThat(player.get("status")).isEqualTo("SPECTATING"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void aFinishedQuizSaysWhyItEndedAndWhoWasFastest() {
        Map<String, Object> json = json(RoomSnapshot.newBuilder()
                .setPhase(RoomPhase.ROOM_PHASE_FINISHED)
                .setQuizId("quiz-1")
                .setEndReason(EndReason.END_REASON_VOTED_TO_END)
                .setFastestAnswer(FastestAnswer.newBuilder().setUserId("user-sam").setResponseTimeMs(4_260))
                .addScoreboard(ScoreEntry.newBuilder()
                        .setUserId("user-sam").setDisplayName("Sam").setTotalPoints(3180).setRank(1).setCorrectAnswers(4))
                .build());

        assertThat(json).containsEntry("endReason", "VOTED_TO_END");
        assertThat(json.get("fastestAnswer")).isEqualTo(Map.of("userId", "user-sam", "seconds", 4.3));
        assertThat((List<Map<String, Object>>) json.get("scoreboard"))
                .first().satisfies(entry -> assertThat(entry.get("correctAnswers")).isEqualTo(4));
    }

    @Test
    void aChatEventCarriesTheMessage() {
        RoomEventJson event = RoomEventJson.from(RoomEvent.newBuilder()
                .setType(RoomEventType.ROOM_EVENT_TYPE_CHAT_MESSAGE)
                .setSubjectUserId("user-sam")
                .setChat(ChatMessage.newBuilder()
                        .setMessageId("msg-9").setUserId("user-sam").setDisplayName("Sam").setText("gg")
                        .setSentAt(DEADLINE))
                .build());

        assertThat(event.type()).isEqualTo("CHAT_MESSAGE");
        assertThat(event.chat()).isEqualTo(
                new ChatMessageJson("msg-9", "user-sam", "Sam", "gg", "2025-11-09T14:53:20.120Z"));
        assertThat(RoomEventJson.from(RoomEvent.getDefaultInstance()).chat()).isNull();
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
