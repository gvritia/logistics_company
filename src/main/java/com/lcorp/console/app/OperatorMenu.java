package com.lcorp.console.app;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.service.ClientService;
import com.lcorp.console.service.DriverService;
import com.lcorp.console.service.OperatorService;
import com.lcorp.console.util.ConsoleReader;
import com.lcorp.console.util.ConsoleWriter;
import com.lcorp.console.util.ErrorHandler;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

public final class OperatorMenu implements Menu {

    private static final BigDecimal MAX_WEIGHT = new BigDecimal("100000");
    private static final BigDecimal MAX_PRICE = new BigDecimal("100000000");

    private static final List<String> ITEMS = List.of(
        "Заявки",
        "Клиенты",
        "Доставщики",
        "Поиск заявок",
        "Статистика по статусам"
    );

    private static final List<String> REQUEST_ITEMS = List.of(
        "Все заявки",
        "Свободные заявки",
        "Карточка заявки",
        "Создать заявку",
        "Удалить заявку",
        "Подтвердить заявку",
        "Назначить доставщика",
        "Переназначить доставщика",
        "Отменить заявку"
    );

    private static final List<String> CLIENT_ITEMS = List.of(
        "Все клиенты",
        "Создать клиента",
        "Изменить клиента",
        "Удалить клиента",
        "Заявки клиента"
    );

    private static final List<String> DRIVER_ITEMS = List.of(
        "Все доставщики",
        "Активные доставщики",
        "Создать доставщика",
        "Изменить доставщика",
        "Активировать",
        "Деактивировать",
        "Удалить доставщика"
    );

    private final ApplicationContext context;
    private final OperatorService operatorService;
    private final ClientService clientService;
    private final DriverService driverService;
    private final ConsoleReader reader;
    private final Session session;

