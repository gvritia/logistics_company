package com.lcorp.console.app;

import com.lcorp.console.model.Driver;
import com.lcorp.console.model.Operator;
import com.lcorp.console.util.ConsoleReader;
import com.lcorp.console.util.ConsoleWriter;
import com.lcorp.console.util.InputCancelledException;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public final class ConsoleApp {

    private final ApplicationContext context;
    private final ConsoleReader reader;

    public ConsoleApp(ApplicationContext context, ConsoleReader reader) {
        this.context = Objects.requireNonNull(context);
        this.reader = Objects.requireNonNull(reader);
    }

    public void run() {
        ConsoleWriter.printInfo("Логистическая компания");

        while (true) {
            ConsoleWriter.printMenu("Выбор роли", List.of(
                Role.OPERATOR.getTitle(),
                Role.DRIVER.getTitle()
            ), "Выход");
            int choice = reader.readMenuChoice(2);
            if (choice == 0) {
                ConsoleWriter.printInfo("Завершение работы");
                return;
            }

            Role role = choice == 1 ? Role.OPERATOR : Role.DRIVER;
            try {
                Session session = login(role);
                if (session != null) menuFor(session).show();
            } catch (InputCancelledException exception) {
                ConsoleWriter.printInfo("Вход отменён");
            }
        }
    }

    private Session login(Role role) {
        return role == Role.OPERATOR ? loginOperator() : loginDriver();
    }

    private Session loginOperator() {
        List<Operator> operators = context.operators().findAll();
        if (operators.isEmpty()) {
            ConsoleWriter.printError("В базе нет операторов");
            return null;
        }

        ConsoleWriter.printForm("Вход оператора");
        ConsoleWriter.printOperators(operators);

        Set<Long> ids = operators.stream().map(Operator::getId).collect(Collectors.toSet());
        long id = reader.readIdFrom("ID оператора", ids);

        Operator operator = operators.stream()
            .filter(candidate -> candidate.getId().equals(id))
            .findFirst()
            .orElseThrow();
        return new Session(Role.OPERATOR, operator.getId(), operator.getFullName());
    }

    private Session loginDriver() {
        List<Driver> drivers = context.drivers().findActive();
        if (drivers.isEmpty()) {
            ConsoleWriter.printError("В базе нет активных доставщиков");
            return null;
        }

        ConsoleWriter.printForm("Вход доставщика");
        ConsoleWriter.printDrivers(drivers);

        long id = reader.readIdFrom("ID доставщика", ConsoleWriter.driverIdsOf(drivers));

        Driver driver = drivers.stream()
            .filter(candidate -> candidate.getId().equals(id))
            .findFirst()
            .orElseThrow();
        return new Session(Role.DRIVER, driver.getId(), driver.getFullName());
    }

    private Menu menuFor(Session session) {
        return session.getRole() == Role.OPERATOR
            ? new OperatorMenu(context, reader, session)
            : new DriverMenu(context, reader, session);
    }
}
