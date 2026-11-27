package com.friendlyfire.room.room;

import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.room.v1.ChatMessage;
import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.EndReason;
import com.friendlyfire.proto.friendlyfire.room.v1.FastestAnswer;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerLook;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerResult;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionOption;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionView;
import com.friendlyfire.proto.friendlyfire.room.v1.Reveal;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.ScoreEntry;
import com.friendlyfire.proto.friendlyfire.room.v1.Vote;
import com.friendlyfire.proto.friendlyfire.room.v1.VoteOption;
import com.friendlyfire.room.clients.PlayerData;
import com.friendlyfire.room.timing.Scheduler.Cancellable;
import com.google.protobuf.Timestamp;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One room's state. Not thread-safe by itself: RoomManager and QuizRunner change it only
 * while holding the room's lock ({@code synchronized (room)}), and publish each event
 * under the same lock, so every stream sees the events in order.
 */
public class Room {

    /** People in a room, spectators included. */
    public static final int MAX_PEOPLE = 8;
    public static final int MIN_PLAYERS = 2;
    /** Chat messages a room keeps (GetChatHistory). */
    public static final int CHAT_KEEP = 200;

    static final RoomSettings DEFAULT_SETTINGS =
            RoomSettings.newBuilder().setQuestionCount(10).setSecondsPerQuestion(20).build();

    private final String code;

    String hostUserId;
    RoomPhase phase = RoomPhase.ROOM_PHASE_LOBBY;
    RoomSettings settings = DEFAULT_SETTINGS;
    /** In the order they joined. */
    final Map<String, Member> members = new LinkedHashMap<>();

    /** The current or last quiz; null before the first one, or after a failed start. */
    Quiz quiz;
    /** 1-based; during the countdown, still the question just played. */
    int roundNumber;
    /** Set during QUESTION and REVEAL. */
    Question currentQuestion;
    /** Set during REVEAL. */
    List<PlayerResult> currentResults = List.of();
    Instant phaseDeadline;
    VoteState vote;
    String waitingForUserId;
    EndReason endReason;

    /** Template IDs of every question asked here, oldest first (GenerateQuiz). */
    final List<String> askedTemplateIds = new ArrayList<>();
    final Deque<ChatMessage> chat = new ArrayDeque<>();

    /** Goes up on every phase change, so a timer set for an earlier phase does nothing. */
    long generation;
    Cancellable phaseTimer;
    Cancellable hostTimer;
    Instant lastActivity;
    boolean closed;

    public Room(String code, Instant now) {
        this.code = code;
        this.lastActivity = now;
    }

    public String getCode() {
        return code;
    }

    /** The snapshot browsers see. The answer is only in it during REVEAL. */
    public RoomSnapshot snapshot() {
        RoomSnapshot.Builder snapshot = RoomSnapshot.newBuilder()
                .setRoomCode(code)
                .setHostUserId(hostUserId == null ? "" : hostUserId)
                .setPhase(phase)
                .setSettings(settings)
                .setRoundNumber(roundNumber);
        members.values().forEach(m -> snapshot.addPlayers(m.toPlayer()));
        if (phaseDeadline != null) {
            snapshot.setPhaseDeadline(timestamp(phaseDeadline));
        }
        if (quiz != null) {
            snapshot.setQuizId(quiz.id).setPauseSecondsLeft(quiz.pauseSecondsLeft);
            quiz.scoreboard().forEach(snapshot::addScoreboard);
        }
        boolean showQuestion = phase == RoomPhase.ROOM_PHASE_QUESTION || phase == RoomPhase.ROOM_PHASE_REVEAL;
        if (showQuestion && currentQuestion != null) {
            QuestionView.Builder view = QuestionView.newBuilder()
                    .setQuestionId(currentQuestion.getQuestionId())
                    .setText(currentQuestion.getText());
            currentQuestion.getOptionsList().forEach(o -> view.addOptions(
                    QuestionOption.newBuilder().setOptionId(o.getOptionId()).setLabel(o.getLabel())));
            if (phase == RoomPhase.ROOM_PHASE_REVEAL) {
                view.setReveal(Reveal.newBuilder()
                        .setCorrectOptionId(currentQuestion.getCorrectOptionId())
                        .setExplanation(currentQuestion.getExplanation())
                        .addAllResults(currentResults));
            }
            snapshot.setCurrentQuestion(view);
        }
        if (vote != null && phase == RoomPhase.ROOM_PHASE_VOTE) {
            snapshot.setActiveVote(vote.toProto());
        }
        if (waitingForUserId != null && phase == RoomPhase.ROOM_PHASE_PAUSED) {
            snapshot.setWaitingForUserId(waitingForUserId);
        }
        if (phase == RoomPhase.ROOM_PHASE_FINISHED) {
            if (endReason != null) {
                snapshot.setEndReason(endReason);
            }
            if (quiz != null && quiz.fastestUserId != null) {
                snapshot.setFastestAnswer(FastestAnswer.newBuilder()
                        .setUserId(quiz.fastestUserId)
                        .setResponseTimeMs(quiz.fastestMs));
            }
        }
        return snapshot.build();
    }

    Optional<Member> member(String userId) {
        return Optional.ofNullable(members.get(userId));
    }

    boolean isPlaying(String userId) {
        return quiz != null && quiz.playerIds.contains(userId) && members.containsKey(userId);
    }

