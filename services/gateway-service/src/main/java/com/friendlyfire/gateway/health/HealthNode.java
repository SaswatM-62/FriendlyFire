package com.friendlyfire.gateway.health;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.friendlyfire.proto.friendlyfire.health.v1.ComponentHealth;
import java.util.List;

/** One node of GET /api/health/deep: the JSON form of ComponentHealth. */
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonPropertyOrder({"name", "status", "latencyMs", "error", "dependencies"})
public record HealthNode(String name, String status, int latencyMs, String error, List<HealthNode> dependencies) {

    static HealthNode from(ComponentHealth component) {
        return new HealthNode(
                component.getName(),
                switch (component.getStatus()) {
                    case COMPONENT_STATUS_UP -> "UP";
                    case COMPONENT_STATUS_DEGRADED -> "DEGRADED";
                    case COMPONENT_STATUS_DOWN -> "DOWN";
                    default -> "UNKNOWN";
                },
                component.getLatencyMs(),
                component.getError().isEmpty() ? null : component.getError(),
                component.getDependenciesList().stream().map(HealthNode::from).toList());
    }
}
