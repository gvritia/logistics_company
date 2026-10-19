package com.lcorp.javafx;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;

// Ручная интеграционная проверка полного сценария доставщика на временной заявке.
public final class DriverWorkflowCheck {
    public static void main(String[] args) throws SQLException {
        DesktopContext context = new DesktopContext();
        var drivers = context.drivers().findActive();
        if (drivers.size() < 2) throw new AssertionError("Для проверки нужны два активных доставщика");
        Long driverId = drivers.getFirst().getId();
        Long otherDriverId = drivers.get(1).getId();
        long initialCount = context.queries().search(new TransportationRequestSearchCriteria()).size();
        TransportationRequest request = new TransportationRequest(
            context.clients().findAll().getFirst().getId(),
            context.operators().findAll().getFirst().getId(),
            "Временная проверка сценария доставщика",
            new BigDecimal("10.000"), new BigDecimal("1000.00"),
            LocalDateTime.now().plusDays(2), "Тестовый адрес А", "Тестовый адрес Б");
        try {
            context.requests().createRequest(request);
            context.requests().approve(request.getId());
            TransportationRequestSearchCriteria available = new TransportationRequestSearchCriteria();
            available.setStatus(TransportationRequestStatus.APPROVED);
            if (context.queries().search(available).stream().noneMatch(row ->
                row.id().equals(request.getId()) && row.driverId() == null)) {
                throw new AssertionError("Одобренная заявка не появилась в свободных");
            }

            context.requests().takeRequest(request.getId(), driverId);
            TransportationRequestSearchCriteria mine = new TransportationRequestSearchCriteria();
            mine.setDriverId(driverId);
            if (context.queries().search(mine).stream().noneMatch(row -> row.id().equals(request.getId()))) {
                throw new AssertionError("Взятая заявка не появилась у доставщика");
            }
            expectFailure(() -> context.requests().startTransportation(request.getId(), otherDriverId));
            context.requests().rejectAssignment(request.getId(), driverId);
            if (context.requests().getById(request.getId()).getDriverId() != null) {
                throw new AssertionError("После отказа заявка осталась назначенной");
            }

            context.requests().takeRequest(request.getId(), driverId);
            context.requests().startTransportation(request.getId(), driverId);
            expectFailure(() -> context.requests().rejectAssignment(request.getId(), driverId));
            context.requests().completeDelivery(request.getId(), driverId);
            if (context.requests().getById(request.getId()).getStatus() != TransportationRequestStatus.DELIVERED) {
                throw new AssertionError("Заявка не завершилась статусом DELIVERED");
            }
        } finally {
            // Сервис разрешает удалять только CREATED, поэтому удаляем исключительно свою тестовую строку.
            if (request.getId() != null) {
                var database = new DatabaseConnectionFactory(DatabaseConfig.load());
                try (var connection = database.openConnection();
                     var statement = connection.prepareStatement("DELETE FROM transportation_requests WHERE id = ?")) {
                    statement.setLong(1, request.getId());
                    statement.executeUpdate();
                }
            }
        }
        long finalCount = context.queries().search(new TransportationRequestSearchCriteria()).size();
        if (initialCount != finalCount) throw new AssertionError("Проверка оставила данные в БД");
        System.out.println("Driver workflow through JDBC: OK");
    }

    private static void expectFailure(Runnable action) {
        try {
            action.run();
        } catch (RuntimeException expected) {
            return;
        }
        throw new AssertionError("Ожидался отказ в недопустимом действии");
    }
}
