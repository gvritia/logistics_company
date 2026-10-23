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
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Objects;

public final class MainController {
    // @FXML связывает поля с fx:id, а методы с onAction в main.fxml.
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
    @FXML private ComboBox<WorkingMode> modeBox;
    @FXML private Label driverLabel;
    @FXML private ComboBox<DriverChoice> activeDriverBox;
    @FXML private ComboBox<DriverView> driverViewBox;
    @FXML private HBox operatorFilterBox;
    @FXML private ComboBox<String> statusFilter;
    @FXML private ComboBox<NamedId> clientFilter;
    @FXML private ComboBox<TransportationRequestSortField> sortField;
    @FXML private ComboBox<SortDirection> sortDirection;
    @FXML private Label resultLabel;
    @FXML private Button createButton;
    @FXML private Button detailsButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button approveButton;
    @FXML private Button assignButton;
    @FXML private Button releaseButton;
    @FXML private Button startButton;
    @FXML private Button completeButton;
    @FXML private Button cancelButton;
    @FXML private HBox operatorStatusActions;
    @FXML private HBox driverActions;
    @FXML private Button takeButton;
    @FXML private Button rejectButton;
    @FXML private Button driverStartButton;
    @FXML private Button driverCompleteButton;
    @FXML private Button statisticsButton;
    @FXML private Label operatorHint;
    @FXML private Label driverHint;
    private final ObservableList<TransportationRequestView> rows = FXCollections.observableArrayList();
    private DesktopContext context;

