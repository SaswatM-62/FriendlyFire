package com.friendlyfire.room.quiz;

import org.springframework.stereotype.Component;

@Component
public class QuizRunner {
    // TODO(ff): run a started quiz from countdown to final standings
    //   - phases STARTING -> QUESTION -> REVEAL -> ... -> FINISHED, with deadlines from
    //     the injected Clock; after a reveal, VOTE (and PAUSED) if someone is
    //     disconnected (ADR-026)
    //   - pause_seconds_left starts at 180 for each quiz
    //   - grade answers, then ScoringService.RecordRound after each round and
    //     ScoringService.FinalizeQuiz at the end
    //   - publish QUESTION_STARTED, QUESTION_REVEALED, QUIZ_FINISHED... (RoomEventPublisher)
}
