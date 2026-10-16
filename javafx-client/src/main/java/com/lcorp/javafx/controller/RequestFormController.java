package com.lcorp.javafx.controller;

import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.javafx.model.ReferenceOption;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

public final class RequestFormController {
    @FXML private ComboBox<ReferenceOption> clientBox;
    @FXML private ComboBox<ReferenceOption> operatorBox;
    @FXML private TextField cargoField;
    @FXML private TextField weightField;
    @FXML private TextField priceField;
    @FXML private DatePicker deliveryDatePicker;
    @FXML private TextField pickupField;
    @FXML private TextField deliveryField;

    @FXML
    private void initialize() {
        deliveryDatePicker.setValue(LocalDate.now().plusDays(1));
    }

    public void setOptions(List<ReferenceOption> clients, List<ReferenceOption> operators) {
        clientBox.setItems(FXCollections.observableArrayList(clients));
        operatorBox.setItems(FXCollections.observableArrayList(operators));
    }

    // Заполняет общую форму данными выбранной заявки перед редактированием.
    public void setRequest(TransportationRequestView request) {
        Objects.requireNonNull(request);
        clientBox.getItems().stream()
            .filter(option -> option.id().equals(request.clientId()))
            .findFirst().ifPresent(clientBox::setValue);
        operatorBox.getItems().stream()
            .filter(option -> option.id().equals(request.operatorId()))
            .findFirst().ifPresent(operatorBox::setValue);
        cargoField.setText(request.cargoDescription());
        weightField.setText(request.weightKg().toPlainString());
        priceField.setText(request.price().toPlainString());
        deliveryDatePicker.setValue(request.plannedDeliveryAt().toLocalDate());
        pickupField.setText(request.pickupAddress());
        deliveryField.setText(request.deliveryAddress());
    }

    // Сохраняет служебные поля исходной заявки; меняет только значения из формы.
    public TransportationRequest readEditedRequest(TransportationRequestView existing) {
        Objects.requireNonNull(existing);
        TransportationRequest values = readRequest();
        // DatePicker хранит лишь дату: неизменённое время доставки берём из БД.
        var plannedDelivery = deliveryDatePicker.getValue()
            .equals(existing.plannedDeliveryAt().toLocalDate())
            ? existing.plannedDeliveryAt()
            : values.getPlannedDeliveryAt();

        return new TransportationRequest(
            existing.id(),
            values.getClientId(),
            existing.driverId(),
            values.getOperatorId(),
            values.getCargoDescription(),
            values.getWeightKg(),
            values.getPrice(),
            existing.createdAt(),
            plannedDelivery,
            existing.status(),
            values.getPickupAddress(),
            values.getDeliveryAddress()
        );
    }

    public TransportationRequest readRequest() {
        if (clientBox.getValue() == null) {
            throw new IllegalArgumentException("Выберите клиента.");
        }
        if (operatorBox.getValue() == null) {
            throw new IllegalArgumentException("Выберите оператора.");
        }
        if (deliveryDatePicker.getValue() == null) {
            throw new IllegalArgumentException("Укажите плановую дату доставки.");
        }

        String cargo = requiredText(cargoField, "описание груза", 500);
        BigDecimal weight = positiveNumber(weightField, "вес", 3, new BigDecimal("1000000000"));
        BigDecimal price = positiveNumber(priceField, "стоимость", 2, new BigDecimal("10000000000"));
        String pickup = requiredText(pickupField, "адрес отправления", 300);
        String delivery = requiredText(deliveryField, "адрес доставки", 300);

        return new TransportationRequest(
            clientBox.getValue().id(),
            operatorBox.getValue().id(),
            cargo,
            weight,
            price,
            deliveryDatePicker.getValue().atTime(23, 59),
            pickup,
            delivery
        );
    }

    private static String requiredText(TextField field, String label, int maxLength) {
        String value = field.getText().trim();
        if (value.isEmpty()) {
            throw new IllegalArgumentException("Заполните поле «" + label + "».");
        }
        if (value.length() > maxLength) {
            throw new IllegalArgumentException("Поле «" + label + "» должно быть не длиннее "
                + maxLength + " символов.");
        }
        return value;
    }

    private static BigDecimal positiveNumber(
        TextField field, String label, int maxScale, BigDecimal exclusiveUpperBound
    ) {
        BigDecimal value;
        try {
            value = new BigDecimal(field.getText().trim().replace(',', '.'));
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Введите число в поле «" + label + "».");
        }
        if (value.signum() <= 0 || value.compareTo(exclusiveUpperBound) >= 0
            || value.stripTrailingZeros().scale() > maxScale) {
            throw new IllegalArgumentException("Поле «" + label
                + "» должно быть положительным числом с точностью до " + maxScale + " знаков после запятой.");
        }
        return value;
    }
}
