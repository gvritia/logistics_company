package com.lcorp.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lcorp.backend.error.ApiError;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

@Component
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {
    private static final Set<String> ALLOWED = Set.of("/api/auth/login", "/api/auth/me", "/api/auth/change-password",
        "/api/auth/logout", "/api/auth/refresh", "/actuator/health", "/api/system/info");
    private final ObjectMapper mapper;

    public PasswordChangeRequiredFilter(ObjectMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthPrincipal principal
            && principal.passwordChangeRequired() && !ALLOWED.contains(request.getRequestURI())) {
            response.setStatus(403);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            mapper.writeValue(response.getOutputStream(), new ApiError(Instant.now(), 403,
                "PASSWORD_CHANGE_REQUIRED", "Сначала смените временный пароль",
                request.getRequestURI(), CorrelationIdFilter.get(request), Map.of()));
            return;
        }
        chain.doFilter(request, response);
    }
}
