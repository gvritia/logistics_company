package com.lcorp.javafx.controller;

import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.javafx.DesktopContext;
import com.lcorp.javafx.util.UiLabels;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

public final class StatisticsController {
    // @FXML связывает подписи с fx:id в statistics.fxml.
    @FXML private Label countLabel;
    @FXML private Label deliveredLabel;
    @FXML private Label totalPriceLabel;
    @FXML private Label averagePriceLabel;
    @FXML private Label totalWeightLabel;
    @FXML private TextArea breakdownArea;

    // Собирает итоги по статусам и заполняет поля окна статистики.
    public void setContext(DesktopContext context) {
        List<RequestStatusStatistics> data = context.queries().getStatisticsByStatus();
        long count = data.stream().mapToLong(RequestStatusStatistics::requestCount).sum();
        long delivered = data.stream()
            .filter(s -> s.status() == TransportationRequestStatus.DELIVERED)
            .mapToLong(RequestStatusStatistics::requestCount).sum();
        BigDecimal totalPrice = data.stream().map(RequestStatusStatistics::totalPrice)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalWeight = data.stream().map(RequestStatusStatistics::totalWeightKg)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
        // При пустой базе избегаем деления на ноль; иначе округляем до копеек.
        BigDecimal averagePrice = count == 0 ? BigDecimal.ZERO
            : totalPrice.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_UP);
        countLabel.setText(String.valueOf(count));
        deliveredLabel.setText(String.valueOf(delivered));
        totalPriceLabel.setText(totalPrice.toPlainString());
        averagePriceLabel.setText(averagePrice.toPlainString());
        totalWeightLabel.setText(totalWeight.toPlainString());
        StringBuilder breakdown = new StringBuilder();
        for (RequestStatusStatistics row : data) {
            breakdown.append(UiLabels.status(row.status())).append(": ").append(row.requestCount()).append('\n');
        }
        breakdownArea.setText(breakdown.toString());
    }
}
