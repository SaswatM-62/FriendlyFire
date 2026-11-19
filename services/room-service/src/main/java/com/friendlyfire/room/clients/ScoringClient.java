package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest;

/** ScoringService: it owns points and stores quizzes. Each call throws a StatusRuntimeException on failure. */
public interface ScoringClient {

    void startQuiz(StartQuizRequest request);

    RecordRoundResponse recordRound(RecordRoundRequest request);

    FinalizeQuizResponse finalizeQuiz(FinalizeQuizRequest request);
}
