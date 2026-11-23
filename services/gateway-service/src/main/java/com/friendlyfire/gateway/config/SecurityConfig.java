package com.friendlyfire.gateway.config;

import com.friendlyfire.gateway.protection.RequestSizeFilter;
import com.friendlyfire.gateway.security.AudienceValidator;
import com.friendlyfire.gateway.security.AuthSettings;
import com.friendlyfire.gateway.security.JsonErrorWriter;
import com.friendlyfire.gateway.security.SignInRules;
import com.friendlyfire.gateway.security.SignInRulesFilter;
import com.friendlyfire.gateway.security.TokenClaims;
import com.friendlyfire.gateway.security.UserResolver;
import com.friendlyfire.gateway.web.ApiErrors;
import com.friendlyfire.proto.friendlyfire.account.v1.AccountServiceGrpc.AccountServiceBlockingStub;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.SupplierJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.header.writers.ContentSecurityPolicyHeaderWriter;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy;
import org.springframework.security.web.header.writers.DelegatingRequestMatcherHeaderWriter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.util.matcher.NegatedRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;
import tools.jackson.databind.json.JsonMapper;

/**
 * Who may call what (FF-103, FF-601, FF-602, FF-618).
 *
 * <ul>
 *   <li>Every /api route needs a valid Auth0 access token (issuer, audience, expiry,
 *       signature), except GET /api/health. Outside production GET /api/health/deep and
 *       /api/docs are public too; in production they're admin-only.</li>
 *   <li>/ws is open: STOMP checks the token on CONNECT (ConnectInterceptor).</li>
 *   <li>Signed-in players must have a verified email, and in production be on the invite
 *       list (SignInRules).</li>
 *   <li>CORS for the frontend's origin only, security headers, request size limit.</li>
 * </ul>
 * Errors use the FF-006 format: 401 UNAUTHENTICATED, 403 FORBIDDEN / EMAIL_NOT_VERIFIED /
 * INVITE_ONLY, 413.
 */
