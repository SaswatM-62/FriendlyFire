package com.friendlyfire.gateway.security;

import com.friendlyfire.gateway.web.ErrorResponse;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * Writes an error in the FF-006 format from a servlet filter, where the
 * GlobalExceptionHandler can't help (sign-in failures happen before any controller).
 */
public final class JsonErrorWriter {

    private final JsonMapper json;

    public JsonErrorWriter(JsonMapper json) {
        this.json = json;
    }

    public void write(HttpServletResponse response, HttpStatus status, ErrorResponse body) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getOutputStream().write(json.writeValueAsBytes(body));
    }
}
