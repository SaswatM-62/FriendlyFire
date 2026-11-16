package com.friendlyfire.account.user;

import java.util.random.RandomGenerator;

/**
 * The display name a new user starts with, made from the name in their Auth0 token.
 * Players can change it later (UpdateDisplayName allows 1 to 24 characters).
 */
public class NewUserNames {

    public static final int MAX_LENGTH = 24;

    private final RandomGenerator random;

    public NewUserNames(RandomGenerator random) {
        this.random = random;
    }

    /**
     * The suggested name trimmed and cut to 24 characters. An empty name, or one with an
     * {@code @}, becomes "Player" plus 4 random digits: Auth0 often uses the email address
     * as the name of an email/password account, and everyone in a room sees this name.
     */
    public String from(String suggested) {
        String name = suggested == null ? "" : suggested.strip();
        if (name.isEmpty() || name.contains("@")) {
            return "Player %04d".formatted(random.nextInt(10_000));
        }
        // Counted in code points, so an emoji is never cut in half.
        StringBuilder cut = new StringBuilder();
        name.codePoints().limit(MAX_LENGTH).forEach(cut::appendCodePoint);
        return cut.toString().strip();
    }
}