@Configuration(proxyBeanMethods = false)
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    /** JSON only: nothing may load or frame an API response. */
    static final String API_CSP = "default-src 'none'; frame-ancestors 'none'";
    /** Swagger UI (/api/docs) needs its own scripts, styles and images. */
    static final String DOCS_CSP =
            "default-src 'self'; img-src 'self' data:; style-src 'self' 'unsafe-inline'; frame-ancestors 'none'";
    /** springdoc serves Swagger UI next to /api/docs, under /api/swagger-ui/. */
    private static final String[] DOCS = {"/api/docs", "/api/docs/**", "/api/swagger-ui/**"};
    /** How long a signed-in user's Account record is reused before asking Account again. */
    private static final Duration USER_CACHE = Duration.ofMinutes(5);

    @Bean
    AuthSettings authSettings(
            @Value("${friendlyfire.auth.issuer-uri}") String issuerUri,
            @Value("${friendlyfire.auth.audience}") String audience,
            @Value("${friendlyfire.auth.claim-namespace}") String claimNamespace,
            @Value("${friendlyfire.app-env}") String appEnv,
            @Value("${friendlyfire.invite-only}") boolean inviteOnly,
            @Value("${friendlyfire.invited-emails}") String invitedEmails) {
        return AuthSettings.of(issuerUri, audience, claimNamespace, appEnv, inviteOnly, invitedEmails);
    }

    @Bean
    TokenClaims tokenClaims(AuthSettings settings) {
        return new TokenClaims(settings.claimNamespace());
    }

    @Bean
    SignInRules signInRules(AuthSettings settings, TokenClaims claims) {
        return new SignInRules(settings, claims);
    }

    @Bean
    UserResolver userResolver(AccountServiceBlockingStub accounts, TokenClaims claims, Clock clock) {
        return new UserResolver(accounts, claims, USER_CACHE, clock);
    }

    @Bean
    JsonErrorWriter jsonErrorWriter(JsonMapper json) {
        return new JsonErrorWriter(json);
    }

    /**
     * Checks Auth0 access tokens. Auth0's signing keys are fetched on first use (and
     * cached), so the Gateway starts even while Auth0 is unreachable. Without
     * AUTH0_ISSUER_URI every token is refused.
     */
    @Bean
    JwtDecoder jwtDecoder(AuthSettings settings) {
        if (!settings.signInConfigured()) {
            log.warn("AUTH0_ISSUER_URI isn't set, so every request that needs sign-in gets 401 (see FF-003).");
            return token -> {
                throw new BadJwtException("Sign-in isn't set up on this Gateway.");
            };
        }
        OAuth2TokenValidator<Jwt> validator = new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(settings.issuerUri()), new AudienceValidator(settings.audience()));
        return new SupplierJwtDecoder(() -> {
            NimbusJwtDecoder decoder = JwtDecoders.fromIssuerLocation(settings.issuerUri());
            decoder.setJwtValidator(validator);
            return decoder;
        });
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${friendlyfire.frontend-origin}") String frontendOrigin) {
        CorsConfiguration cors = new CorsConfiguration();
        cors.setAllowedOrigins(List.of(frontendOrigin));
        cors.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE"));
        cors.setAllowedHeaders(List.of(HttpHeaders.AUTHORIZATION, HttpHeaders.CONTENT_TYPE, HttpHeaders.ACCEPT));
        cors.setMaxAge(Duration.ofHours(1));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", cors);
        return source;
    }

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http, AuthSettings settings, TokenClaims claims, SignInRules rules, JsonErrorWriter errors)
            throws Exception {
        AuthenticationEntryPoint unauthenticated = (request, response, e) -> {
            response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
            errors.write(response, HttpStatus.UNAUTHORIZED, ApiErrors.unauthenticated());
        };
        AccessDeniedHandler forbidden =
                (request, response, e) -> errors.write(response, HttpStatus.FORBIDDEN, ApiErrors.forHttpStatus(HttpStatus.FORBIDDEN));

        http
                .csrf(AbstractHttpConfigurer::disable) // bearer tokens, no cookies
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(requests -> {
                    requests.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR).permitAll();
                    requests.requestMatchers("/api/health", "/ws", "/ws/**").permitAll();
                    if (settings.production()) {
                        requests.requestMatchers("/api/health/deep").hasRole("ADMIN");
                        requests.requestMatchers(DOCS).hasRole("ADMIN");
                    } else {
                        requests.requestMatchers("/api/health/deep").permitAll();
                        requests.requestMatchers(DOCS).permitAll();
                    }
                    requests.anyRequest().authenticated();
                })
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(token -> new JwtAuthenticationToken(
                                token,
                                claims.isAdmin(token) ? List.of(new SimpleGrantedAuthority("ROLE_ADMIN")) : List.of(),
                                token.getSubject())))
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(forbidden))
                .exceptionHandling(handling -> handling
                        .authenticationEntryPoint(unauthenticated)
                        .accessDeniedHandler(forbidden))
                .addFilterAfter(new RequestSizeFilter(errors), CorsFilter.class)
                .addFilterAfter(new SignInRulesFilter(rules, errors), BearerTokenAuthenticationFilter.class)
                .headers(headers -> {
                    RequestMatcher docs = docsMatcher();
                    headers.httpStrictTransportSecurity(hsts -> hsts
                                    .includeSubDomains(true)
                                    .maxAgeInSeconds(Duration.ofDays(365).toSeconds()))
                            .frameOptions(frame -> frame.deny())
                            .referrerPolicy(referrer -> referrer.policy(ReferrerPolicy.NO_REFERRER))
                            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                    new NegatedRequestMatcher(docs), new ContentSecurityPolicyHeaderWriter(API_CSP)))
                            .addHeaderWriter(new DelegatingRequestMatcherHeaderWriter(
                                    docs, new ContentSecurityPolicyHeaderWriter(DOCS_CSP)));
                });
        return http.build();
    }

    private static RequestMatcher docsMatcher() {
        PathPatternRequestMatcher.Builder paths = PathPatternRequestMatcher.withDefaults();
        List<RequestMatcher> matchers = Arrays.stream(DOCS).<RequestMatcher>map(paths::matcher).toList();
        return new OrRequestMatcher(matchers);
    }
}
