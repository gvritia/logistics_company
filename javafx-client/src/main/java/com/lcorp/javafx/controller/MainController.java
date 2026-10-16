package com.lcorp.javafx.controller;

import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.exception.InvalidRequestDataException;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.console.service.TransportationRequestService;
import com.lcorp.javafx.service.RequestCreationService;
import com.lcorp.javafx.service.RequestDeletionService;
import com.lcorp.javafx.service.RequestEditingService;
import com.lcorp.javafx.model.ReferenceOption;
import com.lcorp.javafx.model.RequestStatisticsSummary;
import com.lcorp.javafx.service.RequestStatisticsService;
import com.lcorp.javafx.export.RequestCsvExporter;
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
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.FlowPane;
import javafx.stage.FileChooser;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public final class MainController {
    @FXML private Button createButton;
    @FXML private Button editButton;
    @FXML private Button deleteButton;
    @FXML private Button detailsButton;
    @FXML private Button refreshButton;
    @FXML private TextField searchField;
    @FXML private HBox searchBox;
    @FXML private FlowPane filterBox;
    @FXML private ComboBox<TransportationRequestStatus> statusBox;
    @FXML private ComboBox<ReferenceOption> clientBox;
    @FXML private ComboBox<TransportationRequestSortField> sortFieldBox;
    @FXML private ComboBox<SortDirection> sortDirectionBox;
    @FXML private Button statisticsButton;
    @FXML private Button exportButton;
    @FXML private TableView<TransportationRequestView> requestTable;
    @FXML private TableColumn<TransportationRequestView, Long> idColumn;
    @FXML private TableColumn<TransportationRequestView, String> cargoColumn;
    @FXML private TableColumn<TransportationRequestView, BigDecimal> weightColumn;
    @FXML private TableColumn<TransportationRequestView, String> statusColumn;
    @FXML private TableColumn<TransportationRequestView, String> pickupColumn;
    @FXML private TableColumn<TransportationRequestView, String> deliveryColumn;
    private TransportationRequestQueryService queryService;
    private RequestCreationService creationService;
    private RequestEditingService editingService;
    private RequestDeletionService deletionService;
    private RequestStatisticsService statisticsService;
    private final RequestCsvExporter csvExporter = new RequestCsvExporter();
    // Храним применённые условия отдельно от полей формы: CRUD обновляет ту же выборку.
    private TransportationRequestSearchCriteria activeCriteria = new TransportationRequestSearchCriteria();

    @FXML
    private void initialize() {
        statusBox.setItems(FXCollections.observableArrayList(TransportationRequestStatus.values()));
        statusBox.setConverter(enumLabels(this::statusLabel));
        sortFieldBox.setItems(FXCollections.observableArrayList(TransportationRequestSortField.values()));
        sortFieldBox.setConverter(enumLabels(this::sortFieldLabel));
        sortFieldBox.setValue(TransportationRequestSortField.PLANNED_DELIVERY_AT);
        sortDirectionBox.setItems(FXCollections.observableArrayList(SortDirection.values()));
        sortDirectionBox.setConverter(enumLabels(direction -> direction == SortDirection.ASC
            ? "По возрастанию" : "По убыванию"));
        sortDirectionBox.setValue(SortDirection.ASC);
        // TableView ожидает ObservableValue, поэтому поля record оборачиваются для ячеек
        idColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().id()));
        cargoColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().cargoDescription()));
        weightColumn.setCellValueFactory(row -> new ReadOnlyObjectWrapper<>(row.getValue().weightKg()));
        statusColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(
            statusLabel(row.getValue().status())));
        pickupColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().pickupAddress()));
        deliveryColumn.setCellValueFactory(row -> new ReadOnlyStringWrapper(row.getValue().deliveryAddress()));
        requestTable.getSelectionModel().selectedItemProperty().addListener((observable, previous, selected) -> {
            boolean canModify = selected != null
                && TransportationRequestService.isEditableDraft(selected.status(), selected.driverId());
            editButton.setDisable(!canModify);
            deleteButton.setDisable(!canModify);
            detailsButton.setDisable(selected == null);
        });
    }

    public void configure(
        TransportationRequestQueryService service,
        RequestCreationService creationService,
        RequestEditingService editingService,
        RequestDeletionService deletionService
    ) {
        this.queryService = Objects.requireNonNull(service);
        this.creationService = Objects.requireNonNull(creationService);
        this.editingService = Objects.requireNonNull(editingService);
        this.deletionService = Objects.requireNonNull(deletionService);
        this.statisticsService = new RequestStatisticsService(service);
        createButton.setDisable(false);
        refreshButton.setDisable(false);
        searchBox.setDisable(false);
        statisticsButton.setDisable(false);
        try {
            clientBox.setItems(FXCollections.observableArrayList(creationService.findClients()));
            filterBox.setDisable(false);
        } catch (DataAccessException exception) {
            showLoadError();
            return;
        }
        loadRequests();
    }

    public void loadRequests() {
        try {
            var requests = queryService.search(activeCriteria);
            requestTable.setItems(FXCollections.observableArrayList(requests));
            exportButton.setDisable(requests.isEmpty());
            requestTable.setPlaceholder(new Label(noActiveFilters()
                ? "Заявок пока нет" : "По запросу ничего не найдено"));
        } catch (DataAccessException exception) {
            showLoadError();
        }
    }

    @FXML
    private void onSearch() {
        TransportationRequestSearchCriteria criteria = new TransportationRequestSearchCriteria();
        criteria.setSearchText(searchField.getText().trim());
        criteria.setStatus(statusBox.getValue());
        ReferenceOption client = clientBox.getValue();
        criteria.setClientId(client == null ? null : client.id());
        criteria.setSortField(sortFieldBox.getValue());
        criteria.setSortDirection(sortDirectionBox.getValue());
        activeCriteria = criteria;
        loadRequests();
    }

    @FXML
    private void onClearSearch() {
        searchField.clear();
        statusBox.setValue(null);
        clientBox.setValue(null);
        sortFieldBox.setValue(TransportationRequestSortField.PLANNED_DELIVERY_AT);
        sortDirectionBox.setValue(SortDirection.ASC);
        activeCriteria = new TransportationRequestSearchCriteria();
        loadRequests();
    }

    @FXML
    private void onRefresh() {
        loadRequests();
    }

    @FXML
    private void onShowDetails() {
        TransportationRequestView selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/request-details.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.initOwner(requestTable.getScene().getWindow());
        dialog.setTitle("Заявка № " + selected.id());
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm());
        dialog.getDialogPane().getButtonTypes().add(
            new ButtonType("Закрыть", ButtonBar.ButtonData.CANCEL_CLOSE));
        try {
            dialog.getDialogPane().setContent(loader.load());
            RequestDetailsController details = loader.getController();
            details.setRequest(selected);
            dialog.showAndWait();
        } catch (IOException exception) {
            showError("Ошибка просмотра", "Не удалось открыть подробности заявки.");
        }
    }

    private boolean noActiveFilters() {
        return (activeCriteria.getSearchText() == null || activeCriteria.getSearchText().isBlank())
            && activeCriteria.getStatus() == null && activeCriteria.getClientId() == null;
    }

    @FXML
    private void onShowStatistics() {
        try {
            RequestStatisticsSummary summary = statisticsService.summarize();
            // Агрегаты вычислены в SQL по всей базе, поэтому фильтры таблицы их не меняют
            String message = "Всего заявок: " + summary.requestCount()
                + "\nОбщая стоимость: " + summary.totalPrice().toPlainString()
                + "\nСредняя стоимость: " + summary.averagePrice().toPlainString()
                + "\nОбщий вес, кг: " + summary.totalWeightKg().toPlainString()
                + "\nДоставлено: " + summary.deliveredCount()
                + "\nВ пути: " + summary.inTransitCount();
            Alert statistics = new Alert(Alert.AlertType.INFORMATION, message);
            statistics.initOwner(requestTable.getScene().getWindow());
            statistics.setTitle("Статистика заявок");
            statistics.setHeaderText("По всем заявкам в базе");
            statistics.showAndWait();
        } catch (DataAccessException exception) {
            showError("Не удалось получить статистику", "Проверьте подключение к PostgreSQL.");
        }
    }

    @FXML
    private void onExportCsv() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Экспорт заявок в CSV");
        chooser.setInitialFileName("заявки.csv");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV (*.csv)", "*.csv"));
        File file = chooser.showSaveDialog(requestTable.getScene().getWindow());
        if (file == null) {
            return;
        }

        try {
            // Снимок строк сохраняет ровно ту выборку и порядок, которые видит пользователь.
            var visibleRequests = List.copyOf(requestTable.getItems());
            csvExporter.export(visibleRequests, file.toPath());
            Alert success = new Alert(Alert.AlertType.INFORMATION,
                "Сохранено заявок: " + visibleRequests.size() + "\nФайл: " + file.getAbsolutePath());
            success.initOwner(requestTable.getScene().getWindow());
            success.setTitle("Экспорт завершён");
            success.setHeaderText(null);
            success.showAndWait();
        } catch (IOException | SecurityException exception) {
            showError("Не удалось сохранить CSV", "Проверьте путь к файлу и право на запись.");
        }
    }

    private String statusLabel(TransportationRequestStatus status) {
        return switch (status) {
            case CREATED -> "Создана";
            case APPROVED -> "Подтверждена";
            case IN_TRANSIT -> "В пути";
            case DELIVERED -> "Доставлена";
            case CANCELLED -> "Отменена";
        };
    }

    private String sortFieldLabel(TransportationRequestSortField field) {
        return switch (field) {
            case ID -> "Номер";
            case CREATED_AT -> "Дата создания";
            case PLANNED_DELIVERY_AT -> "Дата доставки";
            case PRICE -> "Стоимость";
            case WEIGHT_KG -> "Вес";
        };
    }

    // ComboBox хранит enum для запроса, а пользователю показывает понятное название
    private <T> StringConverter<T> enumLabels(java.util.function.Function<T, String> label) {
        return new StringConverter<>() {
            @Override public String toString(T value) { return value == null ? "" : label.apply(value); }
            @Override public T fromString(String value) { return null; }
        };
    }

    @FXML
    private void onCreateRequest() {
        openRequestDialog(null);
    }

    @FXML
    private void onEditRequest() {
        TransportationRequestView selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            openRequestDialog(selected);
        }
    }

    @FXML
    private void onDeleteRequest() {
        TransportationRequestView selected = requestTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            return;
        }

        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.initOwner(requestTable.getScene().getWindow());
        confirmation.setTitle("Удаление заявки");
        confirmation.setHeaderText("Удалить заявку № " + selected.id() + "?");
        confirmation.setContentText("Груз: " + selected.cargoDescription());
        ButtonType deleteType = new ButtonType("Удалить", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelType = new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE);
        confirmation.getButtonTypes().setAll(deleteType, cancelType);

        // Данные удаляются только после явного подтверждения в диалоге
        if (confirmation.showAndWait().orElse(cancelType) != deleteType) {
            return;
        }
        try {
            deletionService.delete(selected);
            loadRequests();
        } catch (IllegalStateException exception) {
            showError("Заявка изменилась", exception.getMessage());
            loadRequests();
        } catch (DataAccessException exception) {
            showError("Не удалось удалить заявку", "Проверьте подключение к PostgreSQL.");
        }
    }

    // Одна FXML форма работает для создания и редактирования а existing == null означает новую заявку.
    private void openRequestDialog(TransportationRequestView existing) {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/request-form.fxml"));
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm());
        dialog.initOwner(requestTable.getScene().getWindow());
        dialog.setTitle(existing == null ? "Новая заявка" : "Редактирование заявки № " + existing.id());
        ButtonType saveType = new ButtonType("Сохранить", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(
            saveType,
            new ButtonType("Отмена", ButtonBar.ButtonData.CANCEL_CLOSE)
        );

        try {
            dialog.getDialogPane().setContent(loader.load());
            RequestFormController form = loader.getController();
            form.setOptions(creationService.findClients(), creationService.findOperators());
            if (existing != null) {
                form.setRequest(existing);
            }

            Button saveButton = (Button) dialog.getDialogPane().lookupButton(saveType);
            // consume() оставляет форму открытой, чтобы пользователь исправил ошибку
            saveButton.addEventFilter(ActionEvent.ACTION, event -> {
                try {
                    long savedId = existing == null
                        ? creationService.create(form.readRequest())
                        : editingService.update(existing, form.readEditedRequest(existing));
                    loadRequests();
                    requestTable.getItems().stream()
                        .filter(row -> row.id() == savedId)
                        .findFirst()
                        .ifPresent(row -> requestTable.getSelectionModel().select(row));
                } catch (InvalidRequestDataException exception) {
                    showError("Проверьте поля", readableValidationMessage(exception));
                    event.consume();
                } catch (IllegalArgumentException exception) {
                    showError("Проверьте поля", exception.getMessage());
                    event.consume();
                } catch (IllegalStateException exception) {
                    showError("Заявка изменилась", exception.getMessage());
                    loadRequests();
                    event.consume();
                } catch (DataAccessException exception) {
                    showError("Не удалось сохранить заявку",
                        "Проверьте подключение к PostgreSQL и выбранных участников.");
                    event.consume();
                }
            });
            dialog.showAndWait();
        } catch (IOException exception) {
            showError("Ошибка формы", "Не удалось открыть форму заявки.");
        } catch (DataAccessException exception) {
            showError("Ошибка загрузки", "Не удалось получить список клиентов и операторов.");
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message);
        alert.initOwner(requestTable.getScene().getWindow());
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm());
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
        requestTable.setItems(FXCollections.observableArrayList());
        exportButton.setDisable(true);
        requestTable.setPlaceholder(new Label("Не удалось загрузить заявки"));
        Alert alert = new Alert(Alert.AlertType.ERROR,
            "Не удалось загрузить заявки. Проверьте подключение к PostgreSQL и настройки .env.");
        alert.initOwner(requestTable.getScene().getWindow());
        alert.getDialogPane().getStylesheets().add(
            getClass().getResource("/styles/app.css").toExternalForm());
        alert.setTitle("Ошибка загрузки");
        alert.setHeaderText(null);
        alert.show();
    }
}
