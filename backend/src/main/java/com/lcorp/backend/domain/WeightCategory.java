package com.lcorp.backend.domain;

import java.math.BigDecimal;

// Категория рассчитывается из веса и не хранится в отдельной колонке
public enum WeightCategory {
    LIGHT,
    MEDIUM,
    HEAVY;

    private static final BigDecimal MEDIUM_FROM_KG = new BigDecimal("1000");
    private static final BigDecimal HEAVY_FROM_KG = new BigDecimal("5000");

    public static WeightCategory fromWeightKg(BigDecimal weightKg) {
        if (weightKg == null) {
            return null;
        }
        if (weightKg.compareTo(MEDIUM_FROM_KG) < 0) {
            return LIGHT;
        }
        if (weightKg.compareTo(HEAVY_FROM_KG) < 0) {
            return MEDIUM;
        }
        return HEAVY;
    }
}
