package com.lcorp.javafx;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.repository.jdbc.JdbcTransportationRequestQueryRepository;
import com.lcorp.console.service.TransportationRequestQueryService;
import com.lcorp.javafx.export.RequestCsvExporter;
import com.lcorp.javafx.repository.JdbcRequestCreateDao;
import com.lcorp.javafx.repository.JdbcRequestDeleteDao;
import com.lcorp.javafx.repository.JdbcRequestUpdateDao;
import com.lcorp.javafx.service.RequestCreationService;
import com.lcorp.javafx.service.RequestDeletionService;
import com.lcorp.javafx.service.RequestEditingService;
import com.lcorp.javafx.service.RequestStatisticsService;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// Запускается вручную на Docker-БД: проверяет полный цикл и удаляет свою заявку.
public final class Kr2IntegrationCheck {
    public static void main(String[] args) throws Exception {
        var connections = new DatabaseConnectionFactory(DatabaseConfig.load());
        var query = new TransportationRequestQueryService(
            new JdbcTransportationRequestQueryRepository(connections));
        var createDao = new JdbcRequestCreateDao(connections);
        var deleteDao = new JdbcRequestDeleteDao(connections);
        var creation = new RequestCreationService(createDao);
        var editing = new RequestEditingService(new JdbcRequestUpdateDao(connections));
        var deletion = new RequestDeletionService(deleteDao);
        var statistics = new RequestStatisticsService(query);

        long baseline = statistics.summarize().requestCount();
        var clients = creation.findClients();
        var operators = creation.findOperators();
        require(!clients.isEmpty() && !operators.isEmpty(), "нет клиентов или операторов");

        String marker = "KR2_CHECK_" + UUID.randomUUID();
        long createdId = -1;
        Path csv = null;
        try {
            createdId = creation.create(new TransportationRequest(
                clients.getFirst().id(), operators.getFirst().id(), marker,
                new BigDecimal("12.500"), new BigDecimal("1500.00"),
                LocalDateTime.now().plusDays(3), "Москва, тестовый склад", "Тверь, тестовый склад"));

            TransportationRequestView created = findOne(query, marker);
            require(created.id() == createdId, "созданная заявка не найдена");
            require(created.clientName().equals(clients.getFirst().name()), "не загружен клиент");
            require(created.operatorName().equals(operators.getFirst().name()), "не загружен оператор");
            require(statistics.summarize().requestCount() == baseline + 1, "статистика не обновилась");

            var filter = new TransportationRequestSearchCriteria();
            filter.setClientId(created.clientId());
            filter.setStatus(TransportationRequestStatus.CREATED);
            long savedId = createdId;
            require(query.search(filter).stream().anyMatch(row -> row.id() == savedId),
                "фильтр клиента и статуса не нашёл заявку");

            var sort = new TransportationRequestSearchCriteria();
            sort.setSortField(TransportationRequestSortField.PRICE);
            sort.setSortDirection(SortDirection.DESC);
            List<TransportationRequestView> sorted = query.search(sort);
            for (int index = 1; index < sorted.size(); index++) {
                require(sorted.get(index - 1).price().compareTo(sorted.get(index).price()) >= 0,
                    "сортировка стоимости неверна");
            }

            String editedCargo = marker + " EDITED";
            editing.update(created, new TransportationRequest(
                created.id(), created.clientId(), created.driverId(), created.operatorId(),
                editedCargo, created.weightKg(), created.price(), created.createdAt(),
                created.plannedDeliveryAt(), created.status(), created.pickupAddress(),
                created.deliveryAddress()));
            TransportationRequestView edited = findOne(query, editedCargo);
            require(edited.id() == createdId, "изменение заявки не найдено");

            csv = Files.createTempFile("kr2-check-", ".csv");
            new RequestCsvExporter().export(List.of(edited), csv);
            String contents = Files.readString(csv, StandardCharsets.UTF_8);
            require(contents.startsWith("\uFEFF"), "CSV без UTF-8 BOM");
            require(contents.contains(editedCargo), "в CSV нет изменённой заявки");

            deletion.delete(edited);
            require(statistics.summarize().requestCount() == baseline, "счётчик после удаления неверен");
            require(search(query, marker).isEmpty(), "заявка осталась после удаления");
            System.out.println("KR2 integration check passed: CRUD, search, filters, sort, stats, CSV.");
        } finally {
            try {
                if (csv != null) {
                    Files.deleteIfExists(csv);
                }
            } finally {
                if (createdId > 0) {
                    // Если проверка оборвалась после INSERT, убираем только созданную ей заявку.
                    deleteDao.deleteById(createdId);
                }
            }
        }
    }

    private static TransportationRequestView findOne(
        TransportationRequestQueryService query, String text
    ) {
        List<TransportationRequestView> rows = search(query, text);
        require(rows.size() == 1, "ожидалась одна заявка: " + text);
        return rows.getFirst();
    }

    private static List<TransportationRequestView> search(
        TransportationRequestQueryService query, String text
    ) {
        var criteria = new TransportationRequestSearchCriteria();
        criteria.setSearchText(text);
        return query.search(criteria);
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
