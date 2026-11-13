package com.friendlyfire.gateway.testsupport;

import com.friendlyfire.proto.friendlyfire.scoring.v1.EndReason;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetPlayerHistoryResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.GetQuizResultResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.QuizSummary;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoreboardEntry;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc;
import com.google.protobuf.Timestamp;
import io.grpc.stub.StreamObserver;

/** A fake ScoringService: every quiz was played by user-maya and user-sam. */
public final class FakeScoringService extends ScoringServiceGrpc.ScoringServiceImplBase {

    public final FakeCalls calls = new FakeCalls();

    public static QuizSummary quiz(String quizId) {
        return QuizSummary.newBuilder()
                .setQuizId(quizId)
                .setStartedAt(Timestamp.newBuilder().setSeconds(1_762_700_000L))
                .setFinishedAt(Timestamp.newBuilder().setSeconds(1_762_700_300L))
                .setTotalRounds(10)
                .setRoundsPlayed(10)
                .setEndReason(EndReason.END_REASON_COMPLETED)
                .addFinalScoreboard(ScoreboardEntry.newBuilder()
                        .setUserId("user-maya").setDisplayName("Maya").setTotalPoints(3180).setCorrectAnswers(6).setRank(1))
                .addFinalScoreboard(ScoreboardEntry.newBuilder()
                        .setUserId("user-sam").setDisplayName("Sam").setTotalPoints(2400).setCorrectAnswers(5).setRank(2))
                .build();
    }

    @Override
    public void getPlayerHistory(GetPlayerHistoryRequest request, StreamObserver<GetPlayerHistoryResponse> observer) {
        calls.handle(request, GetPlayerHistoryResponse.newBuilder().addQuizzes(quiz("quiz-1")).build(), observer);
    }

    @Override
    public void getQuizResult(GetQuizResultRequest request, StreamObserver<GetQuizResultResponse> observer) {
        calls.handle(request, GetQuizResultResponse.newBuilder().setSummary(quiz(request.getQuizId())).build(), observer);
    }
}
