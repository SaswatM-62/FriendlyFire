package com.friendlyfire.room.clients;

import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizPlayer;
import java.time.Duration;
import java.util.List;

/** QuizService.GenerateQuiz. */
public interface QuizClient {

    /**
     * Every question for a quiz. Throws a StatusRuntimeException on failure:
     * FAILED_PRECONDITION (with a message for players) when there isn't enough match data.
     */
    List<Question> generate(List<QuizPlayer> players, int questionCount, List<String> askedTemplateIds,
            Duration deadline);
}
