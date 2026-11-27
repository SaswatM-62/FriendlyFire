package com.friendlyfire.account.user;

import java.util.List;
import java.util.Optional;

/**
 * A user's favourite character (Profile "Your look", FF-519), by the frontend's IDs
 * ({@code frontend/src/lib/characters.ts}). RoomService hands out icons and colours first
 * come, first served; the expression is always kept.
 */
public record FavouriteLook(String icon, String colour, String expression) {

    public static final List<String> ICONS =
            List.of("blob", "ghost", "boxy", "onigiri", "star", "cloud", "shield", "flower", "gem", "drop");
    public static final List<String> COLOURS =
            List.of("pink", "cyan", "mint", "orange", "violet", "yellow", "blue", "lime", "fuchsia", "silver");
    public static final List<String> EXPRESSIONS =
            List.of("happy", "smirk", "cocky", "locked-in", "fierce", "wink", "gloat", "shades");

    /** The look if the icon, colour and expression are all known IDs, otherwise empty. */
    public static Optional<FavouriteLook> valid(String icon, String colour, String expression) {
        if (!ICONS.contains(icon) || !COLOURS.contains(colour) || !EXPRESSIONS.contains(expression)) {
            return Optional.empty();
        }
        return Optional.of(new FavouriteLook(icon, colour, expression));
    }
}
