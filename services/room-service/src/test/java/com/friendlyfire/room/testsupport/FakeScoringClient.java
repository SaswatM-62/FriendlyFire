package com.friendlyfire.room.testsupport;

import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.PlayerAnswer;
import com.friendlyfire.proto.friendlyfire.scoring.v1.QuizSummary;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RoundPoints;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoreboardEntry;
import com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest;
import com.friendlyfire.room.clients.ScoringClient;
import io.grpc.Status;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * ScoringService in memory, with the contract's points (500 + 500 × time left / limit)
 * and shared ranks. Remembers every call; can be switched off.
 */
public final class FakeScoringClient implements ScoringClient {

    public final List<StartQuizRequest> started = new ArrayList<>();
    public final List<RecordRoundRequest> rounds = new ArrayList<>();
    public final List<FinalizeQuizRequest> finalized = new ArrayList<>();
    private final Map<String, StartQuizRequest> quizzes = new HashMap<>();
    private final Map<String, Map<Integer, Map<String, Integer>>> points = new HashMap<>();
    private final Map<String, Map<Integer, Map<String, Boolean>>> correct = new HashMap<>();
    private volatile boolean down;

    public void down(boolean down) {
        this.down = down;
    }

    @Override
    public synchronized void startQuiz(StartQuizRequest request) {
        check();
        started.add(request);
        quizzes.put(request.getQuizId(), request);
    }

    @Override
    public synchronized RecordRoundResponse recordRound(RecordRoundRequest request) {
        check();
        rounds.add(request);
        StartQuizRequest quiz = quizzes.get(request.getQuizId());
        int limitMs = quiz.getSecondsPerQuestion() * 1000;
        Map<String, Integer> round = new LinkedHashMap<>();
        Map<String, Boolean> rights = new LinkedHashMap<>();
        for (PlayerAnswer a : request.getAnswersList()) {
            int earned = a.getAnswered() && a.getCorrect()
                    ? (int) Math.round(500 + 500.0 * Math.max(0, limitMs - a.getResponseTimeMs()) / limitMs)
                    : 0;
            round.put(a.getUserId(), earned);
            rights.put(a.getUserId(), a.getAnswered() && a.getCorrect());
        }
        points.computeIfAbsent(request.getQuizId(), id -> new HashMap<>()).put(request.getRoundNumber(), round);
        correct.computeIfAbsent(request.getQuizId(), id -> new HashMap<>()).put(request.getRoundNumber(), rights);
        RecordRoundResponse.Builder response = RecordRoundResponse.newBuilder();
        round.forEach((user, p) -> response.addRoundPoints(RoundPoints.newBuilder().setUserId(user).setPoints(p)));
        response.addAllScoreboard(scoreboard(request.getQuizId()));
        return response.build();
    }

    @Override
    public synchronized FinalizeQuizResponse finalizeQuiz(FinalizeQuizRequest request) {
        check();
        finalized.add(request);
        return FinalizeQuizResponse.newBuilder()
                .setSummary(QuizSummary.newBuilder()
                        .setQuizId(request.getQuizId())
                        .setEndReason(request.getEndReason())
                        .addAllFinalScoreboard(scoreboard(request.getQuizId())))
                .build();
    }

    private List<ScoreboardEntry> scoreboard(String quizId) {
        Map<String, Integer> totals = new LinkedHashMap<>();
        Map<String, Integer> rights = new LinkedHashMap<>();
        quizzes.get(quizId).getPlayersList().forEach(p -> {
            totals.put(p.getUserId(), 0);
            rights.put(p.getUserId(), 0);
        });
        points.getOrDefault(quizId, Map.of()).values().forEach(r -> r.forEach((u, p) -> totals.merge(u, p, Integer::sum)));
        correct.getOrDefault(quizId, Map.of()).values()
                .forEach(r -> r.forEach((u, c) -> rights.merge(u, c ? 1 : 0, Integer::sum)));
        List<ScoreboardEntry> board = new ArrayList<>();
        totals.forEach((user, total) -> board.add(ScoreboardEntry.newBuilder()
                .setUserId(user)
                .setTotalPoints(total)
                .setCorrectAnswers(rights.get(user))
                .setRank(1 + (int) totals.values().stream().filter(t -> t > total).count())
                .build()));
        return board;
    }

    private void check() {
        if (down) {
            throw Status.UNAVAILABLE.withDescription("scoring is down").asRuntimeException();
        }
    }
}
