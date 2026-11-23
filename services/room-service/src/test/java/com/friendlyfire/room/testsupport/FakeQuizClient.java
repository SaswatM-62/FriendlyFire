package com.friendlyfire.room.testsupport;

import com.friendlyfire.proto.friendlyfire.quiz.v1.AnswerOption;
import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizPlayer;
import com.friendlyfire.room.clients.QuizClient;
import io.grpc.StatusRuntimeException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The quiz engine: question N's answer is the Nth player (round robin), and its template
 * is "template-N". Remembers every request.
 */
public final class FakeQuizClient implements QuizClient {

    public record Request(List<QuizPlayer> players, int questionCount, List<String> askedTemplateIds) {
    }

    public final List<Request> requests = new ArrayList<>();
    private StatusRuntimeException failure;
    private int quizzes;

    public void failWith(StatusRuntimeException failure) {
        this.failure = failure;
    }

    @Override
    public synchronized List<Question> generate(
            List<QuizPlayer> players, int questionCount, List<String> askedTemplateIds, Duration deadline) {
        requests.add(new Request(List.copyOf(players), questionCount, List.copyOf(askedTemplateIds)));
        if (failure != null) {
            throw failure;
        }
        quizzes++;
        List<Question> questions = new ArrayList<>();
        for (int n = 1; n <= questionCount; n++) {
            Question.Builder question = Question.newBuilder()
                    .setQuestionId("quiz" + quizzes + "-q" + n)
                    .setText("Question " + n + "?")
                    .setCorrectOptionId(players.get((n - 1) % players.size()).getUserId())
                    .setExplanation("Because of match " + n + ".")
                    .setTemplateId("template-" + n);
            players.forEach(p -> question.addOptions(
                    AnswerOption.newBuilder().setOptionId(p.getUserId()).setLabel(p.getDisplayName())));
            questions.add(question.build());
        }
        return questions;
    }
}
