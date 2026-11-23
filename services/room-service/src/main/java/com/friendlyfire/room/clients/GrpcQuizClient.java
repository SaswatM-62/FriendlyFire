package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.quiz.v1.GenerateQuizRequest;
import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizPlayer;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizServiceGrpc.QuizServiceBlockingStub;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class GrpcQuizClient implements QuizClient {

    private final QuizServiceBlockingStub quiz;

    public GrpcQuizClient(QuizServiceBlockingStub quiz) {
        this.quiz = quiz;
    }

    @Override
    public List<Question> generate(
            List<QuizPlayer> players, int questionCount, List<String> askedTemplateIds, Duration deadline) {
        return quiz.withDeadlineAfter(deadline.toMillis(), TimeUnit.MILLISECONDS)
                .generateQuiz(GenerateQuizRequest.newBuilder()
                        .addAllPlayers(players)
                        .setQuestionCount(questionCount)
                        // 0: the quiz engine picks a random seed.
                        .setSeed(0)
                        .addAllAskedTemplateIds(askedTemplateIds)
                        .build())
                .getQuestionsList();
    }
}
