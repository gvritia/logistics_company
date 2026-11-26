package com.lcorp.backend.security;

import com.lcorp.backend.error.RateLimitFailure;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.concurrent.ConcurrentHashMap;

// Ограничение по реальному адресу соединения; X-Forwarded-For не принимается на веру
@Component
public class LoginRateLimiter {
    private static final int MAX_ATTEMPTS = 20;
    private static final Duration WINDOW = Duration.ofMinutes(5);
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> attempts = new ConcurrentHashMap<>();

    public void check(String remoteAddress) {
        String key = remoteAddress == null ? "unknown" : remoteAddress;
        Instant now = Instant.now();
        ArrayDeque<Instant> bucket = attempts.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (bucket) {
            while (!bucket.isEmpty() && bucket.peekFirst().isBefore(now.minus(WINDOW))) {
                bucket.removeFirst();
            }
            if (bucket.size() >= MAX_ATTEMPTS) {
                throw new RateLimitFailure();
            }
            bucket.addLast(now);
        }
        if (attempts.size() > 10_000) {
            attempts.entrySet().removeIf(entry -> {
                synchronized (entry.getValue()) {
                    return entry.getValue().isEmpty()
                        || entry.getValue().peekLast().isBefore(now.minus(WINDOW));
                }
            });
        }
    }
}
