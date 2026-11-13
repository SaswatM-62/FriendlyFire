package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.security.SignedInUser;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.utils.SpringDocUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * /api/docs: every REST endpoint with its request and response bodies (FF-005). The STOMP
 * messages are in docs/realtime-messages.md.
 */
@Configuration(proxyBeanMethods = false)
public class OpenApiConfig {

    private static final String AUTH0 = "auth0";

    @Bean
    OpenAPI gatewayApi() {
        // Filled in from the token, not sent by the browser.
        SpringDocUtils.getConfig().addRequestWrapperToIgnore(SignedInUser.class, Jwt.class);
        return new OpenAPI()
                .info(new Info()
                        .title("FriendlyFire Gateway")
                        .version("v1")
                        .description("The browser's REST API. Every route except /api/health needs an Auth0 access "
                                + "token for the Gateway's audience. Errors are {\"error\": {\"code\", \"message\"}}."))
                .components(new Components().addSecuritySchemes(AUTH0, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")))
                .addSecurityItem(new SecurityRequirement().addList(AUTH0));
    }
}
