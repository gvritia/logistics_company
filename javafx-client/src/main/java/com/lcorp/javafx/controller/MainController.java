package com.lcorp.javafx.controller;

import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.service.TransportationRequestQueryService;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

import java.math.BigDecimal;
import java.util.Objects;

public final class MainController {
    @FXML private TableView<TransportationRequestView> requestTable;
    @FXML private TableColumn<TransportationRequestView, Long> idColumn;
    @FXML private TableColumn<TransportationRequestView, String> cargoColumn;
    @FXML private TableColumn<TransportationRequestView, BigDecimal> weightColumn;
    @FXML private TableColumn<TransportationRequestView, TransportationRequestStatus> statusColumn;
    @FXML private TableColumn<TransportationRequestView, String> pickupColumn;
    @FXML private TableColumn<TransportationRequestView, String> deliveryColumn;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().id()));
        cargoColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().cargoDescription()));
        weightColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().weightKg()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().status()));
        pickupColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().pickupAddress()));
        deliveryColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().deliveryAddress()));
    }

    public void loadRequests(TransportationRequestQueryService service) {
        Objects.requireNonNull(service);
        try {
            var requests = service.search(new TransportationRequestSearchCriteria());
            requestTable.setItems(FXCollections.observableArrayList(requests));
            requestTable.setPlaceholder(new Label("Заявок пока нет"));
        } catch (DataAccessException exception) {
            showLoadError();
        }
    }

    public void showLoadError() {
        requestTable.setPlaceholder(new Label("Не удалось загрузить заявки"));
        new Alert(Alert.AlertType.ERROR,
            "Не удалось загрузить заявки. Проверьте подключение к PostgreSQL и настройки .env.")
            .show();
    }
}
