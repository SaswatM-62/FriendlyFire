package com.friendlyfire.account.health;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static org.assertj.core.api.Assertions.assertThat;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentStatus;
import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

/** The mock RSO is replaced by WireMock, which answers /healthz with HTTP 500. */
// A real gRPC server on a random port, and match-data-service at an address where
// nothing listens. (@AutoConfigureTestGrpcTransport would route every channel back to
// this service, so its match-data-service check would call itself over and over.)
@SpringBootTest(properties = {
    "spring.grpc.server.port=0",
    "spring.grpc.client.channel.matchdata.target=localhost:1",
    // Flyway needs a database at startup; this test doesn't use one.
    "spring.flyway.enabled=false",
})
@EnableWireMock(@ConfigureWireMock(name = "mock-rso", baseUrlProperties = "friendlyfire.mock-rso-url"))
class MockRsoHealthIT {

    @InjectWireMock("mock-rso")
    private WireMockServer mockRso;

    @Autowired
    private DependencyHealthChecker checker;

    @Test
    void reportsMockRsoDownWhenItsHealthCheckFails() {
        mockRso.stubFor(get("/healthz").willReturn(serverError()));
        // Warm up: on a slow CI runner the very first call can miss the 1-second deadline.
        checker.check();

        ComponentHealth mockRsoNode = checker.check().getDependenciesList().stream()
                .filter(node -> node.getName().equals("mock-rso-service"))
                .findFirst()
                .orElseThrow();

        assertThat(mockRsoNode.getStatus()).isEqualTo(ComponentStatus.COMPONENT_STATUS_DOWN);
        assertThat(mockRsoNode.getError()).isEqualTo("HTTP 500");
    }
}
