package com.lcorp.console.app;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.model.Client;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.util.ConsoleReader;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

public final class ConsoleNavigationCheck {
    private static final String HINT = "0 — отменить и вернуться в меню";

    public static void main(String[] args) throws Exception {
        try (ApplicationContext context = new ApplicationContext(DatabaseConfig.load())) {
            var operator = context.operators().findAll().getFirst();
            Session session = new Session(Role.OPERATOR, operator.getId(), operator.getFullName());

            String login = capture(() -> new ConsoleApp(context, reader("1\n0\n0\n")).run());
            require(login.contains("0) Выход") && login.contains(HINT) && login.contains("Вход отменён")
                && login.contains("Завершение работы") && !login.contains("[Ошибка]"), "Отмена входа");
            String labels = operator(context, session, "1\n0\n0\n");
            require(labels.contains("0) Сменить пользователя") && labels.contains("0) Назад"),
                "Подписи пункта 0 по действию");

            Client client = context.clients().createClient(new Client("Console check " + UUID.randomUUID(),
                "+7-000-000-00-00", "check@example.com"));
            try {
                int count = context.requests().findAll().size();
                String cancel = operator(context, session, "1\n4\n" + client.getId()
                    + "\nПробный груз\n0\n0\n0\n");
                require(cancel.contains(HINT) && cancel.contains("Операция отменена")
                    && !cancel.contains("Enter — вернуться в меню"), "Отмена формы сразу возвращает в меню");
                require(context.requests().findAll().size() == count, "Нет частично созданной заявки");

                operator(context, session, "2\n3\n" + client.getId() + "\nДругое имя\n0\n0\n0\n");
                require(context.clients().getById(client.getId()).getFullName().equals(client.getFullName()),
                    "Отмена редактирования не сохраняет уже введённое имя");
                String edit = operator(context, session, "2\n3\n" + client.getId() + "\n\n\n\n\n0\n0\n");
                Client unchanged = context.clients().getById(client.getId());
                require(edit.contains("[" + client.getFullName() + "]") && edit.contains("[check@example.com]")
                    && edit.contains("Изменений нет") && unchanged.getEmail().equals(client.getEmail())
                    && unchanged.getPhone().equals(client.getPhone()), "Enter оставляет значения");
                operator(context, session, "2\n3\n" + client.getId() + "\n\n\n-\n\n0\n0\n");
                require(context.clients().getById(client.getId()).getEmail() == null, "«-» удаляет email");
                operator(context, session, "2\n3\n" + client.getId()
                    + "\n\n-\n\n\nплохой\ncheck@example.com\n\n0\n0\n");
                Client restored = context.clients().getById(client.getId());
                require(restored.getPhone() == null && "check@example.com".equals(restored.getEmail()),
                    "Контакт и email исправляются в той же форме");

                String search = operator(context, session, "4\n" + client.getFullName() + "\n\n\n0\n");
                require(search.contains("Дополнительные фильтры?") && search.contains("Результаты поиска")
                    && !search.contains("Цена от") && !search.contains("Поле сортировки"), "Короткий поиск");
                String cancelledExport = operator(context, session, "6\n0\n0\n");
                require(cancelledExport.contains("Операция отменена")
                    && !cancelledExport.contains("Выгружено заявок"), "Отмена экспорта");

                String date = LocalDateTime.now().plusDays(2).format(ConsoleReader.DATE_TIME_FORMAT);
                String created = operator(context, session, "1\n4\n" + client.getId()
                    + "\nПробный груз\n-1\n0.0\n1.123\n0,00\n100.50\n31.02.2028 12:00\n01.01.2000 12:00\n"
                    + date + "\nМосква\nМосква\nТверь\n\n0\n0\n");
                require(created.contains("Заявка создана") && !created.contains("[Ошибка] Invalid")
                    && !created.contains("значение должно быть больше нуля"),
                    "Ошибки полей исправляются сразу, без отказа сервиса");
                var ownRequests = context.clients().findRequests(client.getId());
                require(ownRequests.size() == 1
                    && ownRequests.getFirst().getCargoDescription().equals("Пробный груз")
                    && ownRequests.getFirst().getWeightKg().compareTo(new BigDecimal("1.123")) == 0
                    && ownRequests.getFirst().getPrice().compareTo(new BigDecimal("100.50")) == 0,
                    "Повторный ввод сохраняет остальные поля");

                Path directory = Files.createTempDirectory("console-export-check-");
                Path exported = directory.resolve("filtered.xlsx");
                try {
                    String exportedOutput = operator(context, session,
                        "4\n" + client.getFullName() + "\n\n1\n" + exported + "\n\n0\n");
                    require(exportedOutput.contains("Выгружено заявок: 1"), "Экспорт поиска через меню");
                    try (var stream = Files.newInputStream(exported); var workbook = new XSSFWorkbook(stream)) {
                        require(workbook.getSheet("Заявки").getLastRowNum() == 1, "Экспорт выбранной заявки");
                        require(workbook.getSheet("Статистика").getRow(1).getCell(2).getNumericCellValue() == 100.5,
                            "Статистика поиска не включает другие заявки БД");
                    }
                    operator(context, session, "6\n" + exported + "\n1\n\n0\n");
                    try (var stream = Files.newInputStream(exported); var workbook = new XSSFWorkbook(stream)) {
                        require(workbook.getSheet("Заявки").getLastRowNum() == count + 1,
                            "Экспорт всех заявок через меню с подтверждённой перезаписью");
                    }
                } finally {
                    Files.deleteIfExists(exported);
                    Files.deleteIfExists(directory);
                }

                List<TransportationRequest> all = context.requests().findAll();
                checkChoices(context, session, 5, all, r -> r.getStatus() == TransportationRequestStatus.CREATED);
                checkChoices(context, session, 6, all, r -> r.getStatus() == TransportationRequestStatus.CREATED);
                checkChoices(context, session, 7, all, r -> r.getStatus() == TransportationRequestStatus.APPROVED
                    && r.getDriverId() == null);
                checkChoices(context, session, 8, all, r -> r.getStatus() == TransportationRequestStatus.APPROVED
                    && r.getDriverId() != null);
                checkChoices(context, session, 9, all, r -> r.getStatus() == TransportationRequestStatus.CREATED
                    || r.getStatus() == TransportationRequestStatus.APPROVED);

                var assigned = all.stream().filter(r -> r.getStatus() == TransportationRequestStatus.APPROVED
                    && r.getDriverId() != null).findFirst();
                if (assigned.isPresent()) {
                    String reassign = operator(context, session,
                        "1\n8\n" + assigned.get().getId() + "\n0\n0\n0\n");
                    String drivers = reassign.substring(reassign.indexOf("Выбор доставщика"));
                    require(!Pattern.compile("(?m)^" + assigned.get().getDriverId() + "\\s{2,}")
                        .matcher(drivers).find(), "Текущий доставщик не предлагается при переназначении");
                }
                String deleteClient = operator(context, session, "2\n4\n0\n0\n0\n");
                require(!deleteClient.contains(client.getFullName()), "Клиент с заявкой не предлагается к удалению");

                var driver = context.drivers().findActive().getFirst();
                Session driverSession = new Session(Role.DRIVER, driver.getId(), driver.getFullName());
                for (int action : List.of(3, 4, 5, 6)) {
                    Predicate<TransportationRequest> allowed = switch (action) {
                        case 3 -> r -> r.getStatus() == TransportationRequestStatus.APPROVED
                            && r.getDriverId() == null;
                        case 6 -> r -> driver.getId().equals(r.getDriverId())
                            && r.getStatus() == TransportationRequestStatus.IN_TRANSIT;
                        default -> r -> driver.getId().equals(r.getDriverId())
                            && r.getStatus() == TransportationRequestStatus.APPROVED;
                    };
                    boolean any = all.stream().anyMatch(allowed);
                    String input = action + "\n" + (any ? "0\n" : "\n") + "0\n";
                    String output = capture(() -> new DriverMenu(context, reader(input), driverSession).show());
                    assertRows(output, all, allowed);
                }
            } finally {
                for (var request : context.clients().findRequests(client.getId())) {
                    context.requests().deleteRequest(request.getId());
                }
                context.clients().deleteClient(client.getId());
            }
        }
        System.out.println("PASS: labels, 0 cancel, Enter/- editing, field validation, short search,"
            + " XLSX export, action filtering");
    }

    private static void checkChoices(ApplicationContext context, Session session, int action,
                                     List<TransportationRequest> all, Predicate<TransportationRequest> allowed) {
        boolean any = all.stream().anyMatch(allowed);
        String output = operator(context, session, "1\n" + action + "\n" + (any ? "0\n" : "\n") + "0\n0\n");
        require(any == output.contains(HINT), "Подсказка отмены только перед вводом");
        assertRows(output, all, allowed);
    }
    private static void assertRows(String output, List<TransportationRequest> all,
                                   Predicate<TransportationRequest> allowed) {
        for (var request : all) {
            boolean shown = Pattern.compile("(?m)^" + request.getId() + "\\s{2,}").matcher(output).find();
            require(shown == allowed.test(request), "Неверный список для действия: " + request.getId());
        }
    }
    private static String operator(ApplicationContext context, Session session, String input) {
        return capture(() -> new OperatorMenu(context, reader(input), session).show());
    }
    private static ConsoleReader reader(String input) { return new ConsoleReader(new Scanner(input)); }
    private static String capture(Runnable action) {
        PrintStream original = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (PrintStream output = new PrintStream(bytes, true, StandardCharsets.UTF_8)) {
            System.setOut(output);
            action.run();
        } finally {
            System.setOut(original);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
