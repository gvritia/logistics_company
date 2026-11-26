package com.lcorp.backend.security;

import com.lcorp.backend.domain.UserAccount;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {
    private final SecretKey key;
    private final String issuer;
    private final int accessMinutes;
    private final int refreshDays;

    public JwtService(@Value("${app.jwt.secret-base64}") String secretBase64,
                      @Value("${app.jwt.issuer}") String issuer,
                      @Value("${app.jwt.access-minutes}") int accessMinutes,
                      @Value("${app.jwt.refresh-days}") int refreshDays) {
        byte[] secret;
        try {
            secret = Base64.getDecoder().decode(secretBase64);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("JWT_SECRET_BASE64 must contain valid Base64", ex);
        }
        if (secret.length < 32 || issuer.isBlank() || accessMinutes < 1 || accessMinutes > 60
            || refreshDays < 1 || refreshDays > 30) {
            throw new IllegalStateException("Invalid JWT configuration");
        }
        this.key = Keys.hmacShaKeyFor(secret);
        this.issuer = issuer;
        this.accessMinutes = accessMinutes;
        this.refreshDays = refreshDays;
    }

    public IssuedToken issue(UserAccount user) {
        Instant now = Instant.now();
        Instant expiry = now.plus(accessMinutes, ChronoUnit.MINUTES);
        String token = Jwts.builder()
            .issuer(issuer)
            .subject(user.getId().toString())
            .claim("typ", "access")
            .claim("av", user.getAuthVersion())
            .issuedAt(Date.from(now))
            .expiration(Date.from(expiry))
            .id(UUID.randomUUID().toString())
            .signWith(key, Jwts.SIG.HS256)
            .compact();
        return new IssuedToken(token, expiry);
    }

    public ParsedToken parse(String token) {
        Claims claims = Jwts.parser().verifyWith(key).requireIssuer(issuer)
            .require("typ", "access").build().parseSignedClaims(token).getPayload();
        Number authVersion = claims.get("av", Number.class);
        if (authVersion == null) throw new IllegalArgumentException("Missing auth version");
        return new ParsedToken(UUID.fromString(claims.getSubject()), authVersion.longValue());
    }

    public int refreshDays() {
        return refreshDays;
    }

    public record IssuedToken(String value, Instant expiresAt) {
    }

    public record ParsedToken(UUID userId, long authVersion) {
    }
}
