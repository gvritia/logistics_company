package com.lcorp.backend.dto;

import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;

import java.math.BigDecimal;
import java.util.List;

public final class AnalyticsDtos{
    private AnalyticsDtos() {
    }

    public record ClientCount(long clientId, String clientName, long count) {
    }

    public record StatusCount(TransportationRequestStatus status, long count) {
    }

    public record WeightCount(WeightCategory category, long count) {
    }

    public record Summary(long total, long active, long archived, long fragile, long nonFragile,
                          BigDecimal averageWeightKg, List<ClientCount> byClient,
                          List<StatusCount> byStatus, List<WeightCount> byWeightCategory) {
    }
}
