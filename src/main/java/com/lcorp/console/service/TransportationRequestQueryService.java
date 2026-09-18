package com.lcorp.console.service;

import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.repository.TransportationRequestQueryRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

// Проверяет параметры чтения и не содержит SQL.
public class TransportationRequestQueryService {

    private final TransportationRequestQueryRepository queryRepository;

    public TransportationRequestQueryService(
        TransportationRequestQueryRepository queryRepository
    ) {
        this.queryRepository = Objects.requireNonNull(queryRepository);
    }

    public List<TransportationRequestView> search(
        TransportationRequestSearchCriteria criteria
    ) {
        validateCriteria(criteria);
        return queryRepository.search(criteria);
    }

    public List<RequestStatusStatistics> getStatisticsByStatus() {
        return queryRepository.getStatisticsByStatus();
    }

    private void validateCriteria(TransportationRequestSearchCriteria criteria) {
        Objects.requireNonNull(criteria, "Критерии поиска обязательны");
        requirePositiveId(criteria.getClientId(), "clientId");
        requirePositiveId(criteria.getDriverId(), "driverId");
        requireNonNegative(criteria.getMinPrice(), "minPrice");
        requireNonNegative(criteria.getMaxPrice(), "maxPrice");

        if (criteria.getMinPrice() != null && criteria.getMaxPrice() != null
            && criteria.getMinPrice().compareTo(criteria.getMaxPrice()) > 0) {
            throw new IllegalArgumentException(
                "Минимальная цена не может быть больше максимальной"
            );
        }
        if (criteria.getPlannedFrom() != null && criteria.getPlannedTo() != null
            && criteria.getPlannedFrom().isAfter(criteria.getPlannedTo())) {
            throw new IllegalArgumentException(
                "Начало диапазона доставки не может быть позже его конца"
            );
        }
    }

    private void requirePositiveId(Long value, String fieldName) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(fieldName + " должен быть положительным");
        }
    }

    private void requireNonNegative(BigDecimal value, String fieldName) {
        if (value != null && value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " не может быть отрицательным");
        }
    }
}
