package com.lcorp.backend.domain;

// Значения совпадают с КР1 и уже сохранёнными строками PostgreSQL.
public enum TransportationRequestStatus {
    CREATED,
    APPROVED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
