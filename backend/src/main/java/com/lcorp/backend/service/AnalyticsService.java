package com.lcorp.backend.service;

import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;
import com.lcorp.backend.dto.AnalyticsDtos;
import com.lcorp.backend.repository.TransportationRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.EnumMap;
import java.util.List;

@Service
public class AnalyticsService {
    private final TransportationRequestRepository requests;

    public AnalyticsService(TransportationRequestRepository requests) {
        this.requests = requests;
    }

    @Transactional(readOnly = true, isolation = Isolation.REPEATABLE_READ)
    public AnalyticsDtos.Summary summary() {
        long active = requests.countByArchivedFalse();
        long archived = requests.countByArchivedTrue();
        long fragile = requests.countByArchivedFalseAndFragileTrue();
        BigDecimal weightSum = requests.sumActiveWeightKg();
        BigDecimal averageWeightKg = active == 0 ? BigDecimal.ZERO.setScale(2)
            : weightSum.divide(BigDecimal.valueOf(active), 2, RoundingMode.HALF_UP);

        List<AnalyticsDtos.ClientCount> byClient = requests.countActiveByClient().stream()
            .map(row -> new AnalyticsDtos.ClientCount(((Number) row[0]).longValue(),
                (String) row[1], ((Number) row[2]).longValue())).toList();

        EnumMap<TransportationRequestStatus, Long> statusCounts =
            new EnumMap<>(TransportationRequestStatus.class);
        for (Object[] row : requests.countActiveByStatus()) {
            statusCounts.put((TransportationRequestStatus) row[0], ((Number) row[1]).longValue());
        }
        List<AnalyticsDtos.StatusCount> byStatus = java.util.Arrays.stream(TransportationRequestStatus.values())
            .map(status -> new AnalyticsDtos.StatusCount(status, statusCounts.getOrDefault(status, 0L))).toList();

        List<AnalyticsDtos.WeightCount> byWeightCategory = List.of(
            new AnalyticsDtos.WeightCount(WeightCategory.LIGHT,
                requests.countByArchivedFalseAndWeightKgLessThan(WeightCategory.MEDIUM_FROM_KG)),
            new AnalyticsDtos.WeightCount(WeightCategory.MEDIUM,
                requests.countByArchivedFalseAndWeightKgGreaterThanEqualAndWeightKgLessThan(
                    WeightCategory.MEDIUM_FROM_KG, WeightCategory.HEAVY_FROM_KG)),
            new AnalyticsDtos.WeightCount(WeightCategory.HEAVY,
                requests.countByArchivedFalseAndWeightKgGreaterThanEqual(WeightCategory.HEAVY_FROM_KG)));

        return new AnalyticsDtos.Summary(active + archived, active, archived, fragile, active - fragile,
            averageWeightKg, byClient, byStatus, byWeightCategory);
    }
}
