package com.lcorp.console.model.query;

import com.lcorp.console.model.TransportationRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

// Строка для таблицы, консоли и будущего XLSX: содержит и ID, и имена участников.
public record TransportationRequestView(
    Long id,
    Long clientId,
    String clientName,
    Long driverId,
    String driverName,
    Long operatorId,
    String operatorName,
    String cargoDescription,
    BigDecimal weightKg,
    BigDecimal price,
    LocalDateTime createdAt,
    LocalDateTime plannedDeliveryAt,
    TransportationRequestStatus status,
    String pickupAddress,
    String deliveryAddress
) {
}
