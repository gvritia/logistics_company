package com.lcorp.console.repository.jdbc;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.service.TransportationRequestQueryService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

// Ручная интеграционная проверка read-only JDBC-запросов на локальной БД из .env.
public final class JdbcTransportationRequestQueryRepositoryCheck {

    public static void main(String[] args) {
        var connectionFactory = new DatabaseConnectionFactory(DatabaseConfig.load());
        var repository = new JdbcTransportationRequestQueryRepository(connectionFactory);
        var service = new TransportationRequestQueryService(repository);

        List<TransportationRequestView> all = service.search(
            new TransportationRequestSearchCriteria()
        );
        require(all.size() >= 10, "Начальные данные должны содержать минимум 10 заявок");
        require(all.stream().allMatch(row -> row.clientName() != null
            && row.operatorName() != null), "JOIN должен вернуть имена клиента и оператора");
        require(all.stream().allMatch(row -> row.driverId() != null
            || row.driverName() == null), "У свободной заявки имя доставщика должно быть null");

        TransportationRequestSearchCriteria textSearch =
            new TransportationRequestSearchCriteria();
        textSearch.setSearchText("оборудование");
        List<TransportationRequestView> textResult = service.search(textSearch);
        require(textResult.size() >= 2, "Поиск должен найти оборудование в seed-данных");
        require(textResult.stream().allMatch(JdbcTransportationRequestQueryRepositoryCheck::
            containsEquipment), "Текстовый поиск вернул неподходящую строку");

        TransportationRequestSearchCriteria filtered =
            new TransportationRequestSearchCriteria();
        filtered.setStatus(TransportationRequestStatus.APPROVED);
        filtered.setMinPrice(new BigDecimal("50000"));
        filtered.setMaxPrice(new BigDecimal("150000"));
        filtered.setPlannedFrom(LocalDateTime.of(2026, 9, 1, 0, 0));
        filtered.setPlannedTo(LocalDateTime.of(2026, 9, 30, 23, 59));
        filtered.setSortField(TransportationRequestSortField.PRICE);
        filtered.setSortDirection(SortDirection.DESC);
        List<TransportationRequestView> filteredResult = service.search(filtered);
        require(!filteredResult.isEmpty(), "Комбинированный фильтр должен вернуть строки");
        require(filteredResult.stream().allMatch(row ->
            row.status() == TransportationRequestStatus.APPROVED
                && row.price().compareTo(new BigDecimal("50000")) >= 0
                && row.price().compareTo(new BigDecimal("150000")) <= 0
        ), "Фильтр статуса и цены");
        requirePriceDescending(filteredResult);

        TransportationRequestSearchCriteria byDriver =
            new TransportationRequestSearchCriteria();
        byDriver.setDriverId(1L);
        require(service.search(byDriver).stream()
            .allMatch(row -> Long.valueOf(1L).equals(row.driverId())), "Фильтр доставщика");

        var statistics = service.getStatisticsByStatus();
        require(statistics.size() == TransportationRequestStatus.values().length,
            "Seed-данные должны покрывать все статусы");
        long statisticsCount = statistics.stream()
            .mapToLong(row -> row.requestCount())
            .sum();
        require(statisticsCount == all.size(), "Количество в статистике и поиске");
        require(statistics.stream().allMatch(row -> row.totalPrice().signum() > 0
            && row.averagePrice().signum() > 0 && row.totalWeightKg().signum() > 0),
            "Агрегаты должны быть положительными");

        TransportationRequestSearchCriteria invalid =
            new TransportationRequestSearchCriteria();
        invalid.setMinPrice(BigDecimal.TEN);
        invalid.setMaxPrice(BigDecimal.ONE);
        expectIllegalArgument(() -> service.search(invalid));

        System.out.println(
            "PASS: JDBC search, filters, sorting, joins, statistics and validation"
        );
    }

    private static boolean containsEquipment(TransportationRequestView row) {
        String query = "оборудование";
        return row.cargoDescription().toLowerCase().contains(query)
            || row.pickupAddress().toLowerCase().contains(query)
            || row.deliveryAddress().toLowerCase().contains(query)
            || row.clientName().toLowerCase().contains(query);
    }

    private static void requirePriceDescending(List<TransportationRequestView> rows) {
        for (int index = 1; index < rows.size(); index++) {
            require(rows.get(index - 1).price().compareTo(rows.get(index).price()) >= 0,
                "Сортировка цены DESC");
        }
    }

    private static void expectIllegalArgument(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Ожидалась ошибка параметров фильтра");
        } catch (IllegalArgumentException expected) {
            // Ожидаемая ветка проверки.
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
