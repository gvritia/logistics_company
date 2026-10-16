package com.lcorp.javafx.controller;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.javafx.DesktopContext;
import com.lcorp.javafx.service.CsvExportService;
import com.lcorp.javafx.util.UiLabels;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;

public final class MainController {
    @FXML private TableView<TransportationRequestView> table;
    @FXML private TableColumn<TransportationRequestView, Long> idColumn;
    @FXML private TableColumn<TransportationRequestView, String> cargoColumn;
    @FXML private TableColumn<TransportationRequestView, String> clientColumn;
    @FXML private TableColumn<TransportationRequestView, String> driverColumn;
    @FXML private TableColumn<TransportationRequestView, String> routeColumn;
    @FXML private TableColumn<TransportationRequestView, String> plannedColumn;
    @FXML private TableColumn<TransportationRequestView, String> statusColumn;
    @FXML private TableColumn<TransportationRequestView, String> priceColumn;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<NamedId> clientFilter;
    @FXML private ComboBox<TransportationRequestSortField> sortField;
    @FXML private ComboBox<SortDirection> sortDirection;
    @FXML private Label resultLabel;
    @FXML private Button detailsButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button approveButton;
    @FXML private Button assignButton;
    @FXML private Button releaseButton;
    @FXML private Button startButton;
    @FXML private Button completeButton;
    @FXML private Button cancelButton;
    private final ObservableList<TransportationRequestView> rows = FXCollections.observableArrayList();
    private DesktopContext context;

