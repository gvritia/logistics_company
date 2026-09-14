package com.lcorp.console.model;

/**
 * Этапы жизненного цикла заявки.
 *
 * <p>Отказ доставщика не является отдельным статусом: до начала перевозки
 * доставщик снимается с заявки, а заявка остаётся {@link #APPROVED} и снова
 * становится доступной.</p>
 */
public enum TransportationRequestStatus {
    CREATED,
    APPROVED,
    IN_TRANSIT,
    DELIVERED,
    CANCELLED
}