    /** Quiz players still in the room, in join order. */
    List<Member> quizMembers() {
        if (quiz == null) {
            return List.of();
        }
        return members.values().stream().filter(m -> quiz.playerIds.contains(m.userId)).toList();
    }

    boolean quizRunning() {
        return switch (phase) {
            case ROOM_PHASE_PREPARING, ROOM_PHASE_STARTING, ROOM_PHASE_QUESTION, ROOM_PHASE_REVEAL,
                    ROOM_PHASE_VOTE, ROOM_PHASE_PAUSED -> true;
            default -> false;
        };
    }

    static Timestamp timestamp(Instant instant) {
        return Timestamp.newBuilder().setSeconds(instant.getEpochSecond()).setNanos(instant.getNano()).build();
    }

    /** Someone in the room: a player, or a spectator during a quiz. */
    static final class Member {
        final String userId;
        String displayName;
        String riotTagLine;
        PlayerStatus status = PlayerStatus.PLAYER_STATUS_CONNECTED;
        ConnectionQuality quality = ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED;
        boolean answered;
        PlayerData data = PlayerData.UNKNOWN;
        /** Their character in this room, handed out when they join (Looks) and kept. */
        PlayerLook look = PlayerLook.getDefaultInstance();
        /** Set while DISCONNECTED or SITTING_OUT. */
        Instant disconnectedAt;

        Member(String userId, String displayName, String riotTagLine) {
            this.userId = userId;
            this.displayName = displayName;
            this.riotTagLine = riotTagLine;
        }

        boolean connected() {
            return status == PlayerStatus.PLAYER_STATUS_CONNECTED || status == PlayerStatus.PLAYER_STATUS_SPECTATING;
        }

        Player toPlayer() {
            return Player.newBuilder()
                    .setUserId(userId)
                    .setDisplayName(displayName)
                    .setStatus(status)
                    .setConnectionQuality(quality)
                    .setAnsweredCurrentQuestion(answered)
                    .setDataStatus(data.status())
                    .setDataEstimatedSeconds(data.estimatedSeconds())
                    .setNotEnoughMatches(data.notEnoughMatches())
                    .setRiotTagLine(riotTagLine)
                    .setLook(look)
                    .build();
        }
    }

    /** A quiz from Start to its final standings. */
    static final class Quiz {
        final String id;
        /** Everyone playing, in join order when it started (spectators aren't). */
        final List<String> playerIds;
        final Instant preparingStartedAt;
        final Instant preparingMinimumEnds;
        List<Question> questions = List.of();
        boolean ready;
        int roundsPlayed;
        int pauseSecondsLeft;
        /** Each player's answer to the open question. */
        final Map<String, Answer> answers = new LinkedHashMap<>();
        Instant questionStartedAt;
        /** True from the moment a question is being graded until the next question. */
        boolean answersClosed;
        /** Points per player, in Start order. */
        final Map<String, Score> scores = new LinkedHashMap<>();
        String fastestUserId;
        int fastestMs;
        Instant pauseStartedAt;
        int pauseGrantedSeconds;

        Quiz(String id, List<String> playerIds, Instant now, Instant minimumEnds, int pauseSeconds) {
            this.id = id;
            this.playerIds = List.copyOf(playerIds);
            this.preparingStartedAt = now;
            this.preparingMinimumEnds = minimumEnds;
            this.pauseSecondsLeft = pauseSeconds;
        }

        int totalRounds() {
            return questions.size();
        }

        List<ScoreEntry> scoreboard() {
            return scores.values().stream()
                    .sorted(Comparator.comparingInt((Score s) -> s.rank)
                            .thenComparingInt(s -> playerIds.indexOf(s.userId)))
                    .map(Score::toProto)
                    .toList();
        }
    }

    record Answer(String optionId, int responseTimeMs) {
    }

    static final class Score {
        final String userId;
        final String displayName;
        int total;
        int correct;
        int rank = 1;
        int lastRound;

        Score(String userId, String displayName) {
            this.userId = userId;
            this.displayName = displayName;
        }

        ScoreEntry toProto() {
            return ScoreEntry.newBuilder()
                    .setUserId(userId)
                    .setDisplayName(displayName)
                    .setTotalPoints(total)
                    .setRank(rank)
                    .setPointsLastRound(lastRound)
                    .setCorrectAnswers(correct)
                    .build();
        }
    }

    /** A vote about a disconnected player. */
    static final class VoteState {
        final String aboutUserId;
        final Instant deadline;
        final boolean pauseAllowed;
        final Map<String, VoteOption> votes = new LinkedHashMap<>();

        VoteState(String aboutUserId, Instant deadline, boolean pauseAllowed) {
            this.aboutUserId = aboutUserId;
            this.deadline = deadline;
            this.pauseAllowed = pauseAllowed;
        }

        long count(VoteOption option) {
            return votes.values().stream().filter(option::equals).count();
        }

        Vote toProto() {
            return Vote.newBuilder()
                    .setAboutUserId(aboutUserId)
                    .setDeadline(timestamp(deadline))
                    .setPauseVotes((int) count(VoteOption.VOTE_OPTION_PAUSE))
                    .setContinueVotes((int) count(VoteOption.VOTE_OPTION_CONTINUE_WITHOUT_PLAYER))
                    .setEndVotes((int) count(VoteOption.VOTE_OPTION_END_QUIZ))
                    .addAllVotedUserIds(votes.keySet())
                    .setPauseAllowed(pauseAllowed)
                    .build();
        }
    }
}
