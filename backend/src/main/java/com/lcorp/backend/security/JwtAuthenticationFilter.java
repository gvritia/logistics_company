package com.lcorp.backend.security;

import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.repository.UserAccountRepository;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtService jwt;
    private final UserAccountRepository users;

    public JwtAuthenticationFilter(JwtService jwt, UserAccountRepository users) {
        this.jwt = jwt;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String token = bearer(request);
        if (token == null && isSwaggerRequest(request)) {
            token = swaggerCookie(request);
        }
        if (token != null) {
            try {
                JwtService.ParsedToken parsed = jwt.parse(token);
                users.findById(parsed.userId())
                    .filter(user -> user.isEnabled() && user.getAuthVersion() == parsed.authVersion())
                    .ifPresent(user -> {
                    var authorities = user.getRoles().stream()
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.name())).toList();
                    var principal = new AuthPrincipal(user.getId(), user.isPasswordChangeRequired());
                    var authentication = new UsernamePasswordAuthenticationToken(principal, null, authorities);
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                });
            } catch (JwtException | IllegalArgumentException ignored) {
                SecurityContextHolder.clearContext();
            }
        }
        chain.doFilter(request, response);
    }

    private String bearer(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) return null;
        String token = header.substring(7);
        return token.length() <= 4096 ? token : null;
    }

    private boolean isSwaggerRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/swagger-ui.html") || path.startsWith("/swagger-ui/")
            || path.equals("/v3/api-docs") || path.startsWith("/v3/api-docs/");
    }

    private String swaggerCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("logistics_swagger".equals(cookie.getName()) && cookie.getValue().length() <= 4096) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
