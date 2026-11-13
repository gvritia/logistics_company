package com.lcorp.backend.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class WeightCategoryTest {
    @Test
    void usesExactWeightBoundaries() {
        assertEquals(WeightCategory.LIGHT, WeightCategory.fromWeightKg(new BigDecimal("999.999")));
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromWeightKg(new BigDecimal("1000.000")));
        assertEquals(WeightCategory.MEDIUM, WeightCategory.fromWeightKg(new BigDecimal("4999.999")));
        assertEquals(WeightCategory.HEAVY, WeightCategory.fromWeightKg(new BigDecimal("5000.000")));
    }
}
