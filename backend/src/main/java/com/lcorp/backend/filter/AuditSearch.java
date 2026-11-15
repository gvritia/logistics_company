package com.lcorp.backend.filter;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.UserAccount;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Locale;

public record AuditSearch(String q, int page, int size, String sort) {
    private static final List<String> SORT_FIELDS = List.of("occurredAt", "eventType", "entityType");

    public AuditSearch {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page должен быть от 0 до 1000000, size — от 1 до 50");
        }
        if (q != null && q.trim().length() > 100) {
            throw new IllegalArgumentException("Поиск длиннее 100 символов");
        }
        String[] parts = sort == null ? new String[0] : sort.split(",", -1);
        if (parts.length != 2 || !SORT_FIELDS.contains(parts[0])
            || !(parts[1].equalsIgnoreCase("asc") || parts[1].equalsIgnoreCase("desc"))) {
            throw new IllegalArgumentException("Недопустимая сортировка аудита");
        }
    }

    public Sort safeSort() {
        String[] parts = sort.split(",", -1);
        return Sort.by(Sort.Direction.fromString(parts[1]), parts[0]).and(Sort.by("id"));
    }

    public Specification<AuditEvent> specification() {
        return (root, query, cb) -> {
            if (q == null || q.isBlank()) return cb.conjunction();
            String escaped = q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                .replace("%", "\\%").replace("_", "\\_");
            String pattern = "%" + escaped + "%";
            Join<AuditEvent, UserAccount> user = root.join("user", JoinType.LEFT);
            return cb.or(
                cb.like(cb.lower(root.get("eventType")), pattern, '\\'),
                cb.like(cb.lower(root.get("entityType")), pattern, '\\'),
                cb.like(cb.lower(root.get("description")), pattern, '\\'),
                cb.like(cb.lower(root.get("correlationId")), pattern, '\\'),
                cb.like(cb.lower(user.get("email")), pattern, '\\'));
        };
    }
}
