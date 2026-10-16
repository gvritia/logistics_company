package com.lcorp.javafx.util;

import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSortField;

public final class UiLabels {
    private UiLabels() { }

    public static String status(TransportationRequestStatus value) {
        return switch (value) {
            case CREATED -> "Создана";
            case APPROVED -> "Одобрена";
            case IN_TRANSIT -> "В пути";
            case DELIVERED -> "Доставлена";
            case CANCELLED -> "Отменена";
        };
    }

    public static String sortField(TransportationRequestSortField value) {
        return switch (value) {
            case ID -> "Номер";
            case CREATED_AT -> "Дата создания";
            case PLANNED_DELIVERY_AT -> "План доставки";
            case PRICE -> "Цена";
            case WEIGHT_KG -> "Вес";
        };
    }

    public static String direction(SortDirection value) {
        return value == SortDirection.ASC ? "По возрастанию" : "По убыванию";
    }
}
