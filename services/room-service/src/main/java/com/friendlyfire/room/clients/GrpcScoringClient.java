package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class GrpcScoringClient implements ScoringClient {

    private final ScoringServiceBlockingStub scoring;
    private final Duration deadline;

    public GrpcScoringClient(ScoringServiceBlockingStub scoring, Duration deadline) {
        this.scoring = scoring;
        this.deadline = deadline;
    }

    @Override
    public void startQuiz(StartQuizRequest request) {
        stub().startQuiz(request);
    }

    @Override
    public RecordRoundResponse recordRound(RecordRoundRequest request) {
        return stub().recordRound(request);
    }

    @Override
    public FinalizeQuizResponse finalizeQuiz(FinalizeQuizRequest request) {
        return stub().finalizeQuiz(request);
    }

    private ScoringServiceBlockingStub stub() {
        return scoring.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS);
    }
}