    @FXML
    private void initialize() {
        idColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().id()));
        cargoColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().cargoDescription()));
        clientColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().clientName()));
        driverColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
            c.getValue().driverName() == null ? "—" : c.getValue().driverName()));
        routeColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
            c.getValue().pickupAddress() + " → " + c.getValue().deliveryAddress()));
        plannedColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
            c.getValue().plannedDeliveryAt().toString().replace('T', ' ')));
        statusColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(UiLabels.status(c.getValue().status())));
        priceColumn.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().price().toPlainString()));
        table.setItems(rows);
        table.getSelectionModel().selectedItemProperty().addListener((observable, previous, current) -> updateActions(current));
        updateActions(null);
        statusFilter.getItems().add("Все статусы");
        for (TransportationRequestStatus status : TransportationRequestStatus.values()) {
            statusFilter.getItems().add(UiLabels.status(status));
        }
        statusFilter.getSelectionModel().selectFirst();
        sortField.setConverter(new StringConverter<>() {
            @Override public String toString(TransportationRequestSortField value) {
                return value == null ? "" : UiLabels.sortField(value);
            }
            @Override public TransportationRequestSortField fromString(String value) { return null; }
        });
        sortField.getItems().setAll(TransportationRequestSortField.values());
        sortField.setValue(TransportationRequestSortField.PLANNED_DELIVERY_AT);
        sortDirection.setConverter(new StringConverter<>() {
            @Override public String toString(SortDirection value) {
                return value == null ? "" : UiLabels.direction(value);
            }
            @Override public SortDirection fromString(String value) { return null; }
        });
        sortDirection.getItems().setAll(SortDirection.values());
        sortDirection.setValue(SortDirection.ASC);
    }

    private void updateActions(TransportationRequestView row) {
        boolean selected = row != null;
        boolean draft = selected && row.status() == TransportationRequestStatus.CREATED && row.driverId() == null;
        boolean approved = selected && row.status() == TransportationRequestStatus.APPROVED;
        boolean assigned = selected && row.driverId() != null;
        detailsButton.setDisable(!selected);
        editButton.setDisable(!draft);
        deleteButton.setDisable(!draft);
        approveButton.setDisable(!draft);
        assignButton.setDisable(!approved || assigned);
        releaseButton.setDisable(!approved || !assigned);
        startButton.setDisable(!approved || !assigned);
        completeButton.setDisable(!selected || row.status() != TransportationRequestStatus.IN_TRANSIT || !assigned);
        cancelButton.setDisable(!selected || !(draft || approved));
    }

    public void setContext(DesktopContext context) {
        this.context = context;
        refreshClients();
        refresh();
    }

    private void refreshClients() {
        NamedId previous = clientFilter.getValue();
        clientFilter.getItems().clear();
        clientFilter.getItems().add(new NamedId(null, "Все клиенты"));
        for (Client client : context.clients().findAll()) {
            clientFilter.getItems().add(new NamedId(client.getId(), client.getFullName()));
        }
        clientFilter.getSelectionModel().select(
            clientFilter.getItems().stream().filter(c -> previous != null &&
                java.util.Objects.equals(c.id(), previous.id())).findFirst().orElse(clientFilter.getItems().getFirst()));
    }

    @FXML
    private void refresh() {
        if (context == null) return;
        try {
            TransportationRequestSearchCriteria criteria = new TransportationRequestSearchCriteria();
            criteria.setSearchText(searchField.getText());
            if (statusFilter.getValue() != null && !statusFilter.getValue().equals("Все статусы")) {
                for (TransportationRequestStatus status : TransportationRequestStatus.values()) {
                    if (UiLabels.status(status).equals(statusFilter.getValue())) {
                        criteria.setStatus(status);
                        break;
                    }
                }
            }
            if (clientFilter.getValue() != null) criteria.setClientId(clientFilter.getValue().id());
            criteria.setSortField(sortField.getValue());
            criteria.setSortDirection(sortDirection.getValue());
            rows.setAll(context.queries().search(criteria));
            resultLabel.setText("Найдено заявок: " + rows.size());
        } catch (RuntimeException e) {
            error("Не удалось загрузить заявки", e);
        }
    }

    @FXML
    private void resetFilters() {
        searchField.clear();
        statusFilter.getSelectionModel().selectFirst();
        clientFilter.getSelectionModel().selectFirst();
        sortField.setValue(TransportationRequestSortField.PLANNED_DELIVERY_AT);
        sortDirection.setValue(SortDirection.ASC);
        refresh();
    }

    @FXML
    private void createRequest() { openForm(null); }

    @FXML
    private void editRequest() {
        TransportationRequestView selected = selected();
        if (selected != null) openForm(selected.id());
    }

    @FXML
    private void showDetails() {
        TransportationRequestView row = selected();
        if (row == null) return;
        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Заявка №" + row.id());
        details.setHeaderText(row.cargoDescription());
        details.setContentText("Клиент: " + row.clientName()
            + "\nОператор: " + row.operatorName()
            + "\nДоставщик: " + (row.driverName() == null ? "не назначен" : row.driverName())
            + "\nВес, кг: " + row.weightKg().toPlainString()
            + "\nЦена: " + row.price().toPlainString()
            + "\nСоздана: " + row.createdAt().toString().replace('T', ' ')
            + "\nПлан доставки: " + row.plannedDeliveryAt().toString().replace('T', ' ')
            + "\nСтатус: " + row.status()
            + "\nОткуда: " + row.pickupAddress()
            + "\nКуда: " + row.deliveryAddress());
        details.showAndWait();
    }

    @FXML
    private void deleteRequest() {
        TransportationRequestView selected = selected();
        if (selected == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Удалить заявку №" + selected.id() + "?", ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Подтверждение удаления");
        confirmation.setHeaderText("Действие нельзя отменить");
        if (confirmation.showAndWait().orElse(ButtonType.NO) != ButtonType.YES) return;
        try {
            context.requests().deleteRequest(selected.id());
            refresh();
        } catch (RuntimeException e) {
            error("Не удалось удалить заявку", e);
        }
    }

    @FXML
    private void approveRequest() {
        TransportationRequestView row = selected();
        if (row != null) perform("Не удалось одобрить заявку", () -> context.requests().approve(row.id()));
    }

    @FXML
    private void assignDriver() {
        TransportationRequestView row = selected();
        if (row == null) return;
        try {
            var choices = context.drivers().findActive().stream()
                .map(driver -> new NamedId(driver.getId(), driver.getFullName())).toList();
            if (choices.isEmpty()) {
                new Alert(Alert.AlertType.INFORMATION, "Нет активных доставщиков.").showAndWait();
                return;
            }
            ChoiceDialog<NamedId> dialog = new ChoiceDialog<>(choices.getFirst(), choices);
            dialog.initOwner(table.getScene().getWindow());
            dialog.setTitle("Назначение доставщика");
            dialog.setHeaderText("Заявка №" + row.id());
            dialog.setContentText("Доставщик:");
            dialog.showAndWait().ifPresent(choice -> perform("Не удалось назначить доставщика",
                () -> context.requests().assignDriver(row.id(), choice.id())));
        } catch (RuntimeException e) {
            error("Не удалось загрузить доставщиков", e);
        }
    }

    @FXML
    private void releaseDriver() {
        TransportationRequestView row = selected();
        if (row == null) return;
        if (row.driverId() == null) {
            new Alert(Alert.AlertType.INFORMATION, "У заявки нет назначенного доставщика.").showAndWait();
            return;
        }
        perform("Не удалось снять доставщика",
            () -> context.requests().rejectAssignment(row.id(), row.driverId()));
    }

    @FXML
    private void startTransportation() {
        TransportationRequestView row = selected();
        if (row == null) return;
        if (row.driverId() == null) {
            new Alert(Alert.AlertType.INFORMATION, "Сначала назначьте доставщика.").showAndWait();
            return;
        }
        perform("Не удалось начать перевозку",
            () -> context.requests().startTransportation(row.id(), row.driverId()));
    }

    @FXML
    private void completeDelivery() {
        TransportationRequestView row = selected();
        if (row == null) return;
        if (row.driverId() == null) {
            new Alert(Alert.AlertType.INFORMATION, "У заявки нет доставщика.").showAndWait();
            return;
        }
        perform("Не удалось завершить доставку",
            () -> context.requests().completeDelivery(row.id(), row.driverId()));
    }

    @FXML
    private void cancelRequest() {
        TransportationRequestView row = selected();
        if (row == null) return;
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION,
            "Отменить заявку №" + row.id() + "?", ButtonType.YES, ButtonType.NO);
        confirmation.setTitle("Подтверждение отмены");
        if (confirmation.showAndWait().orElse(ButtonType.NO) == ButtonType.YES) {
            perform("Не удалось отменить заявку", () -> context.requests().cancel(row.id()));
        }
    }

    private void perform(String errorTitle, Runnable action) {
        try {
            action.run();
            refresh();
        } catch (RuntimeException e) {
            error(errorTitle, e);
        }
    }

    private TransportationRequestView selected() {
        TransportationRequestView selected = table.getSelectionModel().getSelectedItem();
        if (selected == null) {
            new Alert(Alert.AlertType.INFORMATION, "Сначала выберите заявку в таблице.").showAndWait();
        }
        return selected;
    }

    private void openForm(Long id) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/request-form.fxml"));
            Parent root = loader.load();
            RequestFormController controller = loader.getController();
            Stage dialog = new Stage();
            dialog.initOwner(table.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle(id == null ? "Новая заявка" : "Редактирование заявки №" + id);
            Scene scene = new Scene(root, 620, 650);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            dialog.setScene(scene);
            controller.configure(context, dialog, id);
            dialog.showAndWait();
            if (controller.isSaved()) refresh();
        } catch (Exception e) {
            error("Не удалось открыть форму", e);
        }
    }

    @FXML
    private void showStatistics() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/statistics.fxml"));
            Parent root = loader.load();
            StatisticsController controller = loader.getController();
            controller.setContext(context);
            Stage dialog = new Stage();
            dialog.initOwner(table.getScene().getWindow());
            dialog.initModality(Modality.WINDOW_MODAL);
            dialog.setTitle("Статистика заявок");
            Scene scene = new Scene(root, 560, 480);
            scene.getStylesheets().add(getClass().getResource("/css/app.css").toExternalForm());
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (Exception e) {
            error("Не удалось открыть статистику", e);
        }
    }

    @FXML
    private void exportCsv() {
        if (rows.isEmpty()) {
            new Alert(Alert.AlertType.INFORMATION, "Текущая выборка пуста.").showAndWait();
            return;
        }
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Сохранить текущую выборку");
        chooser.setInitialFileName("transportation_requests.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(table.getScene().getWindow());
        if (file == null) return;
        try {
            new CsvExportService().export(file.toPath(), rows);
            resultLabel.setText("Экспортировано заявок: " + rows.size());
        } catch (IOException e) {
            error("Не удалось сохранить CSV", e);
        }
    }

    private void error(String title, Exception cause) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Ошибка");
        alert.setHeaderText(title);
        alert.setContentText(cause.getMessage() == null ? "Проверьте подключение и введённые данные." : cause.getMessage());
        alert.showAndWait();
    }

    public record NamedId(Long id, String name) {
        @Override public String toString() { return name; }
    }
}
