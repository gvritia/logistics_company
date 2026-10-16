package com.lcorp.javafx.controller;

import com.lcorp.console.model.TransportationRequest;
import com.lcorp.javafx.model.ReferenceOption;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

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
