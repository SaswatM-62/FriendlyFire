package com.friendlyfire.account.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/** HTTP clients for the APIs account-service calls. */
@Configuration(proxyBeanMethods = false)
public class HttpClientsConfig {

    /** mock-rso-service (MOCK_RSO_URL), which stands in for Riot Sign-On. */
    @Bean
    RestClient mockRsoRestClient(RestClient.Builder builder, @Value("${friendlyfire.mock-rso-url}") String baseUrl) {
        // HTTP/1.1: the JDK client would otherwise try an HTTP/2 upgrade on every plain-http call.
        HttpClient http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(2))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(http);
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
