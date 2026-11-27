package com.friendlyfire.room.room;

import com.friendlyfire.proto.friendlyfire.room.v1.PlayerLook;
import com.friendlyfire.room.clients.FavouriteLook;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Hands out characters in a room (FF-519). Favourite icons and colours are first come,
 * first served, each on its own: someone whose favourite is taken gets a random free one
 * ({@code random}). Without a favourite, a player gets the first free icon and colour in
 * the default order, as the join order did before. The expression is always kept, because
 * faces can repeat. A room holds at most 8 people and there are 10 of each, so there's
 * always one free.
 */
final class Looks {

    /** The frontend's default order (frontend/src/lib/characters.ts). */
    static final List<String> ICONS =
            List.of("blob", "ghost", "boxy", "onigiri", "star", "cloud", "shield", "flower", "gem", "drop");
    static final List<String> COLOURS =
            List.of("pink", "cyan", "mint", "orange", "violet", "yellow", "blue", "lime", "fuchsia", "silver");
    static final List<String> EXPRESSIONS =
            List.of("happy", "smirk", "cocky", "locked-in", "fierce", "wink", "gloat", "shades");
    static final String DEFAULT_EXPRESSION = "happy";

    private Looks() {
    }

    /**
     * A look for someone joining a room where {@code taken} are the looks of the people
     * already in it. {@code favourite} is null when they haven't picked one (or Account
     * couldn't say).
     */
    static PlayerLook assign(FavouriteLook favourite, Collection<PlayerLook> taken, RandomGenerator random) {
        Set<String> takenIcons = taken.stream().map(PlayerLook::getIcon).collect(Collectors.toSet());
        Set<String> takenColours = taken.stream().map(PlayerLook::getColour).collect(Collectors.toSet());
        if (favourite == null) {
            return PlayerLook.newBuilder()
                    .setIcon(firstFree(ICONS, takenIcons))
                    .setColour(firstFree(COLOURS, takenColours))
                    .setExpression(DEFAULT_EXPRESSION)
                    .build();
        }
        boolean iconFree = ICONS.contains(favourite.icon()) && !takenIcons.contains(favourite.icon());
        boolean colourFree = COLOURS.contains(favourite.colour()) && !takenColours.contains(favourite.colour());
        return PlayerLook.newBuilder()
                .setIcon(iconFree ? favourite.icon() : randomFree(ICONS, takenIcons, random))
                .setColour(colourFree ? favourite.colour() : randomFree(COLOURS, takenColours, random))
                .setExpression(EXPRESSIONS.contains(favourite.expression()) ? favourite.expression() : DEFAULT_EXPRESSION)
                .setRandom(!iconFree || !colourFree)
                .build();
    }

    private static String firstFree(List<String> all, Set<String> taken) {
        return all.stream().filter(id -> !taken.contains(id)).findFirst().orElse(all.getFirst());
    }

    private static String randomFree(List<String> all, Set<String> taken, RandomGenerator random) {
        List<String> free = all.stream().filter(id -> !taken.contains(id)).toList();
        return free.isEmpty() ? all.get(random.nextInt(all.size())) : free.get(random.nextInt(free.size()));
    }
}
