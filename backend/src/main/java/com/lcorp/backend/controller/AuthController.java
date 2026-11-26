package com.lcorp.backend.controller;

import com.lcorp.backend.dto.AuthDtos.AuthResponse;
import com.lcorp.backend.dto.AuthDtos.ChangePasswordRequest;
import com.lcorp.backend.dto.AuthDtos.CurrentUser;
import com.lcorp.backend.dto.AuthDtos.EmailAuthMode;
import com.lcorp.backend.dto.AuthDtos.EmailCodeConfirmation;
import com.lcorp.backend.dto.AuthDtos.EmailCodeLogin;
import com.lcorp.backend.dto.AuthDtos.EmailCodeRequest;
import com.lcorp.backend.dto.AuthDtos.LoginRequest;
import com.lcorp.backend.dto.AuthDtos.RegisterRequest;
import com.lcorp.backend.dto.AuthDtos.RefreshRequest;
import com.lcorp.backend.domain.Role;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.security.LoginRateLimiter;
import com.lcorp.backend.security.RegistrationRateLimiter;
import com.lcorp.backend.service.AuthService;
import com.lcorp.backend.service.EmailLoginService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.time.Instant;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    private final EmailLoginService emailLogin;
    private final LoginRateLimiter limiter;
    private final RegistrationRateLimiter registrationLimiter;
    private final boolean cookieSecure;

    public AuthController(AuthService auth, EmailLoginService emailLogin, LoginRateLimiter limiter,
                          RegistrationRateLimiter registrationLimiter,
                          @Value("${app.cookie.secure:false}") boolean cookieSecure) {
        this.auth = auth;
        this.emailLogin = emailLogin;
        this.limiter = limiter;
        this.registrationLimiter = registrationLimiter;
        this.cookieSecure = cookieSecure;
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr());
        return withSwaggerCookie(auth.login(body.email(), body.password(), CorrelationIdFilter.get(request)), request);
    }

    @PostMapping("/email/start")
    public ResponseEntity<EmailAuthMode> startEmailAuth(@Valid @RequestBody EmailCodeRequest body,
                                                       HttpServletRequest request) {
        limiter.check("email:" + request.getRemoteAddr());
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store").body(emailLogin.start(body.email()));
    }

    @PostMapping("/email/request")
    public ResponseEntity<Void> requestEmailLogin(@Valid @RequestBody EmailCodeRequest body,
                                                  HttpServletRequest request) {
        limiter.check("email:" + request.getRemoteAddr());
        emailLogin.requestLogin(body.email());
        return ResponseEntity.noContent().header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }

    @PostMapping("/email/verify")
    public ResponseEntity<AuthResponse> verifyEmailLogin(@Valid @RequestBody EmailCodeLogin body,
                                                          HttpServletRequest request) {
        limiter.check("email:" + request.getRemoteAddr());
        return withSwaggerCookie(auth.loginByEmail(body.email(), body.code(), body.password(),
            CorrelationIdFilter.get(request)), request);
    }

    @PostMapping("/email/enroll/request")
    public ResponseEntity<Void> requestEmailEnrollment(@AuthenticationPrincipal AuthPrincipal principal,
                                                       HttpServletRequest request) {
        limiter.check("email:" + request.getRemoteAddr());
        emailLogin.requestEnrollment(principal.id());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/email/enroll/verify")
    public ResponseEntity<Void> confirmEmailEnrollment(@AuthenticationPrincipal AuthPrincipal principal,
                                                       @Valid @RequestBody EmailCodeConfirmation body,
                                                       HttpServletRequest request) {
        limiter.check("email:" + request.getRemoteAddr());
        emailLogin.confirmEnrollment(principal.id(), body.code());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/register")
    public ResponseEntity<CurrentUser> register(@Valid @RequestBody RegisterRequest body,
                                                HttpServletRequest request) {
        registrationLimiter.check(request.getRemoteAddr());
        CurrentUser user = auth.register(body.email(), body.displayName(), body.password(),
            CorrelationIdFilter.get(request));
        return ResponseEntity.status(HttpStatus.CREATED).header(HttpHeaders.CACHE_CONTROL, "no-store").body(user);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest body, HttpServletRequest request) {
        limiter.check(request.getRemoteAddr());
        return withSwaggerCookie(auth.refresh(body.refreshToken(), CorrelationIdFilter.get(request)), request);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody RefreshRequest body, HttpServletRequest request) {
        auth.logout(body.refreshToken(), CorrelationIdFilter.get(request));
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE,
            ResponseCookie.from("logistics_swagger", "").httpOnly(true)
                .secure(cookieSecure || request.isSecure()).sameSite("Strict").path("/")
                .maxAge(Duration.ZERO).build().toString()).build();
    }

    @GetMapping("/me")
    public CurrentUser me(@AuthenticationPrincipal AuthPrincipal principal) {
        return auth.me(principal.id());
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@AuthenticationPrincipal AuthPrincipal principal,
                                               @Valid @RequestBody ChangePasswordRequest body,
                                               HttpServletRequest request) {
        auth.changePassword(principal.id(), body.currentPassword(), body.newPassword(),
            CorrelationIdFilter.get(request));
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<AuthResponse> withSwaggerCookie(AuthResponse response, HttpServletRequest request) {
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store");
        if (!response.user().passwordChangeRequired() && response.user().roles().contains(Role.ADMIN)) {
            long seconds = Math.max(0, Duration.between(Instant.now(), response.accessExpiresAt()).toSeconds());
            builder.header(HttpHeaders.SET_COOKIE, ResponseCookie.from("logistics_swagger", response.accessToken())
                .httpOnly(true).secure(cookieSecure || request.isSecure()).sameSite("Strict")
                .path("/").maxAge(Duration.ofSeconds(seconds)).build().toString());
        }
        return builder.body(response);
    }
}
