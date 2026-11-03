
package com.friendlyfire.room.room;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.room.v1.ConnectionQuality;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerResult;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionOption;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionView;
import com.friendlyfire.proto.friendlyfire.room.v1.Reveal;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.ScoreEntry;
import com.friendlyfire.proto.friendlyfire.room.v1.Vote;
import com.google.protobuf.Timestamp;

public class Room {

    private final String code;
    private String hostUserId;
    private RoomPhase phase;
    private RoomSettings settings;

    private final Map<String, Player> players = new LinkedHashMap<>();

    private String quizId;
    private int roundNumber;

    private final List<Question> questions = new ArrayList<>();
    private Question currentQuestion;

    private final List<ScoreEntry> scoreboard = new ArrayList<>();
    private final List<PlayerResult> currentResults = new ArrayList<>();

    private Vote activeVote;
    private Timestamp phaseDeadline;

    public Room(String code, String hostUserId, String hostDisplayName) {
        this.code = code;
        this.hostUserId = hostUserId;
        this.phase = RoomPhase.ROOM_PHASE_LOBBY;

        this.settings = RoomSettings.newBuilder()
                .setQuestionCount(10)
                .setSecondsPerQuestion(20)
                .build();

        Player host = Player.newBuilder()
                .setUserId(hostUserId)
                .setDisplayName(hostDisplayName)
                .setStatus(com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus.PLAYER_STATUS_CONNECTED)
                .setMatchDataReady(false)
                .setConnectionQuality(ConnectionQuality.CONNECTION_QUALITY_UNSPECIFIED)
                .setAnsweredCurrentQuestion(false)
                .build();

        players.put(hostUserId, host);
        roundNumber = 0;
    }

    public String getCode() {
        return code;
    }

    public synchronized String getHostUserId() {
        return hostUserId;
    }

    public synchronized void setHostUserId(String hostUserId) {
        this.hostUserId = hostUserId;
    }

    public synchronized RoomPhase getPhase() {
        return phase;
    }

    public synchronized void setPhase(RoomPhase phase) {
        this.phase = phase;
    }

    public synchronized RoomSettings getSettings() {
        return settings;
    }

    public synchronized void setSettings(RoomSettings settings) {
        if (settings.getQuestionCount() < 5 || settings.getQuestionCount() > 20) {
            throw new IllegalArgumentException("Question count must be between 5 and 20.");
        }

        if (settings.getSecondsPerQuestion() < 10 || settings.getSecondsPerQuestion() > 45) {
            throw new IllegalArgumentException("Seconds per question must be between 10 and 45.");
        }

        this.settings = settings;
    }

    public synchronized Map<String, Player> getPlayers() {
        return new LinkedHashMap<>(players);
    }

    public synchronized boolean addPlayer(Player player) {
        if (players.size() >= 8 || players.containsKey(player.getUserId())) {
            return false;
        }

        players.put(player.getUserId(), player);
        return true;
    }

    public synchronized boolean removePlayer(String userId) {
        if (userId.equals(hostUserId)) {
            return false;
        }

        return players.remove(userId) != null;
    }

    public synchronized String getQuizId() {
        return quizId;
    }

    public synchronized void setQuizId(String quizId) {
        this.quizId = quizId;
    }

    public synchronized int getRoundNumber() {
        return roundNumber;
    }

    public synchronized void setRoundNumber(int roundNumber) {
        this.roundNumber = roundNumber;
    }

    public synchronized List<Question> getQuestions() {
        return new ArrayList<>(questions);
    }

    public synchronized void setQuestions(List<Question> questions) {
        this.questions.clear();
        this.questions.addAll(questions);
    }

    public synchronized Question getCurrentQuestion() {
        return currentQuestion;
    }

    public synchronized void setCurrentQuestion(Question currentQuestion) {
        this.currentQuestion = currentQuestion;
    }

    public synchronized List<ScoreEntry> getScoreboard() {
        return new ArrayList<>(scoreboard);
    }

    public synchronized void setScoreboard(List<ScoreEntry> scoreboard) {
        this.scoreboard.clear();
        this.scoreboard.addAll(scoreboard);
    }

    public synchronized List<PlayerResult> getCurrentResults() {
        return new ArrayList<>(currentResults);
    }

    public synchronized void setCurrentResults(List<PlayerResult> results) {
        this.currentResults.clear();
        this.currentResults.addAll(results);
    }

    public synchronized Vote getActiveVote() {
        return activeVote;
    }

    public synchronized void setActiveVote(Vote activeVote) {
        this.activeVote = activeVote;
    }

    public synchronized Timestamp getPhaseDeadline() {
        return phaseDeadline;
    }

    public synchronized void setPhaseDeadline(Timestamp phaseDeadline) {
        this.phaseDeadline = phaseDeadline;
    }

    public synchronized RoomSnapshot buildSnapshot() {
        RoomSnapshot.Builder snapshot = RoomSnapshot.newBuilder()
                .setRoomCode(code)
                .setHostUserId(hostUserId)
                .setPhase(phase)
                .setSettings(settings)
                .setQuizId(quizId == null ? "" : quizId)
                .setRoundNumber(roundNumber)
                .addAllPlayers(players.values())
                .addAllScoreboard(scoreboard);

        if (phaseDeadline != null) {
            snapshot.setPhaseDeadline(phaseDeadline);
        }

        if (activeVote != null) {
            snapshot.setActiveVote(activeVote);
        }

        if (currentQuestion != null) {
            QuestionView.Builder questionView = QuestionView.newBuilder()
                    .setQuestionId(currentQuestion.getQuestionId())
                    .setText(currentQuestion.getText());

            for (var option : currentQuestion.getOptionsList()) {
                questionView.addOptions(
                        QuestionOption.newBuilder()
                                .setOptionId(option.getOptionId())
                                .setLabel(option.getLabel())
                                .build()
                );
            }

            if (phase == RoomPhase.ROOM_PHASE_REVEAL) {
                Reveal.Builder reveal = Reveal.newBuilder()
                        .setCorrectOptionId(currentQuestion.getCorrectOptionId())
                        .setExplanation(currentQuestion.getExplanation())
                        .addAllResults(currentResults);

                questionView.setReveal(reveal);
            }

            snapshot.setCurrentQuestion(questionView);
        }

        return snapshot.build();
    }
}
