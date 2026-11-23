package com.friendlyfire.gateway.protection;

import com.friendlyfire.gateway.security.JsonErrorWriter;
import com.friendlyfire.gateway.web.ApiErrors;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.http.HttpStatus;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Refuses request bodies over {@link #MAX_BYTES} with 413 before anything reads them
 * (FF-601). Every body the frontend sends is a few small JSON fields. Part of the
 * security filter chain only (SecurityConfig), so it's not a bean.
 */
public final class RequestSizeFilter extends OncePerRequestFilter {

    public static final int MAX_BYTES = 16 * 1024;

    private final JsonErrorWriter errors;

    public RequestSizeFilter(JsonErrorWriter errors) {
        this.errors = errors;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (request.getContentLengthLong() > MAX_BYTES) {
            errors.write(response, HttpStatus.CONTENT_TOO_LARGE, ApiErrors.forHttpStatus(HttpStatus.CONTENT_TOO_LARGE));
            return;
        }
        chain.doFilter(request, response);
    }
}
