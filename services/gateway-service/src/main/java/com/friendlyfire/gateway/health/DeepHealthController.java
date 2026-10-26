package com.friendlyfire.gateway.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/health/deep: the whole system's health tree. Always HTTP 200; problems are
 * reported in the tree. (GET /api/health, the simple "am I running?" check, is
 * Actuator's: see management.endpoint.health in application.yml.)
 */
@RestController
public class DeepHealthController {

    private final DependencyHealthChecker checker;

    public DeepHealthController(DependencyHealthChecker checker) {
        this.checker = checker;
    }

    @GetMapping("/api/health/deep")
    public HealthNode deep() {
        return HealthNode.from(checker.check());
    }
}
