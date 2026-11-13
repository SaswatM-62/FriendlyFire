package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.security.SignedInUserArgumentResolver;
import com.friendlyfire.gateway.security.UserResolver;
import java.util.List;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Spring MVC. CORS is in SecurityConfig. */
@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final UserResolver users;

    public WebConfig(UserResolver users) {
        this.users = users;
    }

    /** GET /api/health is Actuator's simple health group: is the Gateway running? */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/api/health").setViewName("forward:/actuator/health/simple");
    }

    /** Controllers declare a SignedInUser parameter to get the player making the request. */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(new SignedInUserArgumentResolver(users));
    }
}