    // FXMLLoader вызывает метод после создания элементов из FXML: настраиваем таблицу и фильтры.
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
        modeBox.getItems().setAll(WorkingMode.values());
        modeBox.setValue(WorkingMode.OPERATOR);
        driverViewBox.getItems().setAll(DriverView.values());
        driverViewBox.setValue(DriverView.AVAILABLE);
        updateActions(null);
        statusFilter.getItems().add("Все статусы");
        for (TransportationRequestStatus status : TransportationRequestStatus.values()) {
            statusFilter.getItems().add(UiLabels.status(status));
        }
        statusFilter.getSelectionModel().selectFirst();
        // Конвертер показывает русские подписи, сохраняя значения enum для поиска.
        sortField.setConverter(new StringConverter<>() {
            @Override public String toString(TransportationRequestSortField value) {
                return value == null ? "" : UiLabels.sortField(value);
            }
            // Поле не редактируется вручную: обратное преобразование здесь не требуется.
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

    // Включает только действия, допустимые для статуса заявки и выбранного доставщика.
    private void updateActions(TransportationRequestView row) {
        boolean selected = row != null;
        boolean draft = selected && row.status() == TransportationRequestStatus.CREATED && row.driverId() == null;
        boolean approved = selected && row.status() == TransportationRequestStatus.APPROVED;
        boolean assigned = selected && row.driverId() != null;
        DriverChoice activeDriver = activeDriverBox.getValue();
        boolean mine = activeDriver != null && selected && Objects.equals(row.driverId(), activeDriver.id());
        detailsButton.setDisable(!selected);
        editButton.setDisable(!draft);
        deleteButton.setDisable(!draft);
        approveButton.setDisable(!draft);
        assignButton.setDisable(!approved || assigned);
        releaseButton.setDisable(!approved || !assigned);
        startButton.setDisable(!approved || !assigned);
        completeButton.setDisable(!selected || row.status() != TransportationRequestStatus.IN_TRANSIT || !assigned);
        cancelButton.setDisable(!selected || !(draft || approved));
        takeButton.setDisable(!approved || assigned || activeDriver == null || !activeDriver.active());
        rejectButton.setDisable(!approved || !mine);
        driverStartButton.setDisable(!approved || !mine);
        driverCompleteButton.setDisable(!selected || row.status() != TransportationRequestStatus.IN_TRANSIT || !mine);
    }

    // Получает сервисы приложения и загружает начальные данные для экрана.
    public void setContext(DesktopContext context) {
        this.context = context;
        refreshClients();
        refreshDrivers();
        refresh();
    }

    // Перезагружает список доставщиков, по возможности сохраняя прежний выбор.
    private void refreshDrivers() {
        Long previousId = activeDriverBox.getValue() == null ? null : activeDriverBox.getValue().id();
        List<DriverChoice> drivers = context.drivers().findAll().stream()
            .map(driver -> new DriverChoice(driver.getId(), driver.getFullName(), driver.isActive())).toList();
        activeDriverBox.getItems().setAll(drivers);
        activeDriverBox.setValue(drivers.stream().filter(driver -> Objects.equals(driver.id(), previousId))
            .findFirst().orElse(null));
    }

    // Для неактивного доставщика открывает историю его заявок вместо списка свободных.
    @FXML
    private void onDriverChanged() {
        DriverChoice driver = activeDriverBox.getValue();
        if (driver != null && !driver.active()) driverViewBox.setValue(DriverView.MINE);
        refresh();
    }

    // Переключает элементы экрана между учебными режимами оператора и доставщика.
    @FXML
    private void onModeChanged() {
        boolean driverMode = modeBox.getValue() == WorkingMode.DRIVER;
        setVisible(driverLabel, driverMode);
        setVisible(activeDriverBox, driverMode);
        setVisible(driverViewBox, driverMode);
        setVisible(driverActions, driverMode);
        setVisible(driverHint, driverMode);
        setVisible(operatorFilterBox, !driverMode);
        setVisible(operatorStatusActions, !driverMode);
        setVisible(operatorHint, !driverMode);
        setVisible(createButton, !driverMode);
        setVisible(editButton, !driverMode);
        setVisible(deleteButton, !driverMode);
        setVisible(statisticsButton, !driverMode);
        table.getSelectionModel().clearSelection();
        updateActions(null);
        if (context != null) {
            if (driverMode) refreshDrivers();
            refresh();
        }
    }

    // managed=false убирает скрытый элемент и из расчёта компоновки окна.
    private void setVisible(javafx.scene.Node node, boolean visible) {
        node.setVisible(visible);
        node.setManaged(visible);
    }

    // Обновляет фильтр клиентов, сохраняя выбранный ID после перезагрузки.
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

    // Собирает критерии поиска по режиму и показывает найденные заявки.
    @FXML
    private void refresh() {
        if (context == null) return;
        try {
            TransportationRequestSearchCriteria criteria = new TransportationRequestSearchCriteria();
            criteria.setSearchText(searchField.getText());
            boolean driverMode = modeBox.getValue() == WorkingMode.DRIVER;
            if (driverMode) {
                DriverChoice driver = activeDriverBox.getValue();
                if (driver == null) {
                    table.getSelectionModel().clearSelection();
                    rows.clear();
                    resultLabel.setText("Выберите доставщика");
                    driverHint.setText("Выберите доставщика, чтобы увидеть свободные или свои заявки.");
                    return;
                }
                driverHint.setText(driver.active()
                    ? "Свободную заявку можно взять; от своей можно отказаться до начала перевозки."
                    : "Доставщик неактивен: доступны его ранее назначенные заявки, новые брать нельзя.");
                if (driverViewBox.getValue() == DriverView.MINE) {
                    criteria.setDriverId(driver.id());
                } else {
                    criteria.setStatus(TransportationRequestStatus.APPROVED);
                }
            } else {
                if (statusFilter.getValue() != null && !statusFilter.getValue().equals("Все статусы")) {
                    for (TransportationRequestStatus status : TransportationRequestStatus.values()) {
                        if (UiLabels.status(status).equals(statusFilter.getValue())) {
                            criteria.setStatus(status);
                            break;
                        }
                    }
                }
                if (clientFilter.getValue() != null) criteria.setClientId(clientFilter.getValue().id());
            }
            criteria.setSortField(sortField.getValue());
            criteria.setSortDirection(sortDirection.getValue());
            List<TransportationRequestView> found = context.queries().search(criteria);
            // В запросе null означает «без фильтра». Свободные заявки отбираем после поиска.
            if (driverMode && driverViewBox.getValue() == DriverView.AVAILABLE) {
                found = found.stream().filter(row -> row.driverId() == null).toList();
            }
            table.getSelectionModel().clearSelection();
            rows.setAll(found);
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

    // Показывает все поля выбранной заявки с подписями для пользователя.
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
            + "\nСтатус: " + UiLabels.status(row.status())
            + "\nОткуда: " + row.pickupAddress()
            + "\nКуда: " + row.deliveryAddress());
        details.showAndWait();
    }

    // Удаляет черновик только после подтверждения; правило дополнительно проверяет сервис.
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

    // Предлагает оператору только активных доставщиков для назначения.
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

    // Выбранный активный доставщик самостоятельно берёт свободную заявку.
    @FXML
    private void takeRequest() {
        TransportationRequestView row = selected();
        DriverChoice driver = activeDriverBox.getValue();
        if (modeBox.getValue() != WorkingMode.DRIVER || row == null || driver == null || !driver.active()
            || driverViewBox.getValue() != DriverView.AVAILABLE
            || row.status() != TransportationRequestStatus.APPROVED || row.driverId() != null) return;
        performDriverAction("Не удалось взять заявку", row.id(), DriverView.MINE,
            () -> context.requests().takeRequest(row.id(), driver.id()));
    }

    // Освобождает свою заявку до начала перевозки и возвращает её в свободные.
    @FXML
    private void rejectRequest() {
        TransportationRequestView row = selected();
        DriverChoice driver = activeDriverBox.getValue();
        if (modeBox.getValue() != WorkingMode.DRIVER || row == null || driver == null
            || row.status() != TransportationRequestStatus.APPROVED
            || !Objects.equals(row.driverId(), driver.id())) return;
        performDriverAction("Не удалось отказаться от заявки", row.id(), DriverView.AVAILABLE,
            () -> context.requests().rejectAssignment(row.id(), driver.id()));
    }

    // Передаёт сервису ID выбранного доставщика и запускает перевозку.
    @FXML
    private void startTransportation() {
        TransportationRequestView row = selected();
        if (row == null) return;
        Long driverId = row.driverId();
        if (modeBox.getValue() == WorkingMode.DRIVER) {
            DriverChoice driver = activeDriverBox.getValue();
            if (driver == null || !Objects.equals(driver.id(), driverId)) return;
            driverId = driver.id();
        }
        if (driverId == null) {
            new Alert(Alert.AlertType.INFORMATION, "Сначала назначьте доставщика.").showAndWait();
            return;
        }
        Long actingDriverId = driverId;
        if (modeBox.getValue() == WorkingMode.DRIVER) {
            performDriverAction("Не удалось начать перевозку", row.id(), DriverView.MINE,
                () -> context.requests().startTransportation(row.id(), actingDriverId));
        } else {
            perform("Не удалось начать перевозку",
                () -> context.requests().startTransportation(row.id(), actingDriverId));
        }
    }

    // Завершает перевозку только для доставщика, назначенного на заявку.
    @FXML
    private void completeDelivery() {
        TransportationRequestView row = selected();
        if (row == null) return;
        Long driverId = row.driverId();
        if (modeBox.getValue() == WorkingMode.DRIVER) {
            DriverChoice driver = activeDriverBox.getValue();
            if (driver == null || !Objects.equals(driver.id(), driverId)) return;
            driverId = driver.id();
        }
        if (driverId == null) {
            new Alert(Alert.AlertType.INFORMATION, "У заявки нет доставщика.").showAndWait();
            return;
        }
        Long actingDriverId = driverId;
        if (modeBox.getValue() == WorkingMode.DRIVER) {
            performDriverAction("Не удалось завершить доставку", row.id(), DriverView.MINE,
                () -> context.requests().completeDelivery(row.id(), actingDriverId));
        } else {
            perform("Не удалось завершить доставку",
                () -> context.requests().completeDelivery(row.id(), actingDriverId));
        }
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

    // Выполняет действие сервиса, затем обновляет таблицу или показывает ошибку.
    private void perform(String errorTitle, Runnable action) {
        try {
            action.run();
            refresh();
        } catch (RuntimeException e) {
            error(errorTitle, e);
        }
    }

    // После смены статуса обновляем выборку и снова выделяем ту же заявку, если она видна.
    private void performDriverAction(String errorTitle, long requestId, DriverView nextView, Runnable action) {
        try {
            action.run();
            driverViewBox.setValue(nextView);
            refresh();
            rows.stream().filter(row -> row.id() == requestId).findFirst()
                .ifPresent(row -> table.getSelectionModel().select(row));
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

    // Открывает одну FXML-форму для создания (id=null) и редактирования заявки.
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

    // Передаёт текущую выборку CSV-сервису после выбора файла пользователем.
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

    // Отдельная модель пункта списка показывает и неактивных доставщиков.
    private record DriverChoice(Long id, String name, boolean active) {
        @Override public String toString() {
            return active ? name : name + " (неактивен)";
        }
    }

    private enum WorkingMode {
        OPERATOR("Оператор"), DRIVER("Доставщик");
        private final String label;
        WorkingMode(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }

    private enum DriverView {
        AVAILABLE("Свободные заявки"), MINE("Мои заявки");
        private final String label;
        DriverView(String label) { this.label = label; }
        @Override public String toString() { return label; }
    }
}
