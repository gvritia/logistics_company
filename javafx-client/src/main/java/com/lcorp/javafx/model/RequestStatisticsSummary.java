package com.lcorp.javafx.model;

import java.math.BigDecimal;

public record RequestStatisticsSummary(
    long requestCount,
    BigDecimal totalPrice,
    BigDecimal averagePrice,
    BigDecimal totalWeightKg,
    long deliveredCount,
    long inTransitCount
) {
}
