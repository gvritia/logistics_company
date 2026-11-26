package com.lcorp.console.model.query;

import com.lcorp.console.model.TransportationRequestStatus;

import java.math.BigDecimal;


public record RequestStatusStatistics(
    TransportationRequestStatus status,
    long requestCount,
    BigDecimal totalPrice,
    BigDecimal averagePrice,
    BigDecimal totalWeightKg
) {
}
