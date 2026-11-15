package com.lcorp.backend.security;

import com.lcorp.backend.error.RateLimitFailure;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RegistrationRateLimiter {
    private static final int MAX_ATTEMPTS = 5;
    private static final Duration WINDOW = Duration.ofHours(1);
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public void check(String remoteAddress) {
        String key = remoteAddress == null ? "unknown" : remoteAddress;
        Instant now = Instant.now();
        ArrayDeque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && !bucket.peekFirst().isAfter(now.minus(WINDOW))) {
                bucket.removeFirst();
            }
            if (bucket.size() >= MAX_ATTEMPTS) {
                throw new RateLimitFailure("Слишком много попыток регистрации. Повторите позже");
            }
            bucket.addLast(now);
        }
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(entry -> {
                synchronized (entry.getValue()) {
                    return entry.getValue().isEmpty()
                        || !entry.getValue().peekLast().isAfter(now.minus(WINDOW));
                }
            });
        }
    }
}
