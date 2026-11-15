package com.lcorp.backend.filter;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RequestSearchTest {
    private RequestSearch search(String operation, BigDecimal value, int size, String sort) {
        return new RequestSearch(null, null, null, null, null, null,
            false, operation, value, 0, size, sort);
    }

    @Test
    void acceptsOnlyWhitelistedSortFieldsAndBoundedPages() {
        assertEquals("price: ASC,id: ASC", search(null, null, 50, "price,asc").safeSort().toString());
        assertThrows(IllegalArgumentException.class, () -> search(null, null, 51, "price,asc"));
        assertThrows(IllegalArgumentException.class, () -> search(null, null, 20, "client.email,asc"));
        assertThrows(IllegalArgumentException.class, () -> search(null, null, 20, "price,asc,createdAt"));
    }

    @Test
    void requiresSafeWeightComparison() {
        search("gte", new BigDecimal("1000.000"), 20, "createdAt,desc");
        assertThrows(IllegalArgumentException.class,
            () -> search("gte", null, 20, "createdAt,desc"));
        assertThrows(IllegalArgumentException.class,
            () -> search("ne", BigDecimal.ONE, 20, "createdAt,desc"));
        assertThrows(IllegalArgumentException.class,
            () -> search("eq", new BigDecimal("1.0001"), 20, "createdAt,desc"));
    }
}
