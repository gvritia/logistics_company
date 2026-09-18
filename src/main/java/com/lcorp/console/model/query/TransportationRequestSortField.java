package com.lcorp.console.model.query;

// Разрешённые поля сортировки. Произвольное имя столбца из консоли в SQL не попадёт
public enum TransportationRequestSortField {
    ID,
    CREATED_AT,
    PLANNED_DELIVERY_AT,
    PRICE,
    WEIGHT_KG
}
