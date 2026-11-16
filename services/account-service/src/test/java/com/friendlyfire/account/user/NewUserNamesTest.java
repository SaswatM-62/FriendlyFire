package com.friendlyfire.account.user;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.random.RandomGenerator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class NewUserNamesTest {

    /** Always draws 42, so the fallback name is "Player 0042". */
    private static final RandomGenerator FORTY_TWO = new RandomGenerator() {
        @Override
        public long nextLong() {
            return 42;
        }

        @Override
        public int nextInt(int bound) {
            return 42;
        }
    };

    private final NewUserNames names = new NewUserNames(FORTY_TWO);

    @Test
    void keepsANormalNameAndTrimsIt() {
        assertThat(names.from("Sam")).isEqualTo("Sam");
        assertThat(names.from("  Sam Rivera \t")).isEqualTo("Sam Rivera");
    }

    @Test
    void cutsALongNameTo24Characters() {
        assertThat(names.from("Maximilian Bartholomew Featherstone")).isEqualTo("Maximilian Bartholomew F");
    }

    @Test
    void neverCutsAnEmojiInHalf() {
        String name = "a".repeat(23) + "😀😀";

        assertThat(names.from(name)).isEqualTo("a".repeat(23) + "😀");
    }

    @Test
    void trimsAgainWhenTheCutEndsOnASpace() {
        String name = "a".repeat(23) + " bcd";

        assertThat(names.from(name)).isEqualTo("a".repeat(23));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t\n"})
    void anEmptyNameBecomesPlayerAndFourDigits(String suggested) {
        assertThat(names.from(suggested)).isEqualTo("Player 0042");
    }

    // Auth0 often uses the email address as an email/password account's name.
    @ParameterizedTest
    @ValueSource(strings = {"sam@example.com", "Sam @ home"})
    void aNameWithAnAtSignBecomesPlayerAndFourDigits(String suggested) {
        assertThat(names.from(suggested)).isEqualTo("Player 0042");
    }

    @Test
    void theFallbackNameAlwaysHasFourDigits() {
        NewUserNames realRandom = new NewUserNames(RandomGenerator.getDefault());

        for (int i = 0; i < 200; i++) {
            assertThat(realRandom.from("")).matches("Player \\d{4}");
        }
    }
}
