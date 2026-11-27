package com.friendlyfire.account.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class FavouriteLookTest {

    @Test
    void aLookFromTheOffersIsValid() {
        assertThat(FavouriteLook.valid("ghost", "cyan", "locked-in"))
                .contains(new FavouriteLook("ghost", "cyan", "locked-in"));
    }

    @Test
    void anUnknownIconColourOrExpressionIsNot() {
        assertThat(FavouriteLook.valid("dragon", "cyan", "happy")).isEmpty();
        assertThat(FavouriteLook.valid("ghost", "teal", "happy")).isEmpty();
        assertThat(FavouriteLook.valid("ghost", "cyan", "wow")).isEmpty();
        assertThat(FavouriteLook.valid("", "", "")).isEmpty();
        assertThat(FavouriteLook.valid("Ghost", "cyan", "happy")).as("IDs are lower case").isEmpty();
    }

    @Test
    void theOffersMatchTheFrontend() {
        // frontend/src/lib/characters.ts: 10 characters, 10 colours, 8 pickable faces.
        assertThat(FavouriteLook.ICONS).hasSize(10).doesNotHaveDuplicates();
        assertThat(FavouriteLook.COLOURS).hasSize(10).doesNotHaveDuplicates();
        assertThat(FavouriteLook.EXPRESSIONS).hasSize(8).doesNotHaveDuplicates();
    }
}
