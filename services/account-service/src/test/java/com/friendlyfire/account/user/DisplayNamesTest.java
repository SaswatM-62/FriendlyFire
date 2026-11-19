package com.friendlyfire.account.user;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class DisplayNamesTest {

    @Test
    void aNameIsTrimmed() {
        assertThat(DisplayNames.valid("  Sam Rivera ")).contains("Sam Rivera");
    }

    @Test
    void oneTo24CharactersAreAllowed() {
        assertThat(DisplayNames.valid("S")).contains("S");
        assertThat(DisplayNames.valid("a".repeat(24))).contains("a".repeat(24));
        assertThat(DisplayNames.valid("a".repeat(25))).isEmpty();
    }

    @Test
    void anEmojiCountsAsOneCharacter() {
        assertThat(DisplayNames.valid("😀".repeat(24))).isPresent();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void anEmptyNameIsNotAllowed(String name) {
        assertThat(DisplayNames.valid(name)).isEmpty();
    }
}
