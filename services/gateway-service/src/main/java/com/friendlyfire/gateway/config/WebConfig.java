package com.friendlyfire.gateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration(proxyBeanMethods = false)
public class WebConfig implements WebMvcConfigurer {

    private final String frontendOrigin;

    public WebConfig(@Value("${friendlyfire.frontend-origin}") String frontendOrigin) {
        this.frontendOrigin = frontendOrigin;
    }

    /** GET /api/health is Actuator's simple health group: is the Gateway running? */
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        registry.addViewController("/api/health").setViewName("forward:/actuator/health/simple");
    }

    /** Only the frontend (FRONTEND_ORIGIN) may call the API from a browser. */
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(frontendOrigin)
                .allowedMethods("GET", "POST", "PATCH", "DELETE");
    }
}
