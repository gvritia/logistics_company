package com.lcorp.backend.error;

import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(AuthFailure.class)
    public ResponseEntity<ApiError> auth(AuthFailure ex, HttpServletRequest request) {
        return error(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(RateLimitFailure.class)
    public ResponseEntity<ApiError> limited(RateLimitFailure ex, HttpServletRequest request) {
        return error(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalid(IllegalArgumentException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", ex.getMessage(), request, Map.of());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiError> malformed(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_JSON", "Неверный формат JSON", request, Map.of());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> fields = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(field ->
            fields.putIfAbsent(field.getField(), field.getDefaultMessage()));
        return error(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Проверьте поля запроса", request, fields);
    }

    private ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
                                            HttpServletRequest request, Map<String, String> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(), code,
            message, request.getRequestURI(), CorrelationIdFilter.get(request), fields));
    }
}
