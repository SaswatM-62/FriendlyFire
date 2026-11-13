package com.friendlyfire.gateway.testsupport;

import io.grpc.ManagedChannel;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.convention.TestBean;

/**
 * The whole Gateway on a random port (MockMvc for REST, a real STOMP client for /ws), with
 * account-, room- and scoring-service replaced by in-process fakes (FakeServices) and
 * Auth0 by FakeTokens. Tests that share this setup share one Spring context, so they use
 * their own player names and room codes rather than relying on a clean slate.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
public abstract class GatewayTestBase {

    @TestBean(methodName = "com.friendlyfire.gateway.testsupport.FakeServices#accountChannel")
    private ManagedChannel accountChannel;

    @TestBean(methodName = "com.friendlyfire.gateway.testsupport.FakeServices#roomChannel")
    private ManagedChannel roomChannel;

    @TestBean(methodName = "com.friendlyfire.gateway.testsupport.FakeServices#scoringChannel")
    private ManagedChannel scoringChannel;

    @TestBean(methodName = "com.friendlyfire.gateway.testsupport.FakeTokens#decoder")
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void resetFakeServices() {
        FakeServices.reset();
    }
}
