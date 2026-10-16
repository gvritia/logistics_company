package com.lcorp.javafx.export;

import com.lcorp.console.model.query.TransportationRequestView;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

public final class RequestCsvExporter {
    // Записываем именно переданные строки, контроллер передаёт текущую выборку TableView
    public void export(List<TransportationRequestView> requests, Path file) throws IOException {
        Objects.requireNonNull(requests);
        Objects.requireNonNull(file);

        try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
            writer.write('\uFEFF'); // BOM помогает Excel на Windows распознать UTF-8.
            writeRow(writer, "Номер", "Груз", "Клиент", "Доставщик", "Оператор",
                "Вес, кг", "Стоимость", "Статус", "Адрес отправления",
                "Адрес доставки", "Создана", "Плановая доставка");
            for (TransportationRequestView request : requests) {
                writeRow(writer,
                    request.id(), request.cargoDescription(), request.clientName(),
                    request.driverName(), request.operatorName(), request.weightKg(),
                    request.price(), request.status(), request.pickupAddress(),
                    request.deliveryAddress(), request.createdAt(), request.plannedDeliveryAt());
            }
        }
    }

    private void writeRow(BufferedWriter writer, Object... values) throws IOException {
        for (int index = 0; index < values.length; index++) {
            if (index > 0) {
                writer.write(',');
            }
            String value = values[index] == null ? "" : values[index].toString();
            String trimmed = value.stripLeading();
            if (values[index] instanceof String && !trimmed.isEmpty()
                && "=+-@".indexOf(trimmed.charAt(0)) >= 0) {
                value = "'" + value;
            }
            writer.write('"');
            writer.write(value.replace("\"", "\"\""));
            writer.write('"');
        }
        writer.write("\r\n");
    }
}
