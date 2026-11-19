package com.friendlyfire.account.user;

import java.util.Optional;

/** The rule for a name a player picks (UpdateDisplayName): trimmed, 1 to 24 characters. */
public final class DisplayNames {

    private DisplayNames() {
    }

    /**
     * The name trimmed, or empty if it breaks the rule. Characters are counted as code
     * points, so an emoji counts once.
     */
    public static Optional<String> valid(String name) {
        String trimmed = name == null ? "" : name.strip();
        long length = trimmed.codePoints().count();
        return length >= 1 && length <= NewUserNames.MAX_LENGTH ? Optional.of(trimmed) : Optional.empty();
    }
}
