package com.friendlyfire.room.quiz;

import org.springframework.stereotype.Component;

@Component
public class QuizRunner {
    // TODO(ff): run a started quiz from countdown to final standings
    //   - phases (PREPARING ->) STARTING -> QUESTION -> REVEAL -> ... -> FINISHED, with
    //     deadlines from the injected Clock; after a reveal, or straight after
    //     PREPARING, VOTE (and PAUSED) if someone is disconnected (ADR-026)
    //   - SPECTATING players aren't options, can't answer or vote and have no score;
    //     when the quiz finishes they become CONNECTED and play from the next quiz
    //   - pause_seconds_left starts at 180 for each quiz
    //   - grade answers, then ScoringService.RecordRound after each round and
    //     ScoringService.FinalizeQuiz at the end
    //   - publish QUESTION_STARTED, QUESTION_REVEALED, QUIZ_FINISHED... (RoomEventPublisher)
}
