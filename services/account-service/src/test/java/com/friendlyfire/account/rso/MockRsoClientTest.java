package com.friendlyfire.account.rso;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

class MockRsoClientTest {

    @Test
    void theAuthorizeUrlIsTheBrowsersAddressWithEveryParameterEncoded() {
        MockRsoClient rso = new MockRsoClient(RestClient.create("http://mock-rso-service:8090"), "http://localhost:8090/");

        String url = rso.authorizeUrl("abc-123_XYZ", "http://localhost:3000/link/riot/callback");

        assertThat(url).isEqualTo("http://localhost:8090/authorize"
                + "?response_type=code"
                + "&client_id=friendlyfire"
                + "&redirect_uri=http%3A%2F%2Flocalhost%3A3000%2Flink%2Friot%2Fcallback"
                + "&scope=openid"
                + "&state=abc-123_XYZ");
    }
}
