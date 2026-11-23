package com.friendlyfire.room.room;

import com.friendlyfire.proto.friendlyfire.quiz.v1.Question;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizPlayer;
import com.friendlyfire.proto.friendlyfire.room.v1.EndReason;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerResult;
import com.friendlyfire.proto.friendlyfire.room.v1.PlayerStatus;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomEventType;
import com.friendlyfire.proto.friendlyfire.room.v1.RoomPhase;
import com.friendlyfire.proto.friendlyfire.room.v1.VoteOption;
import com.friendlyfire.proto.friendlyfire.scoring.v1.FinalizeQuizRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.PlayerAnswer;
import com.friendlyfire.proto.friendlyfire.scoring.v1.QuizParticipant;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundRequest;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RecordRoundResponse;
import com.friendlyfire.proto.friendlyfire.scoring.v1.RoundPoints;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoreboardEntry;
import com.friendlyfire.proto.friendlyfire.scoring.v1.StartQuizRequest;
import com.friendlyfire.room.clients.QuizClient;
import com.friendlyfire.room.clients.ScoringClient;
import com.friendlyfire.room.room.Room.Answer;
import com.friendlyfire.room.room.Room.Member;
import com.friendlyfire.room.room.Room.Quiz;
import com.friendlyfire.room.room.Room.Score;
import com.friendlyfire.room.room.Room.VoteState;
import com.friendlyfire.room.timing.RoomTimings;
import com.friendlyfire.room.timing.Scheduler;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Runs a quiz from Start to the final standings: PREPARING → STARTING → QUESTION →
 * REVEAL → ... → FINISHED, with a 3-2-1 before every question, and a VOTE (and PAUSED)
 * when a player has dropped (ADR-026). Deadlines come from the injected Clock.
 *
 * <p>Every method here runs with the room's lock held, except the background work
 * (building the questions, talking to ScoringService), which re-takes the lock and
 * checks the room hasn't moved on before applying its result.
 */
public class QuizRunner {

    private static final Logger log = LoggerFactory.getLogger(QuizRunner.class);

    static final String TOO_SLOW = "Building the questions took too long. Try again.";
    static final String SCORES_UNAVAILABLE = "Scores can't be saved right now. Try again in a moment.";
    static final String QUIZ_ENGINE_UNAVAILABLE = "The quiz engine couldn't be reached. Try again in a moment.";
    static final String TOO_FEW_LEFT = "Not enough players are left to start. You need at least 2.";

    private final RoomEventPublisher events;
    private final QuizClient quizzes;
    private final ScoringClient scoring;
    private final Clock clock;
    private final Scheduler scheduler;
    private final Executor background;
    private final RoomTimings timings;

    public QuizRunner(
            RoomEventPublisher events,
            QuizClient quizzes,
            ScoringClient scoring,
            Clock clock,
            Scheduler scheduler,
            Executor background,
            RoomTimings timings) {
        this.events = events;
        this.quizzes = quizzes;
        this.scoring = scoring;
        this.clock = clock;
        this.scheduler = scheduler;
        this.background = background;
        this.timings = timings;
    }

    // ---------- Start ----------

    /**
     * After RoomManager checked the room: PREPARING (every browser shows the loading
     * screen) and QUIZ_PREPARING. Returns the work that builds the questions, for the
     * caller to run in the background once it has let go of the lock.
     */
    Runnable beginPreparing(Room room) {
        Instant now = clock.instant();
        List<String> players = new ArrayList<>();
        for (Member m : room.members.values()) {
            if (m.status == PlayerStatus.PLAYER_STATUS_SITTING_OUT) {
                // Still away from the last quiz.
                m.status = PlayerStatus.PLAYER_STATUS_DISCONNECTED;
            } else if (m.status == PlayerStatus.PLAYER_STATUS_SPECTATING) {
                m.status = PlayerStatus.PLAYER_STATUS_CONNECTED;
            }
            m.answered = false;
            players.add(m.userId);
        }
        Quiz quiz = new Quiz(UUID.randomUUID().toString(), players, now, now.plus(timings.preparingMinimum()),
                (int) timings.pauseBudget().toSeconds());
        room.members.values().forEach(m -> quiz.scores.put(m.userId, new Score(m.userId, m.displayName)));
        room.quiz = quiz;
        room.roundNumber = 0;
        room.currentQuestion = null;
        room.currentResults = List.of();
        room.vote = null;
        room.waitingForUserId = null;
        room.endReason = null;
        transition(room, RoomPhase.ROOM_PHASE_PREPARING, quiz.preparingMinimumEnds);
        timer(room, now.plus(timings.preparingTimeout()), () -> {
            if (!quiz.ready) {
                failStart(room, TOO_SLOW);
            }
        });
        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_QUIZ_PREPARING, null);

