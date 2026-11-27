package com.friendlyfire.room.config;

import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.quiz.v1.QuizServiceGrpc.QuizServiceBlockingStub;
import com.friendlyfire.proto.friendlyfire.scoring.v1.ScoringServiceGrpc.ScoringServiceBlockingStub;
import com.friendlyfire.room.clients.DataStatusClient;
import com.friendlyfire.room.clients.FavouriteLookClient;
import com.friendlyfire.room.clients.GrpcDataStatusClient;
import com.friendlyfire.room.clients.GrpcFavouriteLookClient;
import com.friendlyfire.room.clients.GrpcQuizClient;
import com.friendlyfire.room.clients.GrpcScoringClient;
import com.friendlyfire.room.clients.QuizClient;
import com.friendlyfire.room.clients.ScoringClient;
import com.friendlyfire.room.room.InMemoryRoomRepository;
import com.friendlyfire.room.room.QuizRunner;
import com.friendlyfire.room.room.RoomCodeGenerator;
import com.friendlyfire.room.room.RoomEventPublisher;
import com.friendlyfire.room.room.RoomJobs;
import com.friendlyfire.room.room.RoomManager;
import com.friendlyfire.room.room.RoomRepository;
import com.friendlyfire.room.timing.ExecutorScheduler;
import com.friendlyfire.room.timing.RoomTimings;
import com.friendlyfire.room.timing.Scheduler;
import java.time.Clock;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.random.RandomGenerator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Rooms, the quiz runner, their timers and the clients they call: the one place they're wired. */
@Configuration(proxyBeanMethods = false)
public class RoomConfig {

    @Bean
    RoomTimings roomTimings() {
        return RoomTimings.defaults();
    }

    /** Background work: building questions, talking to Scoring, timer tasks. */
    @Bean(destroyMethod = "shutdownNow")
    ExecutorService roomWorkers() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }

    @Bean
    ExecutorScheduler roomScheduler(Clock clock, ExecutorService roomWorkers) {
        return new ExecutorScheduler(clock, roomWorkers);
    }

    @Bean
    RoomRepository roomRepository() {
        return new InMemoryRoomRepository();
    }

    @Bean
    RoomCodeGenerator roomCodeGenerator(RoomRepository rooms, RandomGenerator random) {
        return new RoomCodeGenerator(rooms, random);
    }

    @Bean
    RoomEventPublisher roomEventPublisher(Clock clock) {
        return new RoomEventPublisher(clock);
    }

    @Bean
    DataStatusClient dataStatusClient(AccountServiceBlockingStub accountService) {
        return new GrpcDataStatusClient(accountService);
    }

    @Bean
    QuizClient quizClient(QuizServiceBlockingStub quizService) {
        return new GrpcQuizClient(quizService);
    }

    @Bean
    ScoringClient scoringClient(ScoringServiceBlockingStub scoringService, RoomTimings timings) {
        return new GrpcScoringClient(scoringService, timings.scoringDeadline());
    }

    @Bean
    QuizRunner quizRunner(
            RoomEventPublisher events, QuizClient quiz, ScoringClient scoring, Clock clock, Scheduler scheduler,
            ExecutorService roomWorkers, RoomTimings timings) {
        return new QuizRunner(events, quiz, scoring, clock, scheduler, roomWorkers, timings);
    }

    @Bean
    FavouriteLookClient favouriteLookClient(AccountServiceBlockingStub accountService) {
        return new GrpcFavouriteLookClient(accountService);
    }

    @Bean
    RoomManager roomManager(
            RoomRepository rooms, RoomCodeGenerator codes, RoomEventPublisher events, QuizRunner runner,
            DataStatusClient dataStatuses, FavouriteLookClient favourites, RandomGenerator random, Clock clock,
            ExecutorService roomWorkers, RoomTimings timings) {
        return new RoomManager(rooms, codes, events, runner, dataStatuses, favourites, random, clock, roomWorkers,
                timings);
    }

    @Bean
    RoomJobs roomJobs(RoomManager rooms, Scheduler scheduler, RoomTimings timings) {
        return new RoomJobs(rooms, scheduler, timings);
    }
}
