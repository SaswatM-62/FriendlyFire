package com.friendlyfire.gateway.security;

import com.friendlyfire.gateway.web.ApiException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * After the token is checked: refuses players who may not use the app (SignInRules) with
 * 403 and a code the frontend sends to the right page. Health checks are left alone.
 * Part of the security filter chain only (SecurityConfig), so it's not a bean.
 */
public final class SignInRulesFilter extends OncePerRequestFilter {

    private final SignInRules rules;
    private final JsonErrorWriter errors;

    public SignInRulesFilter(SignInRules rules, JsonErrorWriter errors) {
        this.rules = rules;
        this.errors = errors;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/api/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken token) {
            try {
                rules.check(token.getToken());
            } catch (ApiException e) {
                errors.write(response, e.status(), e.body());
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
