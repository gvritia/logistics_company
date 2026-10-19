package com.lcorp.javafx.controller;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.Operator;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.javafx.DesktopContext;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeParseException;

public final class RequestFormController {
    @FXML private ComboBox<MainController.NamedId> clientBox;
    @FXML private ComboBox<MainController.NamedId> operatorBox;
    @FXML private TextArea cargoField;
    @FXML private TextField weightField;
    @FXML private TextField priceField;
    @FXML private TextField pickupField;
    @FXML private TextField deliveryField;
    @FXML private DatePicker plannedDate;
    @FXML private TextField plannedTime;
    @FXML private Label errorLabel;
    private DesktopContext context;
    private Stage stage;
    private TransportationRequest existing;
    private boolean saved;

    public void configure(DesktopContext context, Stage stage, Long requestId) {
        this.context = context;
        this.stage = stage;
        for (Client client : context.clients().findAll()) {
            clientBox.getItems().add(new MainController.NamedId(client.getId(), client.getFullName()));
        }
        for (Operator operator : context.operators().findAll()) {
            operatorBox.getItems().add(new MainController.NamedId(operator.getId(), operator.getFullName()));
        }
        if (requestId == null) {
            plannedDate.setValue(LocalDate.now().plusDays(1));
            plannedTime.setText("12:00");
        } else {
            existing = context.requests().getById(requestId);
            selectById(clientBox, existing.getClientId());
            selectById(operatorBox, existing.getOperatorId());
            cargoField.setText(existing.getCargoDescription());
            weightField.setText(existing.getWeightKg().toPlainString());
            priceField.setText(existing.getPrice().toPlainString());
            pickupField.setText(existing.getPickupAddress());
            deliveryField.setText(existing.getDeliveryAddress());
            plannedDate.setValue(existing.getPlannedDeliveryAt().toLocalDate());
            plannedTime.setText(existing.getPlannedDeliveryAt().toLocalTime().toString());
        }
    }

    private void selectById(ComboBox<MainController.NamedId> box, Long id) {
        box.getItems().stream().filter(item -> item.id().equals(id)).findFirst().ifPresent(box::setValue);
    }

    @FXML
    private void save() {
        try {
            MainController.NamedId client = clientBox.getValue();
            MainController.NamedId operator = operatorBox.getValue();
            if (client == null) throw new IllegalArgumentException("Выберите клиента.");
            if (operator == null) throw new IllegalArgumentException("Выберите оператора.");
            String cargo = text(cargoField.getText(), "Описание груза", 500);
            String pickup = text(pickupField.getText(), "Адрес отправления", 300);
            String delivery = text(deliveryField.getText(), "Адрес доставки", 300);
            BigDecimal weight = positiveDecimal(weightField.getText(), "Вес", 3, 12);
            BigDecimal price = positiveDecimal(priceField.getText(), "Цена", 2, 12);
            if (plannedDate.getValue() == null) throw new IllegalArgumentException("Укажите дату доставки.");
            LocalTime time;
            try {
                time = LocalTime.parse(plannedTime.getText().trim());
            } catch (DateTimeParseException e) {
                throw new IllegalArgumentException("Время доставки укажите в формате ЧЧ:ММ.");
            }
            LocalDateTime planned = LocalDateTime.of(plannedDate.getValue(), time);
            LocalDateTime created = existing == null ? LocalDateTime.now() : existing.getCreatedAt();
            TransportationRequest request = new TransportationRequest(
                existing == null ? null : existing.getId(), client.id(),
                existing == null ? null : existing.getDriverId(), operator.id(),
                cargo, weight, price, created, planned,
                existing == null ? TransportationRequestStatus.CREATED : existing.getStatus(),
                pickup, delivery);
            if (existing == null) {
                context.requests().createRequest(request);
            } else {
                context.requests().updateRequest(request);
            }
            saved = true;
            stage.close();
        } catch (RuntimeException e) {
            errorLabel.setText(e.getMessage() == null ? "Проверьте введённые данные." : e.getMessage());
        }
    }

    private String text(String raw, String label, int maxLength) {
        String value = raw == null ? "" : raw.trim();
        if (value.isEmpty()) throw new IllegalArgumentException(label + ": заполните поле.");
        if (value.length() > maxLength) {
            throw new IllegalArgumentException(label + ": максимум " + maxLength + " символов.");
        }
        return value;
    }

    private BigDecimal positiveDecimal(String raw, String label, int maxScale, int maxPrecision) {
        try {
            BigDecimal value = new BigDecimal(raw.trim().replace(',', '.'));
            if (value.signum() <= 0 || value.scale() < 0 || value.scale() > maxScale
                || value.precision() - value.scale() > maxPrecision - maxScale) {
                throw new NumberFormatException();
            }
            return value;
        } catch (NullPointerException | NumberFormatException e) {
            throw new IllegalArgumentException(label + ": положительное число, максимум "
                + maxPrecision + " цифр и " + maxScale + " после запятой.");
        }
    }

    @FXML
    private void cancel() { stage.close(); }

    public boolean isSaved() { return saved; }
}
