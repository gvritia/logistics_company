package com.lcorp.backend.error;

public class RateLimitFailure extends RuntimeException {
    public RateLimitFailure() {
        super("Слишком много попыток входа. Повторите позже");
    }

    public RateLimitFailure(String message) {
        super(message);
    }
}