    public OperatorMenu(ApplicationContext context, ConsoleReader reader, Session session) {
        this.context = Objects.requireNonNull(context);
        this.operatorService = context.operators();
        this.clientService = context.clients();
        this.driverService = context.drivers();
        this.reader = Objects.requireNonNull(reader);
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void show() {
        while (true) {
            ConsoleWriter.printMenu(title(), ITEMS);
            int choice = reader.readInt("Выбор", 0, ITEMS.size());
            if (choice == 0) {
                return;
            }
            switch (choice) {
                case 1 -> showRequests();
                case 2 -> showClients();
                case 3 -> showDrivers();
                case 4 -> runAndPause(this::searchRequests);
                case 5 -> runAndPause(this::showStatistics);
                default -> ConsoleWriter.printError("Неизвестный пункт меню");
            }
        }
    }

    private String title() {
        return "Оператор: " + session.getActorName();
    }

    private void runAndPause(Runnable action) {
        ErrorHandler.run(action);
        reader.pause();
    }

    private void showRequests() {
        while (true) {
            ConsoleWriter.printMenu(title() + " / Заявки", REQUEST_ITEMS);
            int choice = reader.readInt("Выбор", 0, REQUEST_ITEMS.size());
            if (choice == 0) {
                return;
            }
            runAndPause(() -> handleRequest(choice));
        }
    }

    private void handleRequest(int choice) {
        switch (choice) {
            case 1 -> {
                ConsoleWriter.printTitle("Все заявки");
                ConsoleWriter.printRequests(operatorService.findAllRequests());
            }
            case 2 -> {
                ConsoleWriter.printTitle("Свободные заявки");
                ConsoleWriter.printRequests(operatorService.findAvailableRequests());
            }
            case 3 -> showRequestCard();
            case 4 -> createRequest();
            case 5 -> deleteRequest();
            case 6 -> approveRequest();
            case 7 -> assignDriver();
            case 8 -> reassignDriver();
            case 9 -> cancelRequest();
            default -> ConsoleWriter.printError("Неизвестный пункт меню");
        }
    }

    private void showRequestCard() {
        Long requestId = chooseRequest("Все заявки", operatorService.findAllRequests());
        if (requestId == null) {
            return;
        }
        ConsoleWriter.printRequestDetails(context.requests().getById(requestId));
    }

    private void createRequest() {
        List<Client> clients = clientService.findAll();
        ConsoleWriter.printTitle("Клиенты");
        ConsoleWriter.printClients(clients);
        if (clients.isEmpty()) {
            return;
        }

        long clientId = reader.readIdFrom("ID клиента", ConsoleWriter.clientIdsOf(clients));
        String cargo = reader.readString("Описание груза", 3, 500);
        BigDecimal weight = reader.readBigDecimal("Вес, кг", BigDecimal.ZERO, MAX_WEIGHT);
        BigDecimal price = reader.readBigDecimal("Цена", BigDecimal.ZERO, MAX_PRICE);
        LocalDateTime plannedAt = reader.readDateTime("Плановая дата доставки");
        String pickup = reader.readString("Адрес отправления", 3, 300);
        String delivery = reader.readString("Адрес доставки", 3, 300);

        TransportationRequest created = operatorService.createRequest(new TransportationRequest(
            clientId,
            session.getActorId(),
            cargo,
            weight,
            price,
            plannedAt,
            pickup,
            delivery
        ));
        ConsoleWriter.printSuccess("Заявка создана, ID " + created.getId());
    }

    private void deleteRequest() {
        Long requestId = chooseRequest("Все заявки", operatorService.findAllRequests());
        if (requestId == null) {
            return;
        }
        if (!reader.confirm("Удалить заявку " + requestId + "?")) {
            return;
        }
        operatorService.deleteRequest(requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " удалена");
    }

    private void approveRequest() {
        Long requestId = chooseRequest("Все заявки", operatorService.findAllRequests());
        if (requestId == null) {
            return;
        }
        operatorService.approveRequest(requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " подтверждена");
    }

    private void assignDriver() {
        Long requestId = chooseRequest("Свободные заявки", operatorService.findAvailableRequests());
        if (requestId == null) {
            return;
        }
        Long driverId = chooseActiveDriver();
        if (driverId == null) {
            return;
        }
        operatorService.assignDriver(requestId, driverId);
        ConsoleWriter.printSuccess("Доставщик " + driverId + " назначен на заявку " + requestId);
    }

    private void reassignDriver() {
        Long requestId = chooseRequest("Все заявки", operatorService.findAllRequests());
        if (requestId == null) {
            return;
        }
        Long driverId = chooseActiveDriver();
        if (driverId == null) {
            return;
        }
        operatorService.reassignDriver(requestId, driverId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " передана доставщику " + driverId);
    }

    private void cancelRequest() {
        Long requestId = chooseRequest("Все заявки", operatorService.findAllRequests());
        if (requestId == null) {
            return;
        }
        if (!reader.confirm("Отменить заявку " + requestId + "?")) {
            return;
        }
        operatorService.cancelRequest(requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " отменена");
    }

    private Long chooseRequest(String title, List<TransportationRequest> requests) {
        ConsoleWriter.printTitle(title);
        ConsoleWriter.printRequests(requests);
        if (requests.isEmpty()) {
            return null;
        }
        return reader.readIdFrom("ID заявки", ConsoleWriter.idsOf(requests));
    }

    private Long chooseActiveDriver() {
        List<Driver> drivers = driverService.findActive();
        ConsoleWriter.printTitle("Активные доставщики");
        ConsoleWriter.printDrivers(drivers);
        if (drivers.isEmpty()) {
            return null;
        }
        return reader.readIdFrom("ID доставщика", ConsoleWriter.driverIdsOf(drivers));
    }

    private void showClients() {
        while (true) {
            ConsoleWriter.printMenu(title() + " / Клиенты", CLIENT_ITEMS);
            int choice = reader.readInt("Выбор", 0, CLIENT_ITEMS.size());
            if (choice == 0) {
                return;
            }
            runAndPause(() -> handleClient(choice));
        }
    }

    private void handleClient(int choice) {
        switch (choice) {
            case 1 -> {
                ConsoleWriter.printTitle("Все клиенты");
                ConsoleWriter.printClients(clientService.findAll());
            }
            case 2 -> {
                String fullName = reader.readString("ФИО", 3, 150);
                String phone = reader.readString("Телефон", 5, 32);
                String email = reader.readOptionalString("Email", 254);
                Client created = clientService.createClient(new Client(fullName, phone, email));
                ConsoleWriter.printSuccess("Клиент создан, ID " + created.getId());
            }
            case 3 -> {
                Long clientId = chooseClient();
                if (clientId == null) {
                    return;
                }
                String fullName = reader.readString("ФИО", 3, 150);
                String phone = reader.readString("Телефон", 5, 32);
                String email = reader.readOptionalString("Email", 254);
                clientService.updateClient(new Client(clientId, fullName, phone, email));
                ConsoleWriter.printSuccess("Клиент " + clientId + " обновлён");
            }
            case 4 -> {
                Long clientId = chooseClient();
                if (clientId == null) {
                    return;
                }
                if (!reader.confirm("Удалить клиента " + clientId + "?")) {
                    return;
                }
                clientService.deleteClient(clientId);
                ConsoleWriter.printSuccess("Клиент " + clientId + " удалён");
            }
            case 5 -> {
                Long clientId = chooseClient();
                if (clientId == null) {
                    return;
                }
                ConsoleWriter.printTitle("Заявки клиента " + clientId);
                ConsoleWriter.printRequests(clientService.findRequests(clientId));
            }
            default -> ConsoleWriter.printError("Неизвестный пункт меню");
        }
    }

    private Long chooseClient() {
        List<Client> clients = clientService.findAll();
        ConsoleWriter.printTitle("Клиенты");
        ConsoleWriter.printClients(clients);
        if (clients.isEmpty()) {
            return null;
        }
        return reader.readIdFrom("ID клиента", ConsoleWriter.clientIdsOf(clients));
    }

    private void showDrivers() {
        while (true) {
            ConsoleWriter.printMenu(title() + " / Доставщики", DRIVER_ITEMS);
            int choice = reader.readInt("Выбор", 0, DRIVER_ITEMS.size());
            if (choice == 0) {
                return;
            }
            runAndPause(() -> handleDriver(choice));
        }
    }

    private void handleDriver(int choice) {
        switch (choice) {
            case 1 -> {
                ConsoleWriter.printTitle("Все доставщики");
                ConsoleWriter.printDrivers(driverService.findAll());
            }
            case 2 -> {
                ConsoleWriter.printTitle("Активные доставщики");
                ConsoleWriter.printDrivers(driverService.findActive());
            }
            case 3 -> {
                String fullName = reader.readString("ФИО", 3, 150);
                String phone = reader.readString("Телефон", 5, 32);
                Driver created = driverService.createDriver(new Driver(fullName, phone));
                ConsoleWriter.printSuccess("Доставщик создан, ID " + created.getId());
            }
            case 4 -> {
                Long driverId = chooseDriver();
                if (driverId == null) {
                    return;
                }
                Driver existing = driverService.getById(driverId);
                String fullName = reader.readString("ФИО", 3, 150);
                String phone = reader.readString("Телефон", 5, 32);
                driverService.updateDriver(
                    new Driver(driverId, fullName, phone, existing.isActive())
                );
                ConsoleWriter.printSuccess("Доставщик " + driverId + " обновлён");
            }
            case 5 -> {
                Long driverId = chooseDriver();
                if (driverId == null) {
                    return;
                }
                driverService.activate(driverId);
                ConsoleWriter.printSuccess("Доставщик " + driverId + " активирован");
            }
            case 6 -> {
                Long driverId = chooseDriver();
                if (driverId == null) {
                    return;
                }
                driverService.deactivate(driverId);
                ConsoleWriter.printSuccess("Доставщик " + driverId + " деактивирован");
            }
            case 7 -> {
                Long driverId = chooseDriver();
                if (driverId == null) {
                    return;
                }
                if (!reader.confirm("Удалить доставщика " + driverId + "?")) {
                    return;
                }
                driverService.deleteDriver(driverId);
                ConsoleWriter.printSuccess("Доставщик " + driverId + " удалён");
            }
            default -> ConsoleWriter.printError("Неизвестный пункт меню");
        }
    }

    private Long chooseDriver() {
        List<Driver> drivers = driverService.findAll();
        ConsoleWriter.printTitle("Доставщики");
        ConsoleWriter.printDrivers(drivers);
        if (drivers.isEmpty()) {
            return null;
        }
        return reader.readIdFrom("ID доставщика", ConsoleWriter.driverIdsOf(drivers));
    }

    private void searchRequests() {
        TransportationRequestSearchCriteria criteria = new TransportationRequestSearchCriteria();
        criteria.setSearchText(reader.readOptionalString("Текст поиска", 200));
        criteria.setStatus(reader.readOptionalEnum("Статус", TransportationRequestStatus.class));
        criteria.setClientId(reader.readOptionalLong("ID клиента", 1));
        criteria.setDriverId(reader.readOptionalLong("ID доставщика", 1));
        criteria.setMinPrice(reader.readOptionalBigDecimal("Цена от", BigDecimal.ZERO));
        criteria.setMaxPrice(reader.readOptionalBigDecimal("Цена до", BigDecimal.ZERO));
        criteria.setPlannedFrom(reader.readOptionalDateTime("Доставка с"));
        criteria.setPlannedTo(reader.readOptionalDateTime("Доставка по"));
        criteria.setSortField(
            reader.readEnum("Поле сортировки", TransportationRequestSortField.class)
        );
        criteria.setSortDirection(
            reader.readEnum("Направление сортировки", SortDirection.class)
        );

        ConsoleWriter.printTitle("Результаты поиска");
        ConsoleWriter.printRequestViews(context.queries().search(criteria));
    }

    private void showStatistics() {
        ConsoleWriter.printTitle("Статистика по статусам");
        ConsoleWriter.printStatistics(context.queries().getStatisticsByStatus());
    }
}
