package com.friendlyfire.gateway.json;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.friendlyfire.proto.friendlyfire.room.v1.FastestAnswer;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerResult;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionOption;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionView;
import com.friendlyfire.proto.friendlyfire.room.v1.Reveal;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.ScoreEntry;
import com.friendlyfire.proto.friendlyfire.room.v1.Vote;
import java.util.List;

/**
 * Everything a browser needs to draw a room: the REST room endpoints return it, and every
 * event on /topic/room/{code} carries one. The same shape as RoomSnapshot in
 * frontend/src/lib/api-types.ts and docs/realtime-messages.md.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record RoomSnapshotJson(
        String roomCode,
        String hostUserId,
        String phase,
        SettingsJson settings,
        List<PlayerJson> players,
        String quizId,
        int roundNumber,
        QuestionJson currentQuestion,
        String phaseDeadline,
        List<ScoreEntryJson> scoreboard,
        VoteJson activeVote,
        String waitingForUserId,
        Integer pauseSecondsLeft,
        String endReason,
        FastestAnswerJson fastestAnswer) {

    public static RoomSnapshotJson from(RoomSnapshot room) {
        return new RoomSnapshotJson(
                room.getRoomCode(),
                ProtoJson.text(room.getHostUserId()),
                ProtoJson.enumName(room.getPhase(), "ROOM_PHASE_"),
                SettingsJson.from(room.getSettings()),
                room.getPlayersList().stream().map(PlayerJson::from).toList(),
                ProtoJson.text(room.getQuizId()),
                room.getRoundNumber(),
                room.hasCurrentQuestion() ? QuestionJson.from(room.getCurrentQuestion()) : null,
                ProtoJson.timestamp(room.hasPhaseDeadline(), room.getPhaseDeadline()),
                room.getScoreboardList().stream().map(ScoreEntryJson::from).toList(),
                room.hasActiveVote() ? VoteJson.from(room.getActiveVote()) : null,
                ProtoJson.text(room.getWaitingForUserId()),
                // Pause time left in this quiz: only once a quiz has started.
                room.getQuizId().isEmpty() ? null : room.getPauseSecondsLeft(),
                ProtoJson.enumName(room.getEndReason(), "END_REASON_"),
                room.hasFastestAnswer() ? FastestAnswerJson.from(room.getFastestAnswer()) : null);
    }

    /** During FINISHED: the quickest correct answer of the quiz, in seconds to one decimal (the podium). */
    public record FastestAnswerJson(String userId, double seconds) {

        static FastestAnswerJson from(FastestAnswer fastest) {
            return new FastestAnswerJson(fastest.getUserId(), Math.round(fastest.getResponseTimeMs() / 100.0) / 10.0);
        }
    }

    public record SettingsJson(int questionCount, int secondsPerQuestion) {

        static SettingsJson from(RoomSettings settings) {
            return new SettingsJson(settings.getQuestionCount(), settings.getSecondsPerQuestion());
        }
    }

    /** {@code dataEstimatedSeconds} only while SYNCING with an estimate. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record PlayerJson(
            String userId,
            String displayName,
            String riotTagLine,
            String status,
            String dataStatus,
            Integer dataEstimatedSeconds,
            boolean notEnoughMatches,
            String connectionQuality,
            boolean answeredCurrentQuestion) {

        static PlayerJson from(Player player) {
            return new PlayerJson(
                    player.getUserId(),
                    player.getDisplayName(),
                    ProtoJson.text(player.getRiotTagLine()),
                    ProtoJson.enumName(player.getStatus(), "PLAYER_STATUS_"),
                    ProtoJson.enumName(player.getDataStatus(), "PLAYER_DATA_STATUS_"),
                    player.getDataEstimatedSeconds() > 0 ? player.getDataEstimatedSeconds() : null,
                    player.getNotEnoughMatches(),
                    ProtoJson.enumName(player.getConnectionQuality(), "CONNECTION_QUALITY_"),
                    player.getAnsweredCurrentQuestion());
        }
    }

    /** During QUESTION and REVEAL. {@code reveal} only during REVEAL. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record QuestionJson(String questionId, String text, List<OptionJson> options, RevealJson reveal) {

        static QuestionJson from(QuestionView question) {
            return new QuestionJson(
                    question.getQuestionId(),
                    question.getText(),
                    question.getOptionsList().stream().map(OptionJson::from).toList(),
                    question.hasReveal() ? RevealJson.from(question.getReveal()) : null);
        }
    }

    public record OptionJson(String optionId, String label) {

        static OptionJson from(QuestionOption option) {
            return new OptionJson(option.getOptionId(), option.getLabel());
        }
    }

    public record RevealJson(String correctOptionId, String explanation, List<ResultJson> results) {

        static RevealJson from(Reveal reveal) {
            return new RevealJson(
                    reveal.getCorrectOptionId(),
                    reveal.getExplanation(),
                    reveal.getResultsList().stream().map(ResultJson::from).toList());
        }
    }

    /** {@code chosenOptionId} is left out if they didn't answer. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ResultJson(String userId, String chosenOptionId, boolean correct, int points) {

        static ResultJson from(PlayerResult result) {
            return new ResultJson(
                    result.getUserId(), ProtoJson.text(result.getChosenOptionId()), result.getCorrect(), result.getPoints());
        }
    }

    public record ScoreEntryJson(
            String userId, String displayName, int totalPoints, int rank, int pointsLastRound, int correctAnswers) {

        static ScoreEntryJson from(ScoreEntry entry) {
            return new ScoreEntryJson(
                    entry.getUserId(),
                    entry.getDisplayName(),
                    entry.getTotalPoints(),
                    entry.getRank(),
                    entry.getPointsLastRound(),
                    entry.getCorrectAnswers());
        }
    }

    /** During VOTE. {@code pauseAllowed} is false once the quiz's 3 minutes of pauses are used. */
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record VoteJson(
            String aboutUserId,
            String deadline,
            int pauseVotes,
            int continueVotes,
            int endVotes,
            List<String> votedUserIds,
            boolean pauseAllowed) {

        static VoteJson from(Vote vote) {
            return new VoteJson(
                    vote.getAboutUserId(),
                    ProtoJson.timestamp(vote.hasDeadline(), vote.getDeadline()),
                    vote.getPauseVotes(),
                    vote.getContinueVotes(),
                    vote.getEndVotes(),
                    List.copyOf(vote.getVotedUserIdsList()),
                    vote.getPauseAllowed());
        }
    }
}
