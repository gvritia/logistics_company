package com.lcorp.console.export;

import com.lcorp.console.exception.ExportException;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.util.ConsoleReader;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class XlsxExporterCheck {
    public static void main(String[] args) throws Exception {
        Path directory = Files.createTempDirectory("logistics-export-check-");
        Path file = directory.resolve("requests.xlsx");
        try {
            XlsxExporter exporter = new XlsxExporter();
            ExportResult result = exporter.export(file, List.of(row(1L, "100.00", "1.123"),
                row(2L, "200.00", "2.234")));
            require(result.requestCount() == 2 && result.file().equals(file.toAbsolutePath()), "Результат экспорта");
            try (var input = Files.newInputStream(file); var workbook = new XSSFWorkbook(input)) {
                var requests = workbook.getSheet("Заявки");
                var statistics = workbook.getSheet("Статистика");
                require(requests.getLastRowNum() == 2, "Все переданные строки сохранены");
                require(statistics.getLastRowNum() == 1, "Итоги только по присутствующим статусам");
                var totals = statistics.getRow(1);
                require(totals.getCell(1).getNumericCellValue() == 2, "Число заявок");
                require(totals.getCell(2).getNumericCellValue() == 300, "Сумма цен");
                require(totals.getCell(3).getNumericCellValue() == 150, "Средняя цена");
                require(Math.abs(totals.getCell(4).getNumericCellValue() - 3.357) < 0.000001, "Сумма веса");
                require(requests.getRow(1).getCell(8).getCellStyle().getDataFormatString().equals("#,##0.000"),
                    "Три знака веса");
                require(DateUtil.isCellDateFormatted(requests.getRow(1).getCell(10)), "Тип даты в Excel");
                require(requests.getRow(1).getCell(7).getCellType() == CellType.STRING,
                    "Описание, похожее на формулу, остаётся текстом");
            }
            byte[] before = Files.readAllBytes(file);
            Path other = directory.resolve("other.xlsx");
            Path declined = new ConsoleReader(new Scanner(file + "\n\n" + directory.resolve("other") + "\n"))
                .readFilePath("Путь", "requests.xlsx", ".xlsx");
            require(declined.equals(other) && java.util.Arrays.equals(before, Files.readAllBytes(file)),
                "Отказ от перезаписи сохраняет файл и спрашивает другое имя");
            exporter.export(file, List.of(row(2L, "200.00", "2.234")));
            try (var input = Files.newInputStream(file); var workbook = new XSSFWorkbook(input)) {
                require(workbook.getSheet("Статистика").getRow(1).getCell(2).getNumericCellValue() == 200,
                    "Повторный экспорт подмножества пересчитывает итоги");
            }
            try {
                exporter.export(directory, List.of());
                throw new AssertionError("Каталог нельзя перезаписать файлом");
            } catch (ExportException expected) { }
        } finally {
            Files.deleteIfExists(file);
            Files.deleteIfExists(directory);
        }
        System.out.println("PASS: XLSX readback, filtered totals, precision, overwrite refusal, write error");
    }

    private static TransportationRequestView row(Long id, String price, String weight) {
        return new TransportationRequestView(id, 1L, "Клиент", null, null, 1L, "Оператор",
            "=1+1", new BigDecimal(weight), new BigDecimal(price), LocalDateTime.of(2026, 9, 1, 12, 0),
            LocalDateTime.of(2026, 10, 1, 12, 0), TransportationRequestStatus.CREATED, "Москва", "Тверь");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
