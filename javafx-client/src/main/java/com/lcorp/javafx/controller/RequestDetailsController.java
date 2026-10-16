package com.lcorp.javafx.controller;

import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.TransportationRequestView;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import java.time.format.DateTimeFormatter;
import java.util.Objects;

public final class RequestDetailsController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    @FXML private Label titleLabel;
    @FXML private Label cargoLabel;
    @FXML private Label clientLabel;
    @FXML private Label operatorLabel;
    @FXML private Label driverLabel;
    @FXML private Label weightLabel;
    @FXML private Label priceLabel;
    @FXML private Label statusLabel;
    @FXML private Label pickupLabel;
    @FXML private Label deliveryLabel;
    @FXML private Label createdLabel;
    @FXML private Label plannedLabel;

    public void setRequest(TransportationRequestView request) {
        Objects.requireNonNull(request);
        titleLabel.setText("Заявка № " + request.id());
        cargoLabel.setText(request.cargoDescription());
        clientLabel.setText(request.clientName());
        operatorLabel.setText(request.operatorName());
        driverLabel.setText(request.driverName() == null ? "Не назначен" : request.driverName());
        weightLabel.setText(request.weightKg().toPlainString() + " кг");
        priceLabel.setText(request.price().toPlainString() + " ₽");
        statusLabel.setText(statusText(request.status()));
        pickupLabel.setText(request.pickupAddress());
        deliveryLabel.setText(request.deliveryAddress());
        createdLabel.setText(request.createdAt().format(DATE_FORMAT));
        plannedLabel.setText(request.plannedDeliveryAt().format(DATE_FORMAT));
    }

    private String statusText(TransportationRequestStatus status) {
        return switch (status) {
            case CREATED -> "Создана";
            case APPROVED -> "Подтверждена";
            case IN_TRANSIT -> "В пути";
            case DELIVERED -> "Доставлена";
            case CANCELLED -> "Отменена";
        };
    }
}
