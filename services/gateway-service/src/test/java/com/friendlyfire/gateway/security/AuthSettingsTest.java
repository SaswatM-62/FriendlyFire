package com.friendlyfire.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;

class AuthSettingsTest {

    @Test
    void readsTheInviteListInLowerCase() {
        AuthSettings settings = AuthSettings.of("", "aud", "ns/", "prod", true, " Sam@Example.com, ,kai@example.com ");

        assertThat(settings.invitedEmails()).isEqualTo(Set.of("sam@example.com", "kai@example.com"));
    }

    @Test
    void theInviteListAppliesOnlyInProductionAndCanBeTurnedOff() {
        assertThat(AuthSettings.of("", "aud", "ns/", "prod", true, "").inviteListApplies()).isTrue();
        assertThat(AuthSettings.of("", "aud", "ns/", "prod", false, "").inviteListApplies()).isFalse();
        assertThat(AuthSettings.of("", "aud", "ns/", "local", true, "").inviteListApplies()).isFalse();
    }

    @Test
    void signInIsConfiguredOnceThereIsAnIssuer() {
        assertThat(AuthSettings.of("  ", "aud", "ns/", "local", true, "").signInConfigured()).isFalse();
        assertThat(AuthSettings.of("https://ff.auth0.com/", "aud", "ns/", "local", true, "").signInConfigured()).isTrue();
    }
}
