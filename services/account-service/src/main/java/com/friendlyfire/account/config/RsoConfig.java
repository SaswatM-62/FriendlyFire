package com.friendlyfire.account.config;

import com.friendlyfire.account.rso.MockRsoClient;
import com.friendlyfire.account.rso.RsoClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

/** Which Riot Sign-On Account talks to. */
@Configuration(proxyBeanMethods = false)
public class RsoConfig {

    // TODO(ff): choose RiotRsoClient from an env var once Riot approves the app (FF-614)
    @Bean
    RsoClient rsoClient(
            @Qualifier("mockRsoRestClient") RestClient mockRso,
            @Value("${friendlyfire.mock-rso-browser-url}") String browserUrl) {
        return new MockRsoClient(mockRso, browserUrl);
    }
}
