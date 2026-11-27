package com.friendlyfire.room.room;

import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.room.v1.PlayerLook;
import com.friendlyfire.room.clients.FavouriteLook;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import org.junit.jupiter.api.Test;

class LooksTest {

    private final Random random = new Random(3);

    private static PlayerLook look(String icon, String colour) {
        return PlayerLook.newBuilder().setIcon(icon).setColour(colour).setExpression("happy").build();
    }

    @Test
    void aFreeFavouriteIsKept() {
        PlayerLook look = Looks.assign(new FavouriteLook("ghost", "cyan", "smirk"), List.of(), random);

        assertThat(look).isEqualTo(PlayerLook.newBuilder()
                .setIcon("ghost").setColour("cyan").setExpression("smirk").setRandom(false).build());
    }

    @Test
    void aTakenIconIsSwappedForARandomFreeOneButTheColourAndFaceStay() {
        PlayerLook look = Looks.assign(new FavouriteLook("ghost", "cyan", "wink"), List.of(look("ghost", "pink")), random);

        assertThat(look.getIcon()).isNotEqualTo("ghost").isIn(Looks.ICONS);
        assertThat(look.getColour()).isEqualTo("cyan");
        assertThat(look.getExpression()).isEqualTo("wink");
        assertThat(look.getRandom()).isTrue();
    }

    @Test
    void iconAndColourAreMatchedSeparately() {
        PlayerLook look = Looks.assign(new FavouriteLook("ghost", "cyan", "happy"), List.of(look("blob", "cyan")), random);

        assertThat(look.getIcon()).isEqualTo("ghost");
        assertThat(look.getColour()).isNotEqualTo("cyan").isIn(Looks.COLOURS);
        assertThat(look.getRandom()).isTrue();
    }

    @Test
    void withoutAFavouriteItsTheFirstFreeOneInTheDefaultOrder() {
        PlayerLook first = Looks.assign(null, List.of(), random);
        PlayerLook second = Looks.assign(null, List.of(first), random);

        assertThat(first).isEqualTo(PlayerLook.newBuilder().setIcon("blob").setColour("pink").setExpression("happy").build());
        assertThat(second.getIcon()).isEqualTo("ghost");
        assertThat(second.getColour()).isEqualTo("cyan");
        assertThat(second.getRandom()).isFalse();
    }

    @Test
    void unknownFavouriteValuesFallBackSafely() {
        PlayerLook look = Looks.assign(new FavouriteLook("dragon", "teal", "wow"), List.of(), random);

        assertThat(look.getIcon()).isIn(Looks.ICONS);
        assertThat(look.getColour()).isIn(Looks.COLOURS);
        assertThat(look.getExpression()).isEqualTo("happy");
    }

    @Test
    void eightPlayersWithTheSameFavouriteAllGetDifferentIconsAndColours() {
        List<PlayerLook> room = new ArrayList<>();
        for (int i = 0; i < 8; i++) {
            room.add(Looks.assign(new FavouriteLook("star", "lime", "gloat"), room, random));
        }

        assertThat(room).extracting(PlayerLook::getIcon).doesNotHaveDuplicates();
        assertThat(room).extracting(PlayerLook::getColour).doesNotHaveDuplicates();
        assertThat(room).extracting(PlayerLook::getExpression).containsOnly("gloat");
        assertThat(room.getFirst().getRandom()).isFalse();
        assertThat(room.subList(1, 8)).allMatch(PlayerLook::getRandom);
    }
}
