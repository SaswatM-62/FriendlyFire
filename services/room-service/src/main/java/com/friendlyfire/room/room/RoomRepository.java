package com.friendlyfire.room.room;

/** Where rooms live. */
public interface RoomRepository {
    // TODO(ff): find, save and delete rooms by room code
    //   - rooms live in memory for now (InMemoryRoomRepository), so a restart of
    //     room-service ends the rooms in progress
    //   - keep it an interface so a shared store can replace it later
}
