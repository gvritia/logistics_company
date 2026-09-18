package com.lcorp.console.util;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.Operator;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.console.model.query.TransportationRequestView;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConsoleWriter {

    private ConsoleWriter() {
    }

    public static void printTitle(String title) {
        System.out.println();
        System.out.println("=== " + title + " ===");
    }

    public static void printMenu(String title, List<String> items) {
        printTitle(title);
        for (int index = 0; index < items.size(); index++) {
            System.out.println("  " + (index + 1) + ") " + items.get(index));
        }
        System.out.println("  0) Назад");
    }

    public static void printSuccess(String message) {
        System.out.println("[OK] " + message);
    }

    public static void printError(String message) {
        System.out.println("[Ошибка] " + message);
    }

    public static void printInfo(String message) {
        System.out.println(message);
    }

    public static void printClients(List<Client> clients) {
        if (isEmpty(clients)) {
            return;
        }
        System.out.printf("%-5s %-30s %-16s %-28s%n", "ID", "ФИО", "Телефон", "Email");
        for (Client client : clients) {
            System.out.printf(
                "%-5d %-30s %-16s %-28s%n",
                client.getId(),
                cut(client.getFullName(), 30),
                cut(client.getPhone(), 16),
                cut(client.getEmail(), 28)
            );
        }
    }

    public static void printDrivers(List<Driver> drivers) {
        if (isEmpty(drivers)) {
            return;
        }
        System.out.printf("%-5s %-30s %-16s %-10s%n", "ID", "ФИО", "Телефон", "Активен");
        for (Driver driver : drivers) {
            System.out.printf(
                "%-5d %-30s %-16s %-10s%n",
                driver.getId(),
                cut(driver.getFullName(), 30),
                cut(driver.getPhone(), 16),
                driver.isActive() ? "да" : "нет"
            );
        }
    }

    public static void printOperators(List<Operator> operators) {
        if (isEmpty(operators)) {
            return;
        }
        System.out.printf("%-5s %-30s %-16s%n", "ID", "ФИО", "Телефон");
        for (Operator operator : operators) {
            System.out.printf(
                "%-5d %-30s %-16s%n",
                operator.getId(),
                cut(operator.getFullName(), 30),
                cut(operator.getPhone(), 16)
            );
        }
    }

    public static void printRequests(List<TransportationRequest> requests) {
        if (isEmpty(requests)) {
            return;
        }
        System.out.printf(
            "%-5s %-24s %-10s %-10s %-12s %-16s %-8s %-8s%n",
            "ID", "Груз", "Вес, кг", "Цена", "Статус", "Доставка до", "Клиент", "Водитель"
        );
        for (TransportationRequest request : requests) {
            System.out.printf(
                "%-5d %-24s %-10s %-10s %-12s %-16s %-8s %-8s%n",
                request.getId(),
                cut(request.getCargoDescription(), 24),
                request.getWeightKg(),
                request.getPrice(),
                request.getStatus(),
                formatDateTime(request.getPlannedDeliveryAt()),
                request.getClientId(),
                request.getDriverId() == null ? "-" : request.getDriverId()
            );
        }
    }

    public static void printRequestViews(List<TransportationRequestView> views) {
        if (isEmpty(views)) {
            return;
        }
        System.out.printf(
            "%-5s %-24s %-10s %-12s %-16s %-24s %-24s%n",
            "ID", "Груз", "Цена", "Статус", "Доставка до", "Клиент", "Водитель"
        );
        for (TransportationRequestView view : views) {
            System.out.printf(
                "%-5d %-24s %-10s %-12s %-16s %-24s %-24s%n",
                view.id(),
                cut(view.cargoDescription(), 24),
                view.price(),
                view.status(),
                formatDateTime(view.plannedDeliveryAt()),
                cut(view.clientName(), 24),
                view.driverName() == null ? "-" : cut(view.driverName(), 24)
            );
        }
    }

    public static void printStatistics(List<RequestStatusStatistics> statistics) {
        if (isEmpty(statistics)) {
            return;
        }
        System.out.printf(
            "%-12s %-10s %-14s %-14s %-14s%n",
            "Статус", "Заявок", "Сумма", "Средняя цена", "Вес, кг"
        );
        for (RequestStatusStatistics row : statistics) {
            System.out.printf(
                "%-12s %-10d %-14s %-14s %-14s%n",
                row.status(),
                row.requestCount(),
                money(row.totalPrice()),
                money(row.averagePrice()),
                money(row.totalWeightKg())
            );
        }
    }

    public static void printRequestDetails(TransportationRequest request) {
        printTitle("Заявка " + request.getId());
        System.out.println("Клиент:        " + request.getClientId());
        System.out.println("Оператор:      " + request.getOperatorId());
        System.out.println("Доставщик:     "
            + (request.getDriverId() == null ? "не назначен" : request.getDriverId()));
        System.out.println("Груз:          " + request.getCargoDescription());
        System.out.println("Вес, кг:       " + request.getWeightKg());
        System.out.println("Цена:          " + request.getPrice());
        System.out.println("Статус:        " + request.getStatus());
        System.out.println("Создана:       " + formatDateTime(request.getCreatedAt()));
        System.out.println("Доставка до:   " + formatDateTime(request.getPlannedDeliveryAt()));
        System.out.println("Откуда:        " + request.getPickupAddress());
        System.out.println("Куда:          " + request.getDeliveryAddress());
    }

    public static Set<Long> idsOf(List<TransportationRequest> requests) {
        return requests.stream().map(TransportationRequest::getId).collect(Collectors.toSet());
    }

    public static Set<Long> clientIdsOf(List<Client> clients) {
        return clients.stream().map(Client::getId).collect(Collectors.toSet());
    }

    public static Set<Long> driverIdsOf(List<Driver> drivers) {
        return drivers.stream().map(Driver::getId).collect(Collectors.toSet());
    }

    private static String money(BigDecimal value) {
        return value == null
            ? "-"
            : value.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean isEmpty(List<?> rows) {
        if (rows == null || rows.isEmpty()) {
            System.out.println("Список пуст");
            return true;
        }
        return false;
    }

    private static String formatDateTime(LocalDateTime value) {
        return value == null ? "-" : value.format(ConsoleReader.DATE_TIME_FORMAT);
    }

    private static String cut(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength - 1) + "…";
    }
}
