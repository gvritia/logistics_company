package com.lcorp.console.app;

import com.lcorp.console.model.Client;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.export.ExportResult;
import com.lcorp.console.service.ClientService;
import com.lcorp.console.service.DriverService;
import com.lcorp.console.service.OperatorService;
import com.lcorp.console.util.ConsoleReader;
import com.lcorp.console.util.ConsoleWriter;
import com.lcorp.console.util.ErrorHandler;

import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public final class OperatorMenu implements Menu {

    private static final DateTimeFormatter FILE_NAME_TIME =
        DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm");

    private static final BigDecimal MAX_WEIGHT = new BigDecimal("100000");
    private static final BigDecimal MAX_PRICE = new BigDecimal("100000000");

    private static final int WEIGHT_SCALE = 3;
    private static final int PRICE_SCALE = 2;

    private static final List<String> ITEMS = List.of(
        "Заявки",
        "Клиенты",
        "Доставщики",
        "Поиск заявок",
        "Статистика по статусам",
        "Экспорт всех заявок в Excel"
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
            ConsoleWriter.printMenu(title(), ITEMS, "Сменить пользователя");
            int choice = reader.readMenuChoice(ITEMS.size());
            if (choice == 0) {
                return;
            }
            switch (choice) {
                case 1 -> showRequests();
                case 2 -> showClients();
                case 3 -> showDrivers();
                case 4 -> runAndPause(this::searchRequests);
                case 5 -> runAndPause(this::showStatistics);
                case 6 -> runAndPause(this::exportAllRequests);
                default -> ConsoleWriter.printError("Неизвестный пункт меню");
            }
        }
    }

    private String title() {
        return "Оператор: " + session.getActorName();
    }

    private void runAndPause(Runnable action) {
        if (ErrorHandler.run(action)) {
            reader.pause();
        }
    }

    private void showRequests() {
        while (true) {
            ConsoleWriter.printMenu(title() + " / Заявки", REQUEST_ITEMS);
            int choice = reader.readMenuChoice(REQUEST_ITEMS.size());
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
        Long requestId = chooseRequest("Карточка заявки", operatorService.findAllRequests());
        if (requestId == null) {
            return;
        }
        ConsoleWriter.printRequestDetails(context.requests().getById(requestId));
    }

    private void createRequest() {
        List<Client> clients = clientService.findAll();
        if (clients.isEmpty()) {
            ConsoleWriter.printTitle("Создание заявки");
            ConsoleWriter.printInfo("Сначала создайте клиента");
            return;
        }
        ConsoleWriter.printForm("Создание заявки");
        ConsoleWriter.printClients(clients);

        long clientId = reader.readIdFrom("ID клиента", ConsoleWriter.clientIdsOf(clients));
        String cargo = reader.readString("Описание груза", 3, 500);
        BigDecimal weight = reader.readPositiveDecimal("Вес, кг", MAX_WEIGHT, WEIGHT_SCALE);
        BigDecimal price = reader.readPositiveDecimal("Цена", MAX_PRICE, PRICE_SCALE);
        LocalDateTime plannedAt = reader.readFutureDateTime("Плановая дата доставки");
        String pickup = reader.readString("Адрес отправления", 3, 300);
        String delivery = readDeliveryAddress(pickup);

        while (!plannedAt.isAfter(LocalDateTime.now())) {
            ConsoleWriter.printError("Указанная дата доставки уже прошла");
            plannedAt = reader.readFutureDateTime("Плановая дата доставки");
        }

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

    private String readDeliveryAddress(String pickup) {
        while (true) {
            String delivery = reader.readString("Адрес доставки", 3, 300);
            if (!delivery.equalsIgnoreCase(pickup)) {
                return delivery;
            }
            ConsoleWriter.printError("Адрес доставки должен отличаться от адреса отправления");
        }
    }

    private void deleteRequest() {
        Long requestId = chooseRequest("Удаление заявки: новые",
            requestsWithStatus(TransportationRequestStatus.CREATED));
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
        Long requestId = chooseRequest("Подтверждение заявки: новые",
            requestsWithStatus(TransportationRequestStatus.CREATED));
        if (requestId == null) {
            return;
        }
        operatorService.approveRequest(requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " подтверждена");
    }

    private void assignDriver() {
        Long requestId = chooseRequest("Назначение доставщика: подтверждённые без доставщика",
            operatorService.findAvailableRequests());
        if (requestId == null) {
            return;
        }
        Long driverId = chooseActiveDriver(null);
        if (driverId == null) {
            return;
        }
        operatorService.assignDriver(requestId, driverId);
        ConsoleWriter.printSuccess("Доставщик " + driverId + " назначен на заявку " + requestId);
    }

    private void reassignDriver() {
        List<TransportationRequest> assigned = requestsWithStatus(TransportationRequestStatus.APPROVED)
            .stream().filter(request -> request.getDriverId() != null).toList();
        Long requestId = chooseRequest("Переназначение: назначенные до начала перевозки", assigned);
        if (requestId == null) {
            return;
        }
        Long currentDriverId = context.requests().getById(requestId).getDriverId();
        Long driverId = chooseActiveDriver(currentDriverId);
        if (driverId == null) {
            return;
        }
        operatorService.reassignDriver(requestId, driverId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " передана доставщику " + driverId);
    }

    private void cancelRequest() {
        Long requestId = chooseRequest("Отмена заявки: новые и подтверждённые", requestsWithStatus(
            TransportationRequestStatus.CREATED, TransportationRequestStatus.APPROVED));
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
        if (requests.isEmpty()) {
            ConsoleWriter.printTitle(title);
            ConsoleWriter.printInfo("Подходящих заявок нет");
            return null;
        }
        ConsoleWriter.printForm(title);
        ConsoleWriter.printRequests(requests);
        return reader.readIdFrom("ID заявки", ConsoleWriter.idsOf(requests));
    }

    private List<TransportationRequest> requestsWithStatus(TransportationRequestStatus... statuses) {
        List<TransportationRequestStatus> allowed = List.of(statuses);
        return operatorService.findAllRequests().stream()
            .filter(request -> allowed.contains(request.getStatus())).toList();
    }

    private Long chooseActiveDriver(Long excludedDriverId) {
        List<Driver> drivers = driverService.findActive().stream()
            .filter(driver -> !driver.getId().equals(excludedDriverId)).toList();
        return chooseDriver("Выбор доставщика: активные", drivers);
    }

    private void showClients() {
        while (true) {
            ConsoleWriter.printMenu(title() + " / Клиенты", CLIENT_ITEMS);
            int choice = reader.readMenuChoice(CLIENT_ITEMS.size());
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
            case 2 -> createClient();
            case 3 -> editClient();
            case 4 -> {
                Set<Long> withRequests = operatorService.findAllRequests().stream()
                    .map(TransportationRequest::getClientId).collect(Collectors.toSet());
                Long clientId = chooseClient("Удаление клиента: без заявок",
                    client -> !withRequests.contains(client.getId()));
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
                Long clientId = chooseClient("Заявки клиента", client -> true);
                if (clientId == null) {
                    return;
                }
                ConsoleWriter.printTitle("Заявки клиента " + clientId);
                ConsoleWriter.printRequests(clientService.findRequests(clientId));
            }
            default -> ConsoleWriter.printError("Неизвестный пункт меню");
        }
    }

    private void createClient() {
        ConsoleWriter.printForm("Новый клиент");
        String fullName = reader.readString("ФИО", 3, 150);
        String phone = reader.readOptionalString("Телефон", 32);
        String email = readEmail();
        while (phone == null && email == null) {
            ConsoleWriter.printError("Укажите хотя бы один контакт: телефон или email");
            phone = reader.readOptionalString("Телефон", 32);
            email = readEmail();
        }
        Client created = clientService.createClient(new Client(fullName, phone, email));
        ConsoleWriter.printSuccess("Клиент создан, ID " + created.getId());
    }

    private void editClient() {
        Long clientId = chooseClient("Изменение клиента", client -> true);
        if (clientId == null) {
            return;
        }
        Client existing = clientService.getById(clientId);
        String fullName = reader.readEditedString("ФИО", existing.getFullName(), 3, 150);
        String phone = reader.readEditedOptionalString("Телефон", existing.getPhone(), 32);
        String email = readEditedEmail(existing.getEmail());
        while (phone == null && email == null) {
            ConsoleWriter.printError("Укажите хотя бы один контакт: телефон или email");
            phone = reader.readEditedOptionalString("Телефон", null, 32);
            email = readEditedEmail(null);
        }

        if (fullName.equals(existing.getFullName())
            && Objects.equals(phone, existing.getPhone())
            && Objects.equals(email, existing.getEmail())) {
            ConsoleWriter.printInfo("Изменений нет");
            return;
        }
        clientService.updateClient(new Client(clientId, fullName, phone, email));
        ConsoleWriter.printSuccess("Клиент " + clientId + " обновлён");
    }

    private Long chooseClient(String title, Predicate<Client> suitable) {
        List<Client> clients = clientService.findAll().stream().filter(suitable).toList();
        if (clients.isEmpty()) {
            ConsoleWriter.printTitle(title);
            ConsoleWriter.printInfo("Подходящих клиентов нет");
            return null;
        }
        ConsoleWriter.printForm(title);
        ConsoleWriter.printClients(clients);
        return reader.readIdFrom("ID клиента", ConsoleWriter.clientIdsOf(clients));
    }

    private String readEmail() {
        while (true) {
            String email = reader.readOptionalString("Email", 254);
            if (email == null || isEmailValid(email)) {
                return email;
            }
            ConsoleWriter.printError("Введите email, например name@example.com");
        }
    }

    private String readEditedEmail(String current) {
        while (true) {
            String email = reader.readEditedOptionalString("Email", current, 254);
            if (email == null || email.equals(current) || isEmailValid(email)) {
                return email;
            }
            ConsoleWriter.printError("Введите email, например name@example.com");
        }
    }

    private static boolean isEmailValid(String email) {
        int at = email.indexOf('@');
        int dot = email.lastIndexOf('.');
        return at > 0 && dot > at + 1 && dot < email.length() - 1;
    }

    private void showDrivers() {
        while (true) {
            ConsoleWriter.printMenu(title() + " / Доставщики", DRIVER_ITEMS);
            int choice = reader.readMenuChoice(DRIVER_ITEMS.size());
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
                ConsoleWriter.printForm("Новый доставщик");
                String fullName = reader.readString("ФИО", 3, 150);
                String phone = reader.readString("Телефон", 5, 32);
                Driver created = driverService.createDriver(new Driver(fullName, phone));
                ConsoleWriter.printSuccess("Доставщик создан, ID " + created.getId());
            }
            case 4 -> editDriver();
            case 5 -> {
                Long driverId = chooseDriver("Активация: неактивные доставщики",
                    driverService.findAll().stream().filter(driver -> !driver.isActive()).toList());
                if (driverId == null) {
                    return;
                }
                driverService.activate(driverId);
                ConsoleWriter.printSuccess("Доставщик " + driverId + " активирован");
            }
            case 6 -> {
                Long driverId = chooseDriver("Деактивация: активные доставщики",
                    driverService.findActive());
                if (driverId == null) {
                    return;
                }
                driverService.deactivate(driverId);
                ConsoleWriter.printSuccess("Доставщик " + driverId + " деактивирован");
            }
            case 7 -> {
                Set<Long> withRequests = operatorService.findAllRequests().stream()
                    .map(TransportationRequest::getDriverId)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toSet());
                Long driverId = chooseDriver("Удаление доставщика: без заявок",
                    driverService.findAll().stream()
                        .filter(driver -> !withRequests.contains(driver.getId())).toList());
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

    private void editDriver() {
        Long driverId = chooseDriver("Изменение доставщика", driverService.findAll());
        if (driverId == null) {
            return;
        }
        Driver existing = driverService.getById(driverId);
        String fullName = reader.readEditedString("ФИО", existing.getFullName(), 3, 150);
        String phone = reader.readEditedString("Телефон", existing.getPhone(), 5, 32);
        if (fullName.equals(existing.getFullName()) && phone.equals(existing.getPhone())) {
            ConsoleWriter.printInfo("Изменений нет");
            return;
        }
        driverService.updateDriver(new Driver(driverId, fullName, phone, existing.isActive()));
        ConsoleWriter.printSuccess("Доставщик " + driverId + " обновлён");
    }

    private Long chooseDriver(String title, List<Driver> drivers) {
        if (drivers.isEmpty()) {
            ConsoleWriter.printTitle(title);
            ConsoleWriter.printInfo("Подходящих доставщиков нет");
            return null;
        }
        ConsoleWriter.printForm(title);
        ConsoleWriter.printDrivers(drivers);
        return reader.readIdFrom("ID доставщика", ConsoleWriter.driverIdsOf(drivers));
    }

    private void searchRequests() {
        ConsoleWriter.printForm("Поиск заявок");
        TransportationRequestSearchCriteria criteria = new TransportationRequestSearchCriteria();
        criteria.setSearchText(reader.readOptionalString("Текст поиска", 200));
        if (reader.confirm("Дополнительные фильтры?")) {
            readSearchFilters(criteria);
        }

        List<TransportationRequestView> found = context.queries().search(criteria);
        ConsoleWriter.printTitle("Результаты поиска");
        ConsoleWriter.printRequestViews(found);

        if (!found.isEmpty() && reader.confirm("Выгрузить результат в Excel?")) {
            exportToFile(found, "requests");
        }
    }

    private void readSearchFilters(TransportationRequestSearchCriteria criteria) {
        criteria.setStatus(reader.readOptionalEnum("Статус", TransportationRequestStatus.class));
        criteria.setClientId(reader.readOptionalLong("ID клиента", 1));
        criteria.setDriverId(reader.readOptionalLong("ID доставщика", 1));
        criteria.setMinPrice(reader.readOptionalBigDecimal("Цена от", BigDecimal.ZERO));
        criteria.setMaxPrice(reader.readOptionalBigDecimal("Цена до", BigDecimal.ZERO));
        while (criteria.getMinPrice() != null && criteria.getMaxPrice() != null
            && criteria.getMaxPrice().compareTo(criteria.getMinPrice()) < 0) {
            ConsoleWriter.printError("Цена до должна быть не меньше цены от");
            criteria.setMaxPrice(reader.readOptionalBigDecimal("Цена до", BigDecimal.ZERO));
        }
        criteria.setPlannedFrom(reader.readOptionalDateTime("Доставка с"));
        criteria.setPlannedTo(reader.readOptionalDateTime("Доставка по"));
        while (criteria.getPlannedFrom() != null && criteria.getPlannedTo() != null
            && criteria.getPlannedTo().isBefore(criteria.getPlannedFrom())) {
            ConsoleWriter.printError("Конец периода должен быть не раньше начала");
            criteria.setPlannedTo(reader.readOptionalDateTime("Доставка по"));
        }
        TransportationRequestSortField sort = reader.readOptionalEnum(
            "Сортировка (по умолчанию — дата доставки)", TransportationRequestSortField.class);
        if (sort != null) {
            criteria.setSortField(sort);
        }
        SortDirection direction = reader.readOptionalEnum(
            "Направление (по умолчанию — по возрастанию)", SortDirection.class);
        if (direction != null) {
            criteria.setSortDirection(direction);
        }
    }

    private void exportAllRequests() {
        List<TransportationRequestView> all =
            context.queries().search(new TransportationRequestSearchCriteria());
        if (all.isEmpty()) {
            ConsoleWriter.printTitle("Экспорт всех заявок");
            ConsoleWriter.printInfo("Список пуст");
            return;
        }
        ConsoleWriter.printForm("Экспорт всех заявок");
        exportToFile(all, "requests_all");
    }

    private void exportToFile(List<TransportationRequestView> requests, String namePrefix) {
        String defaultName = namePrefix + "_"
            + LocalDateTime.now().format(FILE_NAME_TIME) + ".xlsx";

        Path target = reader.readFilePath("Путь к файлу", defaultName, ".xlsx");
        ExportResult result = context.exporter().export(target, requests);
        ConsoleWriter.printSuccess(
            "Выгружено заявок: " + result.requestCount() + ", файл " + result.file()
        );
    }

    private void showStatistics() {
        ConsoleWriter.printTitle("Статистика по статусам");
        ConsoleWriter.printStatistics(context.queries().getStatisticsByStatus());
    }
}
