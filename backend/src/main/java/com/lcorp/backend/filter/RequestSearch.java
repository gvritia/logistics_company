package com.lcorp.backend.filter;

import com.lcorp.backend.domain.TransportationRequest;
import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public record RequestSearch(String q, Long clientId, TransportationRequestStatus status,
                            Boolean fragile, WeightCategory weightCategory, String tag,
                            boolean archived, String weightOp, BigDecimal weightValue,
                            int page, int size, String sort) {
    private static final Map<String, String> SORT_FIELDS = Map.of(
        "createdAt", "createdAt", "plannedDeliveryAt", "plannedDeliveryAt",
        "price", "price", "weightKg", "weightKg", "requestCode", "requestCode");

    public RequestSearch {
        if (page < 0 || page > 1_000_000 || size < 1 || size > 50) {
            throw new IllegalArgumentException("page должен быть от 0 до 1000000, size — от 1 до 50");
        }
        if (q != null && q.trim().length() > 100) throw new IllegalArgumentException("Поиск длиннее 100 символов");
        if (clientId != null && clientId < 1) throw new IllegalArgumentException("Некорректный clientId");
        if (tag != null && (tag.isBlank() || tag.length() > 50)) {
            throw new IllegalArgumentException("Некорректный тег");
        }
        if ((weightOp == null) != (weightValue == null)) {
            throw new IllegalArgumentException("weightOp и weightValue задаются вместе");
        }
        if (weightValue != null && (weightValue.signum() <= 0 || weightValue.scale() > 3
            || weightValue.precision() > 12 || !List.of("eq", "lt", "lte", "gt", "gte").contains(weightOp))) {
            throw new IllegalArgumentException("Некорректный фильтр веса");
        }
        String[] sortParts = sort.split(",", -1);
        if (sortParts.length != 2 || !SORT_FIELDS.containsKey(sortParts[0])
            || !(sortParts[1].equalsIgnoreCase("asc") || sortParts[1].equalsIgnoreCase("desc"))) {
            throw new IllegalArgumentException("Недопустимая сортировка");
        }
    }

    public Sort safeSort() {
        String[] parts = sort.split(",", -1);
        Sort.Direction direction = Sort.Direction.fromString(parts[1]);
        return Sort.by(direction, SORT_FIELDS.get(parts[0])).and(Sort.by("id"));
    }

    public Specification<TransportationRequest> specification() {
        return (root, query, cb) -> {
            List<Predicate> filters = new ArrayList<>();
            filters.add(cb.equal(root.get("archived"), archived));
            if (clientId != null) filters.add(cb.equal(root.get("client").get("id"), clientId));
            if (status != null) filters.add(cb.equal(root.get("status"), status));
            if (fragile != null) filters.add(cb.equal(root.get("fragile"), fragile));
            if (weightCategory != null) {
                switch (weightCategory) {
                    case LIGHT -> filters.add(cb.lessThan(root.get("weightKg"), WeightCategory.MEDIUM_FROM_KG));
                    case MEDIUM ->
                        filters.add(cb.and(cb.greaterThanOrEqualTo(root.get("weightKg"), WeightCategory.MEDIUM_FROM_KG),
                            cb.lessThan(root.get("weightKg"), WeightCategory.HEAVY_FROM_KG)));
                    case HEAVY ->
                        filters.add(cb.greaterThanOrEqualTo(root.get("weightKg"), WeightCategory.HEAVY_FROM_KG));
                }
            }
            if (weightValue != null) {
                switch (weightOp) {
                    case "eq" -> filters.add(cb.equal(root.get("weightKg"), weightValue));
                    case "lt" -> filters.add(cb.lessThan(root.get("weightKg"), weightValue));
                    case "lte" -> filters.add(cb.lessThanOrEqualTo(root.get("weightKg"), weightValue));
                    case "gt" -> filters.add(cb.greaterThan(root.get("weightKg"), weightValue));
                    case "gte" -> filters.add(cb.greaterThanOrEqualTo(root.get("weightKg"), weightValue));
                    default -> throw new IllegalArgumentException("Некорректный weightOp");
                }
            }
            if (tag != null) {
                Subquery<Integer> subquery = query.subquery(Integer.class);
                Root<TransportationRequest> correlated = subquery.correlate(root);
                Join<TransportationRequest, String> tags = correlated.join("tags");
                subquery.select(cb.literal(1)).where(cb.equal(tags, tag.trim()));
                filters.add(cb.exists(subquery));
            }
            if (q != null && !q.isBlank()) {
                String escaped = q.trim().toLowerCase(Locale.ROOT).replace("\\", "\\\\")
                    .replace("%", "\\%").replace("_", "\\_");
                String pattern = "%" + escaped + "%";
                filters.add(cb.or(
                    cb.like(cb.lower(root.get("requestCode")), pattern, '\\'),
                    cb.like(cb.lower(root.get("cargoDescription")), pattern, '\\'),
                    cb.like(cb.lower(root.get("pickupAddress")), pattern, '\\'),
                    cb.like(cb.lower(root.get("deliveryAddress")), pattern, '\\'),
                    cb.like(cb.lower(root.join("client").get("fullName")), pattern, '\\')));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }
}
