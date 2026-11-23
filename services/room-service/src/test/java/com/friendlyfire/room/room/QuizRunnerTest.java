package com.friendlyfire.room.room;

import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_ANSWERED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_DISCONNECTED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_JOINED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_LEFT;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_PLAYER_RECONNECTED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_QUESTION_REVEALED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_QUESTION_STARTED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_QUIZ_FINISHED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_QUIZ_PREPARING;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_QUIZ_STARTING;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_QUIZ_START_FAILED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_SNAPSHOT;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_VOTE_CAST;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_VOTE_RESOLVED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType.ROOM_EVENT_TYPE_VOTE_STARTED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_FINISHED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_LOBBY;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_PAUSED;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_PREPARING;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_QUESTION;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_REVEAL;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_STARTING;
import static com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase.ROOM_PHASE_VOTE;
import static com.friendlyfire.proto.friendlyfire.room.v1.VoteOption.VOTE_OPTION_CONTINUE_WITHOUT_PLAYER;
import static com.friendlyfire.proto.friendlyfire.room.v1.VoteOption.VOTE_OPTION_END_QUIZ;
import static com.friendlyfire.proto.friendlyfire.room.v1.VoteOption.VOTE_OPTION_PAUSE;
import static com.friendlyfire.room.testsupport.RoomFixture.fails;
import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizPlayer;
import com.friendlyfire.proto.friendlyfire.room.v1.EndReason;
import com.friendlyfire.proto.friendlyfire.room.v1.Player;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.QuestionOption;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEvent;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSettings;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomSnapshot;
import com.friendlyfire.proto.friendlyfire.room.v1.ScoreEntry;
import com.friendlyfire.room.testsupport.FakeDataStatusClient;
import com.friendlyfire.room.testsupport.RecordingStream;
import com.friendlyfire.room.testsupport.RoomFixture;
import io.grpc.Status;
import io.grpc.Status.Code;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class QuizRunnerTest {

    private final RoomFixture f = new RoomFixture();

    /** Alice hosts; Bob (and Cara) join; 5 questions of 20 seconds. */
    private String room(String... others) {
        String code = f.create("alice");
        for (String other : others) {
            f.join(code, other);
        }
        f.manager.updateSettings(code, "alice", RoomSettings.newBuilder()
                .setQuestionCount(5).setSecondsPerQuestion(20).build());
        return code;
    }

    private RoomSnapshot snapshot(String code) {
        return f.snapshot(code);
    }

    private Player player(String code, String userId) {
        return snapshot(code).getPlayersList().stream()
                .filter(p -> p.getUserId().equals(userId)).findFirst().orElseThrow();
    }

    private String questionId(String code) {
        return snapshot(code).getCurrentQuestion().getQuestionId();
    }

    /** The fake quiz engine's answer for question N among these players: the Nth in join order. */
    private static String answerFor(int round, String... players) {
        return players[(round - 1) % players.length];
    }

    /** Starts and waits out the loading screen and the countdown: question 1 is open. */
    private void startAndReachQuestion1(String code) {
        f.manager.startQuiz(code, "alice");
        f.advanceSeconds(8);
        f.advanceSeconds(3);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_QUESTION);
    }

    // ---------- Start (FF-416) ----------

    @Test
    void startingAloneTellsTheHostWhy() {
        String code = room();
        String message = fails(Code.FAILED_PRECONDITION, () -> f.manager.startQuiz(code, "alice"));
        assertThat(message).isEqualTo("You need at least 2 players to start.");
    }

    @Test
    void onlyTheHostCanStart() {
        String code = room("bob");
        fails(Code.PERMISSION_DENIED, () -> f.manager.startQuiz(code, "bob"));
    }

    @Test
    void everyoneMustBeSyncedAndTheMessageSaysWhoIsStillSyncing() {
        String code = room("bob", "cara");
        f.data.set("bob", FakeDataStatusClient.syncing(90));
        f.data.set("cara", FakeDataStatusClient.syncing(30));

        String message = fails(Code.FAILED_PRECONDITION, () -> f.manager.startQuiz(code, "alice"));

        assertThat(message).startsWith("Waiting for Bob and Cara: everyone needs a linked Riot account");
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_LOBBY);
    }

    @Test
    void atLeastTwoPlayersNeedEnoughMatches() {
        String code = room("bob", "cara");
        f.data.set("bob", FakeDataStatusClient.FEW_MATCHES);
        f.data.set("cara", FakeDataStatusClient.FEW_MATCHES);

        String message = fails(Code.FAILED_PRECONDITION, () -> f.manager.startQuiz(code, "alice"));

        assertThat(message).contains("At least 2 players need 5 or more");
    }

    @Test
    void startingWhileAccountIsDownIsUnavailable() {
        String code = room("bob");
        f.data.down(true);
        fails(Code.UNAVAILABLE, () -> f.manager.startQuiz(code, "alice"));
    }

    @Test
    void startMovesEveryoneToTheLoadingScreenForAtLeastEightSeconds() {
        String code = room("bob");
        RecordingStream stream = f.stream(code);

        RoomSnapshot started = f.manager.startQuiz(code, "alice");

        assertThat(started.getPhase()).isEqualTo(ROOM_PHASE_PREPARING);
        assertThat(started.getQuizId()).isNotEmpty();
        assertThat(started.getPauseSecondsLeft()).isEqualTo(180);
        assertThat(Instant.ofEpochSecond(started.getPhaseDeadline().getSeconds()))
                .isEqualTo(RoomFixture.START.plusSeconds(8));
        assertThat(stream.types()).containsExactly(ROOM_EVENT_TYPE_SNAPSHOT, ROOM_EVENT_TYPE_QUIZ_PREPARING);
        // The questions were ready at once, but the loading screen stays its 8 seconds.
        f.advanceSeconds(7);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_PREPARING);
        f.advanceSeconds(1);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_STARTING);
        assertThat(snapshot(code).getRoundNumber()).isZero();
        assertThat(f.scoring.started).hasSize(1);
        assertThat(f.scoring.started.getFirst().getHostDisplayName()).isEqualTo("Alice");
        assertThat(f.scoring.started.getFirst().getTotalRounds()).isEqualTo(5);
    }

    @Test
    void settingsKicksAndASecondStartAreRefusedWhilePreparing() {
        String code = room("bob", "cara");
        f.manager.startQuiz(code, "alice");

        fails(Code.FAILED_PRECONDITION, () -> f.manager.startQuiz(code, "alice"));
        fails(Code.FAILED_PRECONDITION, () -> f.manager.kickPlayer(code, "alice", "cara"));
        fails(Code.FAILED_PRECONDITION, () -> f.manager.updateSettings(code, "alice", RoomSettings.newBuilder()
                .setQuestionCount(10).setSecondsPerQuestion(20).build()));
    }

    @Test
    void notEnoughMatchDataSendsTheRoomBackToTheLobbyWithTheReason() {
        String code = room("bob");
        f.quiz.failWith(Status.FAILED_PRECONDITION
                .withDescription("There's only enough match data for 3 different questions, not 5. "
                        + "Choose 3 or fewer questions.")
                .asRuntimeException());
        RecordingStream stream = f.stream(code);

        f.manager.startQuiz(code, "alice");

        RoomEvent failed = stream.last();
        assertThat(failed.getType()).isEqualTo(ROOM_EVENT_TYPE_QUIZ_START_FAILED);
        assertThat(failed.getMessage()).startsWith("There's only enough match data for 3");
        assertThat(failed.getSnapshot().getPhase()).isEqualTo(ROOM_PHASE_LOBBY);
        assertThat(failed.getSnapshot().getQuizId()).isEmpty();
    }

    @Test
    void buildingTheQuestionsForOverAMinuteFails() {
        List<Runnable> held = new ArrayList<>();
        RoomFixture slow = new RoomFixture(held::add);
        String code = slow.create("alice");
        slow.join(code, "bob");
        RecordingStream stream = slow.stream(code);
        slow.manager.startQuiz(code, "alice");

        slow.advanceSeconds(59);
        assertThat(slow.snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_PREPARING);
        slow.advanceSeconds(1);

        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_QUIZ_START_FAILED);
        assertThat(stream.last().getMessage()).isEqualTo(QuizRunner.TOO_SLOW);
        // The questions arriving late change nothing.
        held.forEach(Runnable::run);
        assertThat(slow.snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_LOBBY);
    }

    @Test
    void ifScoresCantBeSavedTheQuizDoesntStart() {
        String code = room("bob");
        f.scoring.down(true);
        RecordingStream stream = f.stream(code);

        f.manager.startQuiz(code, "alice");

        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_QUIZ_START_FAILED);
        assertThat(stream.last().getMessage()).isEqualTo(QuizRunner.SCORES_UNAVAILABLE);
    }

    @Test
    void playersWithTheSameNameAreToldApartByTheirRiotTag() {
        String code = f.manager.createRoom("sam1", "Sam", "NA1").getRoomCode();
        f.manager.joinRoom(code, "sam2", "Sam", "EUW");
        f.manager.joinRoom(code, "kim", "Kim", "");

        f.manager.startQuiz(code, "sam1");

        assertThat(f.quiz.requests.getFirst().players()).extracting(QuizPlayer::getDisplayName)
                .containsExactly("Sam (NA1)", "Sam (EUW)", "Kim");
    }

    @Test
    void aPlayerWithFewMatchesIsMarkedForTheQuizEngine() {
        String code = room("bob", "cara");
        f.data.set("cara", FakeDataStatusClient.FEW_MATCHES);

        f.manager.startQuiz(code, "alice");

        assertThat(f.quiz.requests.getFirst().players()).extracting(QuizPlayer::getNotEnoughMatches)
                .containsExactly(false, false, true);
    }

    // ---------- A whole quiz (FF-417, FF-418) ----------

    @Test
    void aFullFiveQuestionQuizRunsFromCountdownToFinalStandings() {
        String code = room("bob");
        RecordingStream stream = f.stream(code);
        startAndReachQuestion1(code);

        for (int round = 1; round <= 5; round++) {
            RoomSnapshot question = snapshot(code);
            assertThat(question.getRoundNumber()).isEqualTo(round);
            assertThat(question.getCurrentQuestion().hasReveal()).isFalse();
            String correct = answerFor(round, "alice", "bob");
            f.advanceSeconds(5);
            // Alice always right after 5 s; Bob always picks himself after 10 s.
            f.manager.submitAnswer(code, "alice", questionId(code), correct);
            f.advanceSeconds(5);
            f.manager.submitAnswer(code, "bob", questionId(code), "bob");
            // Both answered: the reveal comes early.
            RoomSnapshot reveal = snapshot(code);
            assertThat(reveal.getPhase()).isEqualTo(ROOM_PHASE_REVEAL);
            assertThat(reveal.getCurrentQuestion().getReveal().getCorrectOptionId()).isEqualTo(correct);
            f.advanceSeconds(10);
            if (round < 5) {
                // Get ready for the next question: still the round just played.
                assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_STARTING);
                assertThat(snapshot(code).getRoundNumber()).isEqualTo(round);
                f.advanceSeconds(3);
            }
        }

        RoomSnapshot finished = snapshot(code);
        assertThat(finished.getPhase()).isEqualTo(ROOM_PHASE_FINISHED);
        assertThat(finished.getEndReason()).isEqualTo(EndReason.END_REASON_COMPLETED);
        assertThat(finished.hasPhaseDeadline()).isFalse();
        // Alice: 875 a round (right after 5 of 20 s). Bob is right in rounds 2 and 4: 750 each (after 10 s).
        ScoreEntry alice = finished.getScoreboard(0);
        assertThat(alice.getUserId()).isEqualTo("alice");
        assertThat(alice.getTotalPoints()).isEqualTo(5 * 875);
        assertThat(alice.getCorrectAnswers()).isEqualTo(5);
        assertThat(alice.getRank()).isEqualTo(1);
        ScoreEntry bob = finished.getScoreboard(1);
        assertThat(bob.getTotalPoints()).isEqualTo(2 * 750);
        assertThat(bob.getCorrectAnswers()).isEqualTo(2);
        assertThat(finished.getFastestAnswer().getUserId()).isEqualTo("alice");
        assertThat(finished.getFastestAnswer().getResponseTimeMs()).isEqualTo(5000);
        // Scoring: once at the start, once per round, once at the end.
        assertThat(f.scoring.started).hasSize(1);
        assertThat(f.scoring.rounds).extracting(r -> r.getRoundNumber()).containsExactly(1, 2, 3, 4, 5);
        assertThat(f.scoring.finalized).hasSize(1);
        assertThat(f.scoring.finalized.getFirst().getRoundsPlayed()).isEqualTo(5);
        assertThat(stream.types()).containsSubsequence(ROOM_EVENT_TYPE_QUIZ_PREPARING, ROOM_EVENT_TYPE_QUIZ_STARTING,
                ROOM_EVENT_TYPE_QUESTION_STARTED, ROOM_EVENT_TYPE_PLAYER_ANSWERED, ROOM_EVENT_TYPE_QUESTION_REVEALED,
                ROOM_EVENT_TYPE_QUIZ_STARTING, ROOM_EVENT_TYPE_QUIZ_FINISHED);
    }

    @Test
    void timeRunningOutRevealsTheAnswerWithoutEveryone() {
        String code = room("bob");
        startAndReachQuestion1(code);
        f.manager.submitAnswer(code, "alice", questionId(code), "alice");

        f.advanceSeconds(19);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_QUESTION);
        f.advanceSeconds(1);

        var reveal = snapshot(code).getCurrentQuestion().getReveal();
        assertThat(reveal.getResultsList()).extracting(r -> r.getUserId() + ":" + r.getPoints() + ":" + r.getChosenOptionId())
                .containsExactly("alice:1000:alice", "bob:0:");
    }

    @Test
    void answersFollowTheRules() {
        String code = room("bob");
        startAndReachQuestion1(code);
        String question = questionId(code);

        fails(Code.FAILED_PRECONDITION, () -> f.manager.submitAnswer(code, "alice", "old-question", "alice"));
        fails(Code.INVALID_ARGUMENT, () -> f.manager.submitAnswer(code, "alice", question, "nobody"));
        fails(Code.PERMISSION_DENIED, () -> f.manager.submitAnswer(code, "mallory", question, "alice"));
        f.manager.submitAnswer(code, "alice", question, "bob");
        fails(Code.ALREADY_EXISTS, () -> f.manager.submitAnswer(code, "alice", question, "alice"));
    }

    @Test
    void anAnswerAfterTheRevealIsTooLate() {
        String code = room("bob");
        startAndReachQuestion1(code);
        String question = questionId(code);
        f.advanceSeconds(20);

        fails(Code.FAILED_PRECONDITION, () -> f.manager.submitAnswer(code, "bob", question, "alice"));
    }

    @Test
    void noSnapshotOrEventShowsTheAnswerBeforeItsReveal() {
        String code = room("bob");
        RecordingStream stream = f.stream(code);
        startAndReachQuestion1(code);
        f.manager.submitAnswer(code, "alice", questionId(code), "alice");
        f.advanceSeconds(20);

        for (RoomEvent event : stream.events) {
            RoomSnapshot s = event.getSnapshot();
            if (s.getPhase() != ROOM_PHASE_REVEAL) {
                assertThat(s.getCurrentQuestion().hasReveal()).as(event.getType().name()).isFalse();
            }
        }
    }

    @Test
    void playingAgainPassesTheTemplatesAlreadyAsked() {
        String code = room("bob");
        startAndReachQuestion1(code);
        for (int round = 1; round <= 5; round++) {
            f.advanceSeconds(20 + 10 + 3);
        }
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_FINISHED);

        f.manager.startQuiz(code, "alice");

        assertThat(f.quiz.requests).hasSize(2);
        assertThat(f.quiz.requests.get(1).askedTemplateIds())
                .containsExactly("template-1", "template-2", "template-3", "template-4", "template-5");
        assertThat(snapshot(code).getEndReason()).isEqualTo(EndReason.END_REASON_UNSPECIFIED);
    }

    @Test
    void settingsAndKicksWorkAfterAQuiz() {
        String code = room("bob", "cara");
        startAndReachQuestion1(code);
        for (int round = 1; round <= 5; round++) {
            f.advanceSeconds(20 + 10 + 3);
        }

        f.manager.updateSettings(code, "alice", RoomSettings.newBuilder()
                .setQuestionCount(8).setSecondsPerQuestion(15).build());
        RoomSnapshot room = f.manager.kickPlayer(code, "alice", "cara");

        assertThat(room.getPhase()).isEqualTo(ROOM_PHASE_FINISHED);
        assertThat(room.getSettings().getQuestionCount()).isEqualTo(8);
        assertThat(room.getPlayersList()).extracting(Player::getUserId).containsExactly("alice", "bob");
    }

    @Test
    void whenScoringIsDownMidQuizTheRoomScoresTheRoundItself() {
        String code = room("bob");
        startAndReachQuestion1(code);
        f.scoring.down(true);
        f.advanceSeconds(10);
        f.manager.submitAnswer(code, "alice", questionId(code), "alice");
        f.manager.submitAnswer(code, "bob", questionId(code), "bob");

        RoomSnapshot reveal = snapshot(code);

        assertThat(reveal.getPhase()).isEqualTo(ROOM_PHASE_REVEAL);
        assertThat(reveal.getScoreboard(0).getTotalPoints()).isEqualTo(750);
        assertThat(reveal.getScoreboard(1).getTotalPoints()).isZero();
    }

    // ---------- Spectators ----------

    @Test
    void someoneJoiningMidQuizWatchesThenPlaysTheNextOne() {
        String code = room("bob");
        f.manager.startQuiz(code, "alice");
        RecordingStream stream = f.stream(code);

        RoomSnapshot joined = f.join(code, "cara");

        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_PLAYER_JOINED);
        assertThat(player(code, "cara").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_SPECTATING);
        assertThat(joined.getScoreboardList()).extracting(ScoreEntry::getUserId).containsExactly("alice", "bob");
        f.advanceSeconds(11);
        assertThat(snapshot(code).getCurrentQuestion().getOptionsList()).extracting(QuestionOption::getOptionId)
                .containsExactly("alice", "bob");
        String message = fails(Code.FAILED_PRECONDITION,
                () -> f.manager.submitAnswer(code, "cara", questionId(code), "alice"));
        assertThat(message).contains("watching");
        // Alice and Bob answering is everyone: Cara doesn't hold up the reveal.
        f.manager.submitAnswer(code, "alice", questionId(code), "alice");
        f.manager.submitAnswer(code, "bob", questionId(code), "alice");
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_REVEAL);

        f.manager.leaveRoom(code, "bob");

        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_FINISHED);
        assertThat(snapshot(code).getEndReason()).isEqualTo(EndReason.END_REASON_NOT_ENOUGH_PLAYERS);
        assertThat(player(code, "cara").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_CONNECTED);
    }

    // ---------- Disconnects, votes and pauses (FF-419, ADR-026) ----------

    /** Alice, Bob and Cara are on question 1; Cara drops; the question finishes and the room votes. */
    private String voteAboutCara(RecordingStream[] stream) {
        String code = room("bob", "cara");
        startAndReachQuestion1(code);
        f.manager.reportDisconnected(code, "cara");
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_QUESTION);
        stream[0] = f.stream(code);
        f.manager.submitAnswer(code, "alice", questionId(code), "alice");
        f.manager.submitAnswer(code, "bob", questionId(code), "alice");
        // Cara's gone, so Alice and Bob are everyone: the reveal comes early.
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_REVEAL);
        f.advanceSeconds(10);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_VOTE);
        return code;
    }

    @Test
    void afterTheRevealTheRoomVotesAboutSomeoneWhoDropped() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);

        var vote = snapshot(code).getActiveVote();
        assertThat(vote.getAboutUserId()).isEqualTo("cara");
        assertThat(vote.getPauseAllowed()).isTrue();
        assertThat(stream[0].last().getType()).isEqualTo(ROOM_EVENT_TYPE_VOTE_STARTED);
        assertThat(stream[0].last().getSubjectUserId()).isEqualTo("cara");
    }

    @Test
    void votingToContinueSitsThePlayerOutAndCarriesOn() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);

        f.manager.castVote(code, "alice", VOTE_OPTION_CONTINUE_WITHOUT_PLAYER);
        assertThat(stream[0].last().getType()).isEqualTo(ROOM_EVENT_TYPE_VOTE_CAST);
        assertThat(snapshot(code).getActiveVote().getVotedUserIdsList()).containsExactly("alice");
        fails(Code.ALREADY_EXISTS, () -> f.manager.castVote(code, "alice", VOTE_OPTION_END_QUIZ));
        fails(Code.FAILED_PRECONDITION, () -> f.manager.castVote(code, "cara", VOTE_OPTION_PAUSE));
        f.manager.castVote(code, "bob", VOTE_OPTION_CONTINUE_WITHOUT_PLAYER);

        assertThat(stream[0].last().getType()).isEqualTo(ROOM_EVENT_TYPE_VOTE_RESOLVED);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_STARTING);
        assertThat(player(code, "cara").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_SITTING_OUT);
        f.advanceSeconds(3);
        assertThat(snapshot(code).getRoundNumber()).isEqualTo(2);
    }

    @Test
    void votingToEndShowsTheFinalStandingsWithTheScoresSoFar() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);

        f.manager.castVote(code, "alice", VOTE_OPTION_END_QUIZ);
        f.manager.castVote(code, "bob", VOTE_OPTION_END_QUIZ);

        RoomSnapshot finished = snapshot(code);
        assertThat(finished.getPhase()).isEqualTo(ROOM_PHASE_FINISHED);
        assertThat(finished.getEndReason()).isEqualTo(EndReason.END_REASON_VOTED_TO_END);
        assertThat(finished.getScoreboard(0).getTotalPoints()).isPositive();
        assertThat(f.scoring.finalized.getFirst().getRoundsPlayed()).isEqualTo(1);
        assertThat(f.scoring.finalized.getFirst().getEndReason().getNumber())
                .isEqualTo(EndReason.END_REASON_VOTED_TO_END.getNumber());
    }

    @Test
    void aTiePausesForAMinuteAndAnotherVoteFollowsIfTheyStayAway() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);
        f.manager.castVote(code, "alice", VOTE_OPTION_CONTINUE_WITHOUT_PLAYER);
        f.manager.castVote(code, "bob", VOTE_OPTION_END_QUIZ);

        RoomSnapshot paused = snapshot(code);
        assertThat(paused.getPhase()).isEqualTo(ROOM_PHASE_PAUSED);
        assertThat(paused.getWaitingForUserId()).isEqualTo("cara");
        assertThat(paused.getPauseSecondsLeft()).isEqualTo(120);
        f.advanceSeconds(60);

        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_VOTE);
        assertThat(stream[0].last().getType()).isEqualTo(ROOM_EVENT_TYPE_VOTE_STARTED);
    }

    @Test
    void nobodyVotingIsATieThatPauses() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);

        f.advanceSeconds(15);

        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_PAUSED);
    }

    @Test
    void comingBackDuringThePauseCarriesOnAndGivesBackTheUnusedTime() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);
        f.manager.castVote(code, "alice", VOTE_OPTION_PAUSE);
        f.manager.castVote(code, "bob", VOTE_OPTION_PAUSE);
        f.advanceSeconds(20);

        f.join(code, "cara");

        RoomSnapshot room = snapshot(code);
        assertThat(room.getPhase()).isEqualTo(ROOM_PHASE_STARTING);
        assertThat(room.getPauseSecondsLeft()).isEqualTo(160);
        assertThat(stream[0].last().getType()).isEqualTo(ROOM_EVENT_TYPE_PLAYER_RECONNECTED);
        assertThat(player(code, "cara").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_CONNECTED);
    }

    @Test
    void comingBackDuringTheVoteCancelsIt() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);

        f.join(code, "cara");

        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_STARTING);
        assertThat(snapshot(code).hasActiveVote()).isFalse();
        assertThat(stream[0].last().getType()).isEqualTo(ROOM_EVENT_TYPE_PLAYER_RECONNECTED);
    }

    @Test
    void onceThreeMinutesOfPausesAreUsedOnlyContinueAndEndRemain() {
        RecordingStream[] stream = new RecordingStream[1];
        String code = voteAboutCara(stream);
        for (int pause = 1; pause <= 3; pause++) {
            f.advanceSeconds(15);
            assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_PAUSED);
            f.advanceSeconds(60);
        }

        var vote = snapshot(code).getActiveVote();
        assertThat(vote.getPauseAllowed()).isFalse();
        fails(Code.FAILED_PRECONDITION, () -> f.manager.castVote(code, "alice", VOTE_OPTION_PAUSE));
        // A tie now continues without her.
        f.advanceSeconds(15);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_STARTING);
        assertThat(player(code, "cara").getStatus()).isEqualTo(PlayerStatus.PLAYER_STATUS_SITTING_OUT);
    }

    @Test
    void someoneDroppingWhilePreparingIsVotedOnBeforeTheCountdown() {
        String code = room("bob", "cara");
        f.manager.startQuiz(code, "alice");
        f.manager.reportDisconnected(code, "cara");

        f.advanceSeconds(8);

        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_VOTE);
        assertThat(snapshot(code).getRoundNumber()).isZero();
        f.manager.castVote(code, "alice", VOTE_OPTION_CONTINUE_WITHOUT_PLAYER);
        f.manager.castVote(code, "bob", VOTE_OPTION_CONTINUE_WITHOUT_PLAYER);
        f.advanceSeconds(3);
        assertThat(snapshot(code).getRoundNumber()).isEqualTo(1);
    }

    @Test
    void aPlayerDroppingMidQuestionDoesntStopTheQuestion() {
        String code = room("bob", "cara");
        startAndReachQuestion1(code);
        RecordingStream stream = f.stream(code);

        f.manager.reportDisconnected(code, "bob");

        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_PLAYER_DISCONNECTED);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_QUESTION);
    }

    @Test
    void whenFewerThanTwoPlayersAreLeftTheQuizEnds() {
        String code = room("bob");
        startAndReachQuestion1(code);
        RecordingStream stream = f.stream(code);

        f.manager.leaveRoom(code, "bob");

        assertThat(stream.last().getType()).isEqualTo(ROOM_EVENT_TYPE_PLAYER_LEFT);
        assertThat(snapshot(code).getPhase()).isEqualTo(ROOM_PHASE_FINISHED);
        assertThat(snapshot(code).getEndReason()).isEqualTo(EndReason.END_REASON_NOT_ENOUGH_PLAYERS);
    }

    @Test
    void closingARoomMidQuizFinalizesIt() {
        String code = room("bob");
        startAndReachQuestion1(code);
        f.manager.reportDisconnected(code, "alice");
        f.manager.reportDisconnected(code, "bob");

        f.advance(Duration.ofMinutes(6));

        assertThat(f.rooms.findByCode(code)).isEmpty();
        assertThat(f.scoring.finalized).isNotEmpty();
    }
}
