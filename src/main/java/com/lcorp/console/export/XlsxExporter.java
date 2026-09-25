package com.lcorp.console.export;

import com.lcorp.console.exception.ExportException;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.console.model.query.TransportationRequestView;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.ArrayList;

public final class XlsxExporter {

    private static final String DATE_TIME_PATTERN = "DD.MM.YYYY HH:MM";
    private static final String MONEY_PATTERN = "#,##0.00";

    private static final List<String> REQUEST_HEADERS = List.of(
        "ID", "Клиент ID", "Клиент", "Доставщик ID", "Доставщик",
        "Оператор ID", "Оператор", "Груз", "Вес, кг", "Цена",
        "Создана", "Доставка до", "Статус", "Откуда", "Куда"
    );

    private static final List<String> STATISTICS_HEADERS = List.of(
        "Статус", "Заявок", "Сумма", "Средняя цена", "Вес, кг"
    );

    private CellStyle headerStyle;
    private CellStyle dateStyle;
    private CellStyle moneyStyle;
    private CellStyle weightStyle;

    public ExportResult export(
        Path targetFile,
        List<TransportationRequestView> requests
    ) {
        try (Workbook workbook = new XSSFWorkbook()) {
            createStyles(workbook);
            writeRequests(workbook.createSheet("Заявки"), requests);
            writeStatistics(workbook.createSheet("Статистика"), statisticsFor(requests));

            Path parent = targetFile.toAbsolutePath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            try (OutputStream output = Files.newOutputStream(targetFile)) {
                workbook.write(output);
            }

            return new ExportResult(targetFile.toAbsolutePath(), requests.size());
        } catch (IOException exception) {
            throw new ExportException(
                "Не удалось сохранить файл " + targetFile + ": " + exception.getMessage(),
                exception
            );
        }
    }

    private List<RequestStatusStatistics> statisticsFor(List<TransportationRequestView> requests) {
        List<RequestStatusStatistics> result = new ArrayList<>();
        for (TransportationRequestStatus status : TransportationRequestStatus.values()) {
            long count = 0;
            BigDecimal price = BigDecimal.ZERO;
            BigDecimal weight = BigDecimal.ZERO;
            for (TransportationRequestView request : requests) {
                if (request.status() != status) continue;
                count++;
                price = price.add(request.price());
                weight = weight.add(request.weightKg());
            }
            if (count > 0) result.add(new RequestStatusStatistics(status, count, price,
                price.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP), weight));
        }
        return result;
    }

    private void createStyles(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);

        headerStyle = workbook.createCellStyle();
        headerStyle.setFont(boldFont);

        CreationHelper helper = workbook.getCreationHelper();

        dateStyle = workbook.createCellStyle();
        dateStyle.setDataFormat(helper.createDataFormat().getFormat(DATE_TIME_PATTERN));

        moneyStyle = workbook.createCellStyle();
        moneyStyle.setDataFormat(helper.createDataFormat().getFormat(MONEY_PATTERN));
        weightStyle = workbook.createCellStyle();
        weightStyle.setDataFormat(helper.createDataFormat().getFormat("#,##0.000"));
    }

    private void writeRequests(Sheet sheet, List<TransportationRequestView> requests) {
        writeHeader(sheet, REQUEST_HEADERS);

        int rowIndex = 1;
        for (TransportationRequestView view : requests) {
            Row row = sheet.createRow(rowIndex++);
            int column = 0;

            putNumber(row, column++, view.id());
            putNumber(row, column++, view.clientId());

            putText(row, column++, view.clientName());
            putNumber(row, column++, view.driverId());

            putText(row, column++, view.driverName());

            putNumber(row, column++, view.operatorId());

            putText(row, column++, view.operatorName());

            putText(row, column++, view.cargoDescription());

            putWeight(row, column++, view.weightKg());

            putMoney(row, column++, view.price());

            putDateTime(row, column++, view.createdAt());
            putDateTime(row, column++, view.plannedDeliveryAt());

            putText(row, column++, view.status() == null ? null : view.status().name());

            putText(row, column++, view.pickupAddress());
            putText(row, column, view.deliveryAddress());
        }

        finishSheet(sheet, REQUEST_HEADERS.size());
    }

    private void writeStatistics(Sheet sheet, List<RequestStatusStatistics> statistics) {
        writeHeader(sheet, STATISTICS_HEADERS);

        int rowIndex = 1;
        for (RequestStatusStatistics item : statistics) {
            Row row = sheet.createRow(rowIndex++);
            putText(row, 0, item.status() == null ? null : item.status().name());
            putNumber(row, 1, item.requestCount());
            putMoney(row, 2, item.totalPrice());
            putMoney(row, 3, item.averagePrice());
            putWeight(row, 4, item.totalWeightKg());
        }

        finishSheet(sheet, STATISTICS_HEADERS.size());
    }

    private void writeHeader(Sheet sheet, List<String> titles) {
        Row header = sheet.createRow(0);
        for (int column = 0; column < titles.size(); column++) {
            Cell cell = header.createCell(column);
            cell.setCellValue(titles.get(column));
            cell.setCellStyle(headerStyle);
        }
    }

    private void finishSheet(Sheet sheet, int columnCount) {
        sheet.createFreezePane(0, 1);
        for (int column = 0; column < columnCount; column++) {
            sheet.autoSizeColumn(column);
        }
    }

    private void putText(Row row, int column, String value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        }
    }

    private void putNumber(Row row, int column, Long value) {
        if (value != null) {
            row.createCell(column).setCellValue(value);
        }
    }

    private void putNumber(Row row, int column, long value) {
        row.createCell(column).setCellValue(value);
    }




    private void putMoney(Row row, int column, BigDecimal value) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(moneyStyle);
    }

    private void putWeight(Row row, int column, BigDecimal value) {
        if (value == null) return;
        Cell cell = row.createCell(column);
        cell.setCellValue(value.doubleValue());
        cell.setCellStyle(weightStyle);
    }

    private void putDateTime(Row row, int column, LocalDateTime value) {
        if (value == null) {
            return;
        }
        Cell cell = row.createCell(column);
        cell.setCellValue(value);
        cell.setCellStyle(dateStyle);
    }
}
