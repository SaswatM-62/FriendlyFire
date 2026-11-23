package com.friendlyfire.account.scoring;

import com.friendlyfire.proto.friendlyfire.scoring.v1.AnonymizePlayerRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Component;

/** ScoringClient over gRPC (SCORING_GRPC_ADDR). */
@Component
public class GrpcScoringClient implements ScoringClient {

    static final long DEADLINE_SECONDS = 10;

    private final ScoringServiceBlockingStub scoring;

    public GrpcScoringClient(ScoringServiceBlockingStub scoring) {
        this.scoring = scoring;
    }

    @Override
    public void anonymizePlayer(UUID userId) {
        try {
            scoring.withDeadlineAfter(DEADLINE_SECONDS, TimeUnit.SECONDS)
                    .anonymizePlayer(AnonymizePlayerRequest.newBuilder().setUserId(userId.toString()).build());
        } catch (StatusRuntimeException e) {
            throw Status.UNAVAILABLE
                    .withDescription("Quiz history isn't available right now. Try again in a few minutes.")
                    .withCause(e)
                    .asRuntimeException();
        }
    }
}
