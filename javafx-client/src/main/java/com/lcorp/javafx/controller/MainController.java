package com.lcorp.javafx.controller;

import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.exception.InvalidRequestDataException;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.javafx.service.RequestCreationService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.io.IOException;
import java.util.Objects;

public final class MainController {
    @FXML private Button createButton;
    @FXML private TableView<TransportationRequestView> requestTable;
    @FXML private TableColumn<TransportationRequestView, Long> idColumn;
    @FXML private TableColumn<TransportationRequestView, String> cargoColumn;
    @FXML private TableColumn<TransportationRequestView, BigDecimal> weightColumn;
    @FXML private TableColumn<TransportationRequestView, TransportationRequestStatus> statusColumn;
    @FXML private TableColumn<TransportationRequestView, String> pickupColumn;
    @FXML private TableColumn<TransportationRequestView, String> deliveryColumn;
    private TransportationRequestQueryService queryService;
    private RequestCreationService creationService;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().id()));
        cargoColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().cargoDescription()));
        weightColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().weightKg()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().status()));
        pickupColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().pickupAddress()));
        deliveryColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().deliveryAddress()));
    }

    public void configure(
        TransportationRequestQueryService service,
        RequestCreationService creationService
    ) {
        this.queryService = Objects.requireNonNull(service);
        this.creationService = Objects.requireNonNull(creationService);
        createButton.setDisable(false);
        loadRequests();
    }

    public void loadRequests() {
        try {
            var requests = queryService.search(new TransportationRequestSearchCriteria());
            requestTable.setItems(FXCollections.observableArrayList(requests));
            requestTable.setPlaceholder(new Label("Заявок пока нет"));
        } catch (DataAccessException exception) {
            showLoadError();
        }
    }

    @FXML
    private void onCreateRequest() {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/request-form.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(requestTable.getScene().getWindow());
        dialog.setTitle("Новая заявка");
        dialog.getDialogPane().getButtonTypes().addAll(
            new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE),
            new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        try {
            dialog.getDialogPane().setContent(loader.load());
            RequestFormController form = loader.getController();
            form.setOptions(creationService.findClients(), creationService.findOperators());

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(
                dialog.getDialogPane().getButtonTypes().getFirst()
            );
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    long newId = creationService.create(form.readRequest());
                    loadRequests();
                    requestTable.getItems().stream()
                        .filter(row -> row.id() == newId)
                        .findFirst()
                        .ifPresent(row -> requestTable.getSelectionModel().select(row));
                } catch (InvalidRequestDataException exception) {
                    showError("Проверьте поля", readableValidationMessage(exception));
                    event.consume();
                } catch (IllegalArgumentException exception) {
                    showError("Проверьте поля", exception.getMessage());
                    event.consume();
                } catch (DataAccessException exception) {
                    showError("Не удалось сохранить заявку",
                        "Проверьте подключение к PostgreSQL и выбранных участников.");
                    event.consume();
                }
            });
            dialog.showAndWait();
        } catch (IOException exception) {
            showError("Ошибка формы", "Не удалось открыть форму новой заявки.");
        } catch (DataAccessException exception) {
            showError("Ошибка загрузки", "Не удалось получить список клиентов и операторов.");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    private String readableValidationMessage(InvalidRequestDataException exception) {
        String field = switch (exception.getFieldName()) {
            case "plannedDeliveryAt" -> "плановая дата доставки";
            case "deliveryAddress" -> "адрес доставки";
            case "pickupAddress" -> "адрес отправления";
            case "cargoDescription" -> "описание груза";
            case "weightKg" -> "вес";
            case "price" -> "стоимость";
            default -> exception.getFieldName();
        };
        return "Поле «" + field + "»: " + exception.getReason() + ".";
    }

    public void showLoadError() {
        requestTable.setPlaceholder(new Label("Не удалось загрузить заявки"));
        new Alert(Alert.AlertType.ERROR,
            "Не удалось загрузить заявки. Проверьте подключение к PostgreSQL и настройки .env.")
            .show();
    }
}
