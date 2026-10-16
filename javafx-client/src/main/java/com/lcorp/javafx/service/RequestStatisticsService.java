package com.lcorp.javafx.service;

import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.javafx.model.RequestStatisticsSummary;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Objects;

public final class RequestStatisticsService {
    private final TransportationRequestQueryService queryService;

    public RequestStatisticsService(TransportationRequestQueryService queryService) {
        this.queryService = Objects.requireNonNull(queryService);
    }

    // SQL возвращает агрегаты по статусам а здесь складываем их в общие показатели
    public RequestStatisticsSummary summarize() {
        List<RequestStatusStatistics> rows = queryService.getStatisticsByStatus();
        long count = rows.stream().mapToLong(RequestStatusStatistics::requestCount).sum();
        BigDecimal totalPrice = rows.stream().map(RequestStatusStatistics::totalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = rows.stream().map(RequestStatusStatistics::totalWeightKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal averagePrice = count == 0 ? BigDecimal.ZERO
            : totalPrice.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);

        return new RequestStatisticsSummary(
            count,
            totalPrice,
            averagePrice,
            totalWeight,
            countForStatus(rows, TransportationRequestStatus.DELIVERED),
            countForStatus(rows, TransportationRequestStatus.IN_TRANSIT)
        );
    }

    private long countForStatus(List<RequestStatusStatistics> rows, TransportationRequestStatus status) {
        return rows.stream().filter(row -> row.status() == status)
            .mapToLong(RequestStatusStatistics::requestCount).sum();
    }
}
