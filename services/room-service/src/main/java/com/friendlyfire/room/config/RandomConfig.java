package com.friendlyfire.room.config;

import java.security.SecureRandom;
import java.util.random.RandomGenerator;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class RandomConfig {

    @Bean
    RandomGenerator randomGenerator() {
        return new SecureRandom();
    }
}