        List<QuizPlayer> quizPlayers = quizPlayers(room);
        int count = room.settings.getQuestionCount();
        List<String> asked = List.copyOf(room.askedTemplateIds);
        Member host = room.members.get(room.hostUserId);
        StartQuizRequest register = StartQuizRequest.newBuilder()
                .setQuizId(quiz.id)
                .setRoomCode(room.getCode())
                .addAllPlayers(room.members.values().stream()
                        .map(m -> QuizParticipant.newBuilder()
                                .setUserId(m.userId)
                                .setDisplayName(m.displayName)
                                .build())
                        .toList())
                .setTotalRounds(count)
                .setSecondsPerQuestion(room.settings.getSecondsPerQuestion())
                .setHostDisplayName(host == null ? "" : host.displayName)
                .build();
        return () -> prepare(room, quiz.id, quizPlayers, count, asked, register);
    }

    /** In the background: the questions from the quiz engine, then the quiz registered with Scoring. */
    private void prepare(
            Room room, String quizId, List<QuizPlayer> players, int count, List<String> asked,
            StartQuizRequest register) {
        List<Question> questions;
        try {
            questions = quizzes.generate(players, count, asked, timings.preparingTimeout());
        } catch (StatusRuntimeException e) {
            log.info("quiz generation failed for room {}: {}", room.getCode(), e.getStatus());
            failIfPreparing(room, quizId, generationMessage(e));
            return;
        } catch (RuntimeException e) {
            log.error("quiz generation failed for room {}", room.getCode(), e);
            failIfPreparing(room, quizId, QUIZ_ENGINE_UNAVAILABLE);
            return;
        }
        if (questions.size() < count) {
            failIfPreparing(room, quizId, "The quiz engine made only " + questions.size()
                    + " questions. Choose " + Math.max(questions.size(), 5) + " or fewer.");
            return;
        }
        try {
            scoring.startQuiz(register);
        } catch (StatusRuntimeException e) {
            if (e.getStatus().getCode() != Status.Code.ALREADY_EXISTS) {
                log.warn("couldn't register quiz {} with Scoring: {}", quizId, e.getStatus());
                failIfPreparing(room, quizId, SCORES_UNAVAILABLE);
                return;
            }
        }
        synchronized (room) {
            if (!preparing(room, quizId)) {
                return;
            }
            Quiz quiz = room.quiz;
            quiz.questions = List.copyOf(questions.subList(0, count));
            quiz.ready = true;
            Instant start = quiz.preparingMinimumEnds;
            Instant now = clock.instant();
            timer(room, start.isAfter(now) ? start : now, () -> nextStep(room, null, null));
        }
    }

    private void failIfPreparing(Room room, String quizId, String message) {
        synchronized (room) {
            if (preparing(room, quizId)) {
                failStart(room, message);
            }
        }
    }

    private static boolean preparing(Room room, String quizId) {
        return !room.closed && room.phase == RoomPhase.ROOM_PHASE_PREPARING && room.quiz != null
                && room.quiz.id.equals(quizId);
    }

    /** Back to the lobby with QUIZ_START_FAILED and a reason everyone sees. */
    void failStart(Room room, String message) {
        room.quiz = null;
        room.roundNumber = 0;
        for (Member m : room.members.values()) {
            if (m.status == PlayerStatus.PLAYER_STATUS_SPECTATING) {
                m.status = PlayerStatus.PLAYER_STATUS_CONNECTED;
            }
        }
        transition(room, RoomPhase.ROOM_PHASE_LOBBY, null);
        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_QUIZ_START_FAILED, null, message, null);
        watchHost(room);
    }

    static String generationMessage(StatusRuntimeException e) {
        String description = e.getStatus().getDescription();
        return switch (e.getStatus().getCode()) {
            // The quiz engine's own sentence, e.g. "There's only enough match data for 8
            // different questions, not 15. Choose 8 or fewer questions."
            case FAILED_PRECONDITION, INVALID_ARGUMENT ->
                description == null || description.isBlank() ? "The quiz couldn't be built." : description;
            case DEADLINE_EXCEEDED -> TOO_SLOW;
            case UNAVAILABLE -> description == null || description.isBlank() ? QUIZ_ENGINE_UNAVAILABLE : description;
            default -> QUIZ_ENGINE_UNAVAILABLE;
        };
    }

    /**
     * The players as the quiz engine sees them. Two players with the same name get their
     * Riot tag ("Sam (NA1)"), so the answer buttons are clear (FF-303).
     */
    static List<QuizPlayer> quizPlayers(Room room) {
        Map<String, Long> names = room.members.values().stream()
                .collect(Collectors.groupingBy(m -> m.displayName, Collectors.counting()));
        Map<String, Integer> seen = new HashMap<>();
        List<QuizPlayer> players = new ArrayList<>();
        for (Member m : room.members.values()) {
            String label = m.displayName;
            if (names.get(m.displayName) > 1) {
                label = m.riotTagLine.isEmpty()
                        ? m.displayName + " (" + seen.merge(m.displayName, 1, Integer::sum) + ")"
                        : m.displayName + " (" + m.riotTagLine + ")";
            }
            players.add(QuizPlayer.newBuilder()
                    .setUserId(m.userId)
                    .setDisplayName(label)
                    .setNotEnoughMatches(m.data.notEnoughMatches())
                    .build());
        }
        // Same name and same tag (or a tag on one of two): number the rest.
        Map<String, Long> labels = players.stream()
                .collect(Collectors.groupingBy(QuizPlayer::getDisplayName, Collectors.counting()));
        if (labels.values().stream().anyMatch(n -> n > 1)) {
            Map<String, Integer> numbering = new HashMap<>();
            List<QuizPlayer> numbered = new ArrayList<>();
            for (QuizPlayer p : players) {
                String label = p.getDisplayName();
                if (labels.get(label) > 1) {
                    label = label + " #" + numbering.merge(label, 1, Integer::sum);
                }
                numbered.add(p.toBuilder().setDisplayName(label).build());
            }
            return numbered;
        }
        return players;
    }

    // ---------- Moving along ----------

    /**
     * What comes after preparing, a reveal, a vote or a pause: the end if fewer than 2
     * players are left, a vote if someone dropped, otherwise the countdown to the next
     * question (or the end after the last one). {@code type} overrides the event that
     * announces the change (e.g. VOTE_RESOLVED, PLAYER_RECONNECTED).
     */
    void nextStep(Room room, RoomEventType type, String subject) {
        Quiz quiz = room.quiz;
        List<Member> remaining = remaining(room);
        long connected = remaining.stream().filter(m -> m.status == PlayerStatus.PLAYER_STATUS_CONNECTED).count();
        if (remaining.size() < Room.MIN_PLAYERS || connected == 0) {
            finish(room, EndReason.END_REASON_NOT_ENOUGH_PLAYERS, type, subject);
            return;
        }
        Optional<Member> gone = remaining.stream()
                .filter(m -> m.status == PlayerStatus.PLAYER_STATUS_DISCONNECTED)
                .findFirst();
        if (gone.isPresent()) {
            startVote(room, gone.get().userId, type, subject);
        } else if (room.roundNumber < quiz.totalRounds()) {
            countdown(room, room.roundNumber + 1, type, subject);
        } else {
            finish(room, EndReason.END_REASON_COMPLETED, type, subject);
        }
    }

    /** Quiz players still in the quiz: in the room, and not sitting out. */
    private static List<Member> remaining(Room room) {
        return room.quizMembers().stream()
                .filter(m -> m.status == PlayerStatus.PLAYER_STATUS_CONNECTED
                        || m.status == PlayerStatus.PLAYER_STATUS_DISCONNECTED)
                .toList();
    }

    private void countdown(Room room, int round, RoomEventType type, String subject) {
        Instant deadline = clock.instant().plus(timings.countdown());
        room.currentQuestion = null;
        room.currentResults = List.of();
        room.vote = null;
        room.waitingForUserId = null;
        room.members.values().forEach(m -> m.answered = false);
        transition(room, RoomPhase.ROOM_PHASE_STARTING, deadline);
        timer(room, deadline, () -> startQuestion(room, round));
        events.publish(room, type == null ? RoomEventType.ROOM_EVENT_TYPE_QUIZ_STARTING : type, subject);
    }

    private void startQuestion(Room room, int round) {
        Quiz quiz = room.quiz;
        Question question = quiz.questions.get(round - 1);
        Instant now = clock.instant();
        Instant deadline = now.plusSeconds(room.settings.getSecondsPerQuestion());
        room.roundNumber = round;
        room.currentQuestion = question;
        room.currentResults = List.of();
        quiz.answers.clear();
        quiz.answersClosed = false;
        quiz.questionStartedAt = now;
        room.members.values().forEach(m -> m.answered = false);
        if (!question.getTemplateId().isEmpty()) {
            room.askedTemplateIds.add(question.getTemplateId());
        }
        transition(room, RoomPhase.ROOM_PHASE_QUESTION, deadline);
        timer(room, deadline, () -> closeQuestion(room));
        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_QUESTION_STARTED, null);
    }

    // ---------- Answers ----------

    void submitAnswer(Room room, Member member, String questionId, String optionId) {
        if (member.status == PlayerStatus.PLAYER_STATUS_SPECTATING) {
            throw RoomErrors.precondition("You're watching this quiz. You'll play in the next one.");
        }
        Quiz quiz = room.quiz;
        Question question = room.currentQuestion;
        if (room.phase != RoomPhase.ROOM_PHASE_QUESTION || question == null || quiz == null || quiz.answersClosed) {
            throw RoomErrors.precondition("No question is open.");
        }
        if (!question.getQuestionId().equals(questionId)) {
            throw RoomErrors.precondition("That question is over.");
        }
        Instant now = clock.instant();
        if (now.isAfter(room.phaseDeadline)) {
            throw RoomErrors.precondition("Time's up for that question.");
        }
        if (!room.isPlaying(member.userId)) {
            throw RoomErrors.precondition("You're not playing in this quiz.");
        }
        if (quiz.answers.containsKey(member.userId)) {
            throw RoomErrors.alreadyExists("You already answered.");
        }
        if (question.getOptionsList().stream().noneMatch(o -> o.getOptionId().equals(optionId))) {
            throw RoomErrors.invalid("That isn't one of the answers.");
        }
        long limitMs = room.settings.getSecondsPerQuestion() * 1000L;
        long ms = Math.clamp(Duration.between(quiz.questionStartedAt, now).toMillis(), 0, limitMs);
        quiz.answers.put(member.userId, new Answer(optionId, (int) ms));
        member.answered = true;
        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_ANSWERED, member.userId);
        if (everyoneAnswered(room)) {
            closeQuestion(room);
        }
    }

    /** Every connected player has answered: the reveal comes early (FF-418). */
    private static boolean everyoneAnswered(Room room) {
        List<Member> connected = room.quizMembers().stream()
                .filter(m -> m.status == PlayerStatus.PLAYER_STATUS_CONNECTED)
                .toList();
        return !connected.isEmpty() && connected.stream().allMatch(m -> room.quiz.answers.containsKey(m.userId));
    }

    /** Time's up (or everyone answered): no more answers; Scoring scores the round, then the reveal. */
    private void closeQuestion(Room room) {
        Quiz quiz = room.quiz;
        if (room.phase != RoomPhase.ROOM_PHASE_QUESTION || quiz == null || quiz.answersClosed) {
            return;
        }
        quiz.answersClosed = true;
        long generation = room.generation;
        String correct = room.currentQuestion.getCorrectOptionId();
        RecordRoundRequest.Builder round = RecordRoundRequest.newBuilder()
                .setQuizId(quiz.id)
                .setRoundNumber(room.roundNumber);
        for (String userId : quiz.scores.keySet()) {
            Answer answer = quiz.answers.get(userId);
            round.addAnswers(PlayerAnswer.newBuilder()
                    .setUserId(userId)
                    .setAnswered(answer != null)
                    .setCorrect(answer != null && answer.optionId().equals(correct))
                    .setResponseTimeMs(answer == null ? 0 : answer.responseTimeMs()));
        }
        RecordRoundRequest request = round.build();
        background.execute(() -> {
            RecordRoundResponse scored = recordRound(request);
            synchronized (room) {
                if (!room.closed && room.generation == generation) {
                    reveal(room, scored);
                }
            }
        });
    }

    /** Scoring's points for the round; it's retry-safe, so try twice. Null if it stays down. */
    private RecordRoundResponse recordRound(RecordRoundRequest request) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            try {
                return scoring.recordRound(request);
            } catch (StatusRuntimeException e) {
                log.warn("RecordRound failed for quiz {} (try {} of 2): {}", request.getQuizId(), attempt, e.getStatus());
            }
        }
        return null;
    }

    /** The answer, everyone's picks and points, and the updated scoreboard. */
    private void reveal(Room room, RecordRoundResponse scored) {
        Quiz quiz = room.quiz;
        Question question = room.currentQuestion;
        String correct = question.getCorrectOptionId();
        int limitMs = room.settings.getSecondsPerQuestion() * 1000;
        Map<String, Integer> points = new LinkedHashMap<>();
        if (scored != null) {
            for (RoundPoints p : scored.getRoundPointsList()) {
                points.put(p.getUserId(), p.getPoints());
            }
        }
        List<PlayerResult> results = new ArrayList<>();
        for (String userId : quiz.scores.keySet()) {
            Answer answer = quiz.answers.get(userId);
            boolean right = answer != null && answer.optionId().equals(correct);
            int earned = points.computeIfAbsent(userId, id -> right ? localPoints(answer.responseTimeMs(), limitMs) : 0);
            if (right && (quiz.fastestUserId == null || answer.responseTimeMs() < quiz.fastestMs)) {
                quiz.fastestUserId = userId;
                quiz.fastestMs = answer.responseTimeMs();
            }
            results.add(PlayerResult.newBuilder()
                    .setUserId(userId)
                    .setChosenOptionId(answer == null ? "" : answer.optionId())
                    .setCorrect(right)
                    .setPoints(earned)
                    .build());
            Score score = quiz.scores.get(userId);
            score.lastRound = earned;
            if (scored == null) {
                score.total += earned;
                score.correct += right ? 1 : 0;
            }
        }
        if (scored != null) {
            for (ScoreboardEntry entry : scored.getScoreboardList()) {
                Score score = quiz.scores.get(entry.getUserId());
                if (score != null) {
                    score.total = entry.getTotalPoints();
                    score.correct = entry.getCorrectAnswers();
                    score.rank = entry.getRank();
                }
            }
        } else {
            rank(quiz);
        }
        quiz.roundsPlayed = room.roundNumber;
        room.currentResults = List.copyOf(results);
        Instant deadline = clock.instant().plus(timings.reveal());
        transition(room, RoomPhase.ROOM_PHASE_REVEAL, deadline);
        timer(room, deadline, () -> nextStep(room, null, null));
        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_QUESTION_REVEALED, null);
    }

    /** The contract's formula, used only when Scoring can't be reached: 500 + 500 × time left / limit. */
    static int localPoints(int responseTimeMs, int limitMs) {
        double left = Math.max(0, limitMs - responseTimeMs);
        return (int) Math.round(500 + 500 * left / limitMs);
    }

    /** Equal points share a rank (1, 1, 3). */
    private static void rank(Quiz quiz) {
        for (Score score : quiz.scores.values()) {
            score.rank = 1 + (int) quiz.scores.values().stream().filter(s -> s.total > score.total).count();
        }
    }

    // ---------- Votes and pauses (ADR-026) ----------

    private void startVote(Room room, String aboutUserId, RoomEventType type, String subject) {
        Instant deadline = clock.instant().plus(timings.vote());
        room.vote = new VoteState(aboutUserId, deadline, room.quiz.pauseSecondsLeft > 0);
        room.currentQuestion = null;
        room.currentResults = List.of();
        room.waitingForUserId = null;
        transition(room, RoomPhase.ROOM_PHASE_VOTE, deadline);
        timer(room, deadline, () -> resolveVote(room));
        events.publish(room, type == null ? RoomEventType.ROOM_EVENT_TYPE_VOTE_STARTED : type,
                subject == null ? aboutUserId : subject);
    }

    void castVote(Room room, Member member, VoteOption option) {
        VoteState vote = room.vote;
        if (room.phase != RoomPhase.ROOM_PHASE_VOTE || vote == null) {
            throw RoomErrors.precondition("There's no vote right now.");
        }
        if (member.status == PlayerStatus.PLAYER_STATUS_SPECTATING) {
            throw RoomErrors.precondition("You're watching this quiz, so you can't vote.");
        }
        if (!room.isPlaying(member.userId) || member.status != PlayerStatus.PLAYER_STATUS_CONNECTED) {
            throw RoomErrors.precondition("Only connected players can vote.");
        }
        if (member.userId.equals(vote.aboutUserId)) {
            throw RoomErrors.precondition("You can't vote about yourself.");
        }
        if (option == null || option == VoteOption.VOTE_OPTION_UNSPECIFIED || option == VoteOption.UNRECOGNIZED) {
            throw RoomErrors.invalid("That isn't one of the vote options.");
        }
        if (option == VoteOption.VOTE_OPTION_PAUSE && !vote.pauseAllowed) {
            throw RoomErrors.precondition("There's no pause time left in this quiz.");
        }
        if (vote.votes.containsKey(member.userId)) {
            throw RoomErrors.alreadyExists("You already voted.");
        }
        vote.votes.put(member.userId, option);
        events.publish(room, RoomEventType.ROOM_EVENT_TYPE_VOTE_CAST, member.userId);
        if (everyoneVoted(room)) {
            resolveVote(room);
        }
    }

    private static boolean everyoneVoted(Room room) {
        VoteState vote = room.vote;
        List<Member> voters = room.quizMembers().stream()
                .filter(m -> m.status == PlayerStatus.PLAYER_STATUS_CONNECTED && !m.userId.equals(vote.aboutUserId))
                .toList();
        return !voters.isEmpty() && voters.stream().allMatch(m -> vote.votes.containsKey(m.userId));
    }

    /** Most votes wins; a tie pauses if pause time is left, otherwise continues. */
    private void resolveVote(Room room) {
        VoteState vote = room.vote;
        if (room.phase != RoomPhase.ROOM_PHASE_VOTE || vote == null) {
            return;
        }
        Map<VoteOption, Long> counts = new LinkedHashMap<>();
        for (VoteOption option : List.of(VoteOption.VOTE_OPTION_PAUSE, VoteOption.VOTE_OPTION_CONTINUE_WITHOUT_PLAYER,
                VoteOption.VOTE_OPTION_END_QUIZ)) {
            counts.put(option, vote.count(option));
        }
        long top = counts.values().stream().mapToLong(Long::longValue).max().orElse(0);
        List<VoteOption> winners = counts.entrySet().stream().filter(e -> e.getValue() == top).map(Map.Entry::getKey).toList();
        VoteOption outcome = winners.size() == 1
                ? winners.getFirst()
                : vote.pauseAllowed ? VoteOption.VOTE_OPTION_PAUSE : VoteOption.VOTE_OPTION_CONTINUE_WITHOUT_PLAYER;
        String about = vote.aboutUserId;
        room.vote = null;
        RoomEventType resolved = RoomEventType.ROOM_EVENT_TYPE_VOTE_RESOLVED;
        switch (outcome) {
            case VOTE_OPTION_END_QUIZ -> finish(room, EndReason.END_REASON_VOTED_TO_END, resolved, about);
            case VOTE_OPTION_PAUSE -> pause(room, about, resolved);
            default -> {
                room.member(about).ifPresent(m -> m.status = PlayerStatus.PLAYER_STATUS_SITTING_OUT);
                nextStep(room, resolved, about);
            }
        }
    }

    private void pause(Room room, String aboutUserId, RoomEventType type) {
        Quiz quiz = room.quiz;
        int seconds = (int) Math.min(timings.pauseStep().toSeconds(), quiz.pauseSecondsLeft);
        Instant now = clock.instant();
        quiz.pauseSecondsLeft -= seconds;
        quiz.pauseStartedAt = now;
        quiz.pauseGrantedSeconds = seconds;
        room.waitingForUserId = aboutUserId;
        Instant deadline = now.plusSeconds(seconds);
        transition(room, RoomPhase.ROOM_PHASE_PAUSED, deadline);
        timer(room, deadline, () -> {
            Optional<Member> about = room.member(aboutUserId);
            if (about.isPresent() && about.get().status == PlayerStatus.PLAYER_STATUS_DISCONNECTED) {
                startVote(room, aboutUserId, null, null);
            } else {
                nextStep(room, null, null);
            }
        });
        events.publish(room, type, aboutUserId);
    }

    /** Pause time not used because they came back early goes back to the quiz's 3 minutes. */
    private void refundPause(Room room) {
        Quiz quiz = room.quiz;
        long used = Duration.between(quiz.pauseStartedAt, clock.instant()).toSeconds();
        quiz.pauseSecondsLeft += (int) Math.max(0, quiz.pauseGrantedSeconds - used);
        quiz.pauseGrantedSeconds = 0;
        room.waitingForUserId = null;
    }

    // ---------- People coming and going during a quiz ----------

    /**
     * A player came back. If the room was voting about them, or paused for them, the quiz
     * carries on. Returns true if an event (PLAYER_RECONNECTED) was already published.
     */
    boolean onReconnected(Room room, String userId) {
        if (room.phase == RoomPhase.ROOM_PHASE_VOTE && room.vote != null && room.vote.aboutUserId.equals(userId)) {
            room.vote = null;
            nextStep(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_RECONNECTED, userId);
            return true;
        }
        if (room.phase == RoomPhase.ROOM_PHASE_PAUSED && userId.equals(room.waitingForUserId)) {
            refundPause(room);
            nextStep(room, RoomEventType.ROOM_EVENT_TYPE_PLAYER_RECONNECTED, userId);
            return true;
        }
        return false;
    }

    /** After PLAYER_DISCONNECTED: the question or vote may now be complete without them. */
    void afterDisconnected(Room room) {
        if (room.phase == RoomPhase.ROOM_PHASE_QUESTION && room.quiz != null && everyoneAnswered(room)) {
            closeQuestion(room);
        } else if (room.phase == RoomPhase.ROOM_PHASE_VOTE && room.vote != null && everyoneVoted(room)) {
            resolveVote(room);
        }
    }

    /**
     * A quiz player left the room (or was removed). Returns true if this published the
     * event announcing it; false if the caller should publish {@code type}.
     */
    boolean afterLeft(Room room, String userId, RoomEventType type) {
        Quiz quiz = room.quiz;
        if (quiz == null || !room.quizRunning() || !quiz.playerIds.contains(userId)) {
            return false;
        }
        if (room.phase == RoomPhase.ROOM_PHASE_PREPARING) {
            if (room.quizMembers().size() < Room.MIN_PLAYERS) {
                failStart(room, TOO_FEW_LEFT);
                return true;
            }
            return false;
        }
        if (remaining(room).size() < Room.MIN_PLAYERS) {
            finish(room, EndReason.END_REASON_NOT_ENOUGH_PLAYERS, type, userId);
            return true;
        }
        switch (room.phase) {
            case ROOM_PHASE_VOTE -> {
                if (room.vote.aboutUserId.equals(userId)) {
                    room.vote = null;
                    nextStep(room, type, userId);
                } else {
                    events.publish(room, type, userId);
                    if (everyoneVoted(room)) {
                        resolveVote(room);
                    }
                }
                return true;
            }
            case ROOM_PHASE_PAUSED -> {
                if (userId.equals(room.waitingForUserId)) {
                    refundPause(room);
                    nextStep(room, type, userId);
                    return true;
                }
                return false;
            }
            case ROOM_PHASE_QUESTION -> {
                events.publish(room, type, userId);
                if (everyoneAnswered(room)) {
                    closeQuestion(room);
                }
                return true;
            }
            default -> {
                return false;
            }
        }
    }

    // ---------- The end ----------

    private void finish(Room room, EndReason reason, RoomEventType type, String subject) {
        Quiz quiz = room.quiz;
        room.endReason = reason;
        room.currentQuestion = null;
        room.currentResults = List.of();
        room.vote = null;
        room.waitingForUserId = null;
        for (Member m : room.members.values()) {
            m.answered = false;
            if (m.status == PlayerStatus.PLAYER_STATUS_SPECTATING) {
                // Spectators play from the next quiz.
                m.status = PlayerStatus.PLAYER_STATUS_CONNECTED;
            } else if (m.status == PlayerStatus.PLAYER_STATUS_SITTING_OUT) {
                m.status = PlayerStatus.PLAYER_STATUS_DISCONNECTED;
            }
        }
        transition(room, RoomPhase.ROOM_PHASE_FINISHED, null);
        events.publish(room, type == null ? RoomEventType.ROOM_EVENT_TYPE_QUIZ_FINISHED : type, subject);
        finalizeLater(quiz, reason);
        watchHost(room);
    }

    /** ScoringService.FinalizeQuiz in the background; the room doesn't wait for it. */
    void finalizeLater(Quiz quiz, EndReason reason) {
        if (quiz == null || !quiz.ready) {
            return;
        }
        FinalizeQuizRequest request = FinalizeQuizRequest.newBuilder()
                .setQuizId(quiz.id)
                .setRoundsPlayed(quiz.roundsPlayed)
                .setEndReason(com.friendlyfire.proto.friendlyfire.scoring.v1.EndReason.forNumber(reason.getNumber()))
                .build();
        background.execute(() -> {
            try {
                scoring.finalizeQuiz(request);
            } catch (StatusRuntimeException e) {
                log.warn("FinalizeQuiz failed for quiz {}: {}", quiz.id, e.getStatus());
            }
        });
    }

    // ---------- The host ----------

    /**
     * A host who stays disconnected in the lobby (or after a quiz) for 30 seconds hands
     * the host role to the next connected player (FF-308).
     */
    void watchHost(Room room) {
        Member host = room.members.get(room.hostUserId);
        if (host == null || host.connected() || host.disconnectedAt == null
                || !(room.phase == RoomPhase.ROOM_PHASE_LOBBY || room.phase == RoomPhase.ROOM_PHASE_FINISHED)) {
            return;
        }
        if (room.hostTimer != null) {
            room.hostTimer.cancel();
        }
        Instant at = host.disconnectedAt.plus(timings.hostHandover());
        room.hostTimer = scheduler.schedule(at, () -> {
            synchronized (room) {
                handOverHost(room);
            }
        });
    }

    private void handOverHost(Room room) {
        if (room.closed || !(room.phase == RoomPhase.ROOM_PHASE_LOBBY || room.phase == RoomPhase.ROOM_PHASE_FINISHED)) {
            return;
        }
        Member host = room.members.get(room.hostUserId);
        if (host == null || host.connected() || host.disconnectedAt == null
                || clock.instant().isBefore(host.disconnectedAt.plus(timings.hostHandover()))) {
            return;
        }
        room.members.values().stream()
                .filter(m -> m.status == PlayerStatus.PLAYER_STATUS_CONNECTED && !m.userId.equals(host.userId))
                .findFirst()
                .ifPresent(next -> {
                    room.hostUserId = next.userId;
                    events.publish(room, RoomEventType.ROOM_EVENT_TYPE_HOST_CHANGED, next.userId);
                });
    }

    // ---------- Phases and timers ----------

    private static void transition(Room room, RoomPhase phase, Instant deadline) {
        room.generation++;
        if (room.phaseTimer != null) {
            room.phaseTimer.cancel();
            room.phaseTimer = null;
        }
        room.phase = phase;
        room.phaseDeadline = deadline;
    }

    /** Runs {@code task} at {@code at} under the room's lock, unless the room has moved on to another phase. */
    private void timer(Room room, Instant at, Runnable task) {
        long generation = room.generation;
        if (room.phaseTimer != null) {
            room.phaseTimer.cancel();
        }
        room.phaseTimer = scheduler.schedule(at, () -> {
            synchronized (room) {
                if (!room.closed && room.generation == generation) {
                    task.run();
                }
            }
        });
    }

    /** Stops the room's timers when it closes. */
    static void stopTimers(Room room) {
        if (room.phaseTimer != null) {
            room.phaseTimer.cancel();
        }
        if (room.hostTimer != null) {
            room.hostTimer.cancel();
        }
        room.generation++;
    }
}
