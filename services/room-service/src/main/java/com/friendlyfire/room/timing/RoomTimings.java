package com.friendlyfire.room.timing;

import java.time.Duration;

/**
 * How long each phase and timer lasts. The defaults are the agreed design (FF-417,
 * FF-518, ADR-026); tests use the same values with a fake clock.
 *
 * @param preparingMinimum the loading screen's minimum, so everyone can read it
 * @param preparingTimeout building the questions fails after this long
 * @param countdown        the 3-2-1 before every question (and after a pause)
 * @param reveal           the answer and scores between questions
 * @param vote             how long a vote about a disconnected player stays open
 * @param pauseStep        one pause, at most (less if the quiz's pause time runs out)
 * @param pauseBudget      pause time per quiz
 * @param hostHandover     a host disconnected this long in the lobby loses the host role
 * @param dataRefresh      how often players' data status is asked again in the lobby
 * @param abandoned        a room whose players have all been gone this long closes
 * @param idle             a room where nothing happened for this long closes
 * @param sweep            how often rooms are checked for the two rules above
 * @param scoringDeadline  how long Room waits for ScoringService per call
 */
public record RoomTimings(
        Duration preparingMinimum,
        Duration preparingTimeout,
        Duration countdown,
        Duration reveal,
        Duration vote,
        Duration pauseStep,
        Duration pauseBudget,
        Duration hostHandover,
        Duration dataRefresh,
        Duration abandoned,
        Duration idle,
        Duration sweep,
        Duration scoringDeadline) {

    public static RoomTimings defaults() {
        return new RoomTimings(
                Duration.ofSeconds(8),
                Duration.ofSeconds(60),
                Duration.ofSeconds(3),
                Duration.ofSeconds(10),
                Duration.ofSeconds(15),
                Duration.ofSeconds(60),
                Duration.ofSeconds(180),
                Duration.ofSeconds(30),
                Duration.ofSeconds(5),
                Duration.ofMinutes(5),
                Duration.ofHours(2),
                Duration.ofSeconds(30),
                Duration.ofSeconds(3));
    }
}
