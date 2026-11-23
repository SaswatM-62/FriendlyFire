package com.friendlyfire.gateway.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.catchThrowableOfType;

import com.friendlyfire.gateway.web.ApiException;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

class SignInRulesTest {

    private static final String NS = "https://friendlyfire.dev/";
    private static final TokenClaims CLAIMS = new TokenClaims(NS);

    @Test
    void verifiedPlayersGetInOutsideProduction() {
        assertThatCode(() -> rules("local").check(player("maya@example.com", true, false))).doesNotThrowAnyException();
    }

    @Test
    void anUnverifiedEmailIsRefused() {
        ApiException error = catchThrowableOfType(ApiException.class,
                () -> rules("local").check(player("maya@example.com", false, false)));

        assertThat(error.status()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(error.code()).isEqualTo("EMAIL_NOT_VERIFIED");
    }

    @Test
    void productionLetsInInvitedEmailsWhateverTheirCase() {
        assertThatCode(() -> rules("prod").check(player("SAM@example.com", true, false))).doesNotThrowAnyException();
    }

    @Test
    void productionRefusesEveryoneElse() {
        ApiException error = catchThrowableOfType(ApiException.class,
                () -> rules("prod").check(player("maya@example.com", true, false)));

        assertThat(error.code()).isEqualTo("INVITE_ONLY");
    }

    @Test
    void adminsAlwaysGetIn() {
        assertThatCode(() -> rules("prod").check(player("pm@example.com", true, true))).doesNotThrowAnyException();
    }

    private static SignInRules rules(String appEnv) {
        return new SignInRules(AuthSettings.of("", "aud", NS, appEnv, true, "sam@example.com"), CLAIMS);
    }

    private static Jwt player(String email, boolean verified, boolean admin) {
        return Jwt.withTokenValue("t")
                .header("alg", "RS256")
                .subject("auth0|x")
                .issuedAt(Instant.now())
                .claim(NS + "email", email)
                .claim(NS + "email_verified", verified)
                .claim(NS + "roles", admin ? List.of("admin") : List.of())
                .build();
    }
}
