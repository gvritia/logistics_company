package com.lcorp.javafx;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.Operator;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.javafx.service.CsvExportService;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

// Ручная интеграционная проверка
public final class DesktopJdbcCheck {
    public static void main(String[] args) {
        DesktopContext context = new DesktopContext();
        Client client = context.clients().findAll().getFirst();
        Operator operator = context.operators().findAll().getFirst();
        long initialCount = context.queries().search(new TransportationRequestSearchCriteria()).size();
        LocalDateTime created = LocalDateTime.now();
        TransportationRequest request = new TransportationRequest(
            client.getId(), operator.getId(), "Проверка JavaFX JDBC",
            new BigDecimal("12.500"), new BigDecimal("1500.00"),
            created.plusDays(2), "Проверочный адрес А", "Проверочный адрес Б");
        try {
            context.requests().createRequest(request);
            if (request.getId() == null || context.requests().getById(request.getId()) == null) {
                throw new AssertionError("Заявка не создана");
            }
            request.setCargoDescription("Изменённая \"проверка\", JavaFX JDBC");
            context.requests().updateRequest(request);
            if (!context.requests().getById(request.getId()).getCargoDescription()
                .equals("Изменённая \"проверка\", JavaFX JDBC")) {
                throw new AssertionError("Заявка не обновлена");
            }
            TransportationRequestSearchCriteria search = new TransportationRequestSearchCriteria();
            search.setSearchText("JavaFX JDBC");
            var found = context.queries().search(search);
            if (found.stream().noneMatch(row -> row.id().equals(request.getId()))) {
                throw new AssertionError("Заявка не найдена JDBC-поиском");
            }
            try {
                Path file = Files.createTempFile("kr2-export-check-", ".csv");
                try {
                    new CsvExportService().export(file, found);
                    String csv = Files.readString(file, StandardCharsets.UTF_8);
                    if (!csv.contains("\"Изменённая \"\"проверка\"\", JavaFX JDBC\"")) {
                        throw new AssertionError("CSV не экранирует кавычки и запятую");
                    }
                } finally {
                    Files.deleteIfExists(file);
                }
            } catch (IOException e) {
                throw new AssertionError("Не удалось проверить CSV", e);
            }
        } finally {
            if (request.getId() != null) context.requests().deleteRequest(request.getId());
        }
        long finalCount = context.queries().search(new TransportationRequestSearchCriteria()).size();
        if (initialCount != finalCount) throw new AssertionError("Проверка оставила данные в БД");
        System.out.println("Desktop JDBC CRUD and search: OK");
    }
}
