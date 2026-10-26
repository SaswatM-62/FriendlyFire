package com.friendlyfire.account.health;

import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import java.time.Duration;
import org.springframework.web.client.RestClient;

/** GETs a health path on an HTTP dependency; any 2xx answer means UP. */
public class HttpDependencyCheck implements DependencyCheck {

    private final String name;
    private final RestClient client;
    private final String path;

    public HttpDependencyCheck(String name, RestClient client, String path) {
        this.name = name;
        this.client = client;
        this.path = path;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public ComponentHealth check(Duration deadline) {
        client.get().uri(path).retrieve().toBodilessEntity();
        return DependencyCheck.up();
    }
}
