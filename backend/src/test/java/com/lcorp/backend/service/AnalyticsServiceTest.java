package com.lcorp.backend.service;

import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;
import com.lcorp.backend.dto.AnalyticsDtos;
import com.lcorp.backend.repository.TransportationRequestRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalyticsServiceTest {
    @Test
    void emptyDatabaseHasZeroCountsAndAverage() {
        var requests = mock(TransportationRequestRepository.class);
        when(requests.countActiveByClient()).thenReturn(List.of());
        when(requests.countActiveByStatus()).thenReturn(List.of());

        AnalyticsDtos.Summary summary = new AnalyticsService(requests).summary();

        assertEquals(0, summary.total());
        assertEquals(0, summary.active());
        assertEquals(0, summary.archived());
        assertEquals(0, summary.fragile());
        assertEquals(0, summary.nonFragile());
        assertEquals(new BigDecimal("0.00"), summary.averageWeightKg());
        assertEquals(List.of(), summary.byClient());
        assertEquals(TransportationRequestStatus.values().length, summary.byStatus().size());
        assertEquals(0, summary.byStatus().stream().mapToLong(AnalyticsDtos.StatusCount::count).sum());
        assertEquals(List.of(WeightCategory.LIGHT, WeightCategory.MEDIUM, WeightCategory.HEAVY),
            summary.byWeightCategory().stream().map(AnalyticsDtos.WeightCount::category).toList());
    }

    @Test
    void archivedRequestsStayOutOfActiveGroupsAndAverageIsRounded() {
        var requests = mock(TransportationRequestRepository.class);
        when(requests.countByArchivedFalse()).thenReturn(4L);
        when(requests.countByArchivedTrue()).thenReturn(2L);
        when(requests.countByArchivedFalseAndFragileTrue()).thenReturn(2L);
        when(requests.sumActiveWeightKg()).thenReturn(new BigDecimal("10001.000"));
        when(requests.countActiveByClient()).thenReturn(List.<Object[]>of(
            new Object[]{7L, "Client A", 3L}, new Object[]{8L, "Client B", 1L}));
        when(requests.countActiveByStatus()).thenReturn(List.<Object[]>of(
            new Object[]{TransportationRequestStatus.CREATED, 3L},
            new Object[]{TransportationRequestStatus.DELIVERED, 1L}));
        when(requests.countByArchivedFalseAndWeightKgLessThan(WeightCategory.MEDIUM_FROM_KG)).thenReturn(1L);
        when(requests.countByArchivedFalseAndWeightKgGreaterThanEqualAndWeightKgLessThan(
            WeightCategory.MEDIUM_FROM_KG, WeightCategory.HEAVY_FROM_KG)).thenReturn(2L);
        when(requests.countByArchivedFalseAndWeightKgGreaterThanEqual(WeightCategory.HEAVY_FROM_KG)).thenReturn(1L);

        AnalyticsDtos.Summary summary = new AnalyticsService(requests).summary();

        assertEquals(6, summary.total());
        assertEquals(4, summary.active());
        assertEquals(2, summary.archived());
        assertEquals(2, summary.fragile());
        assertEquals(2, summary.nonFragile());
        assertEquals(new BigDecimal("2500.25"), summary.averageWeightKg());
        assertEquals(List.of(new AnalyticsDtos.ClientCount(7, "Client A", 3),
            new AnalyticsDtos.ClientCount(8, "Client B", 1)), summary.byClient());
        assertEquals(3, summary.byStatus().getFirst().count());
        assertEquals(1, summary.byStatus().stream()
            .filter(entry -> entry.status() == TransportationRequestStatus.DELIVERED)
            .findFirst().orElseThrow().count());
        assertEquals(4, summary.byWeightCategory().stream().mapToLong(AnalyticsDtos.WeightCount::count).sum());
    }
}
