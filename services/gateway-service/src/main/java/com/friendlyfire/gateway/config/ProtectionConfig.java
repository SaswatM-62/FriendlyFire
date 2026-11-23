package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.protection.RateLimiter;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Protection at the edge (FF-601). Headers, CORS and the size limit are in SecurityConfig. */
@Configuration(proxyBeanMethods = false)
public class ProtectionConfig {

    @Bean
    RateLimiter rateLimiter(Clock clock) {
        return new RateLimiter(clock);
    }
}
