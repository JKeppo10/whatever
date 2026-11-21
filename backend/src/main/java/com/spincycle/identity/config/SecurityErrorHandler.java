package com.spincycle.identity.config;

import com.spincycle.shared.api.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationTrustResolver;
import org.springframework.security.authentication.AuthenticationTrustResolverImpl;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

/**
 * Writes 401 and 403 from the security filters in the shared error body (section 8.1).
 * These happen before any controller runs, so GlobalExceptionHandler never sees them.
 */
@Component
public class SecurityErrorHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    private final ObjectMapper mapper;
    private final AuthenticationTrustResolver trustResolver = new AuthenticationTrustResolverImpl();

    public SecurityErrorHandler(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    /** No session, or an expired one. */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException ex) throws IOException {
        write(request, response, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Please sign in.");
    }

    /** Wrong role, or a missing or wrong CSRF token. */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response,
                       AccessDeniedException ex) throws IOException {
        // The CSRF check runs before the sign-in check, so a visitor's POST without a token lands here.
        // A visitor must still get 401 (NFR06), not 403.
        if (!trustResolver.isAuthenticated(SecurityContextHolder.getContext().getAuthentication())) {
            commence(request, response, null);
            return;
        }
        write(request, response, HttpStatus.FORBIDDEN, "FORBIDDEN", "You are not allowed to do this.");
    }

    private void write(HttpServletRequest request, HttpServletResponse response,
                       HttpStatus status, String code, String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        mapper.writeValue(response.getOutputStream(),
                new ApiError(status.value(), code, message, request.getRequestURI(), List.of()));
    }
}
