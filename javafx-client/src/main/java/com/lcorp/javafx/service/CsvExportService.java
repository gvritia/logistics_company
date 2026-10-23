package com.lcorp.javafx.service;

import com.lcorp.console.model.query.TransportationRequestView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public final class CsvExportService {
    // Записывает текущую выборку в UTF-8 CSV вместе с заголовком колонок.
    public void export(Path path, List<TransportationRequestView> rows) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            writer.write("ID,Клиент,Оператор,Доставщик,Груз,Вес кг,Цена,Создана,План доставки,Статус,Откуда,Куда");
            writer.newLine();
            for (TransportationRequestView row : rows) {
                List<String> fields = List.of(
                    String.valueOf(row.id()), row.clientName(), row.operatorName(),
                    row.driverName() == null ? "" : row.driverName(), row.cargoDescription(),
                    row.weightKg().toPlainString(), row.price().toPlainString(),
                    row.createdAt().toString(), row.plannedDeliveryAt().toString(),
                    row.status().name(), row.pickupAddress(), row.deliveryAddress());
                writer.write(String.join(",", fields.stream().map(CsvExportService::escape).toList()));
                writer.newLine();
            }
        }
    }

    // Кавычки внутри поля удваиваются, чтобы запятые и переносы не ломали CSV.
    private static String escape(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
}
