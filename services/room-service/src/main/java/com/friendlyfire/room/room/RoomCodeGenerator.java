package com.friendlyfire.room.room;

import org.springframework.stereotype.Component;

@Component
public class RoomCodeGenerator {
    // TODO(ff): make 6-character room codes, e.g. "K7QM2X"
    //   - no look-alike characters (0/O, 1/I/L, 5/S, 2/Z...)
    //   - try again if the code is already taken (RoomRepository)
    //   - take the random source in the constructor so tests get fixed codes
}
