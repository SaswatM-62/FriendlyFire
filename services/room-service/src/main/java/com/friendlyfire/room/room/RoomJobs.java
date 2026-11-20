package com.friendlyfire.room.room;

import com.friendlyfire.room.timing.RoomTimings;
import com.friendlyfire.room.timing.Scheduler;
import com.friendlyfire.room.timing.Scheduler.Cancellable;
import java.util.List;

/** The scheduled checks: data status in lobbies (FF-307) and abandoned rooms (FF-311). */
public class RoomJobs implements AutoCloseable {

    private final List<Cancellable> jobs;

    public RoomJobs(RoomManager rooms, Scheduler scheduler, RoomTimings timings) {
        jobs = List.of(
                scheduler.every(timings.dataRefresh(), rooms::refreshDataStatuses),
                scheduler.every(timings.sweep(), rooms::closeAbandonedRooms));
    }

    @Override
    public void close() {
        jobs.forEach(Cancellable::cancel);
    }
}
