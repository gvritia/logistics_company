package com.lcorp.console.app;

import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.service.DriverService;
import com.lcorp.console.util.ConsoleReader;
import com.lcorp.console.util.ConsoleWriter;
import com.lcorp.console.util.ErrorHandler;

import java.util.List;
import java.util.Objects;

public final class DriverMenu implements Menu {

    private static final List<String> ITEMS = List.of(
        "Доступные заявки",
        "Мои заявки",
        "Взять заявку",
        "Отказаться от заявки",
        "Начать перевозку",
        "Завершить доставку"
    );

    private final DriverService driverService;
    private final ConsoleReader reader;


    private final Session session;

    public DriverMenu(ApplicationContext context, ConsoleReader reader, Session session) {
        this.driverService = Objects.requireNonNull(context).drivers();
        this.reader = Objects.requireNonNull(reader);
        this.session = Objects.requireNonNull(session);
    }

    @Override
    public void show() {
        while (true) {
            ConsoleWriter.printMenu("Доставщик: " + session.getActorName(), ITEMS);
            int choice = reader.readInt("Выбор", 0, ITEMS.size());
            if (choice == 0) {
                return;
            }
            ErrorHandler.run(() -> handle(choice));
            reader.pause();
        }
    }

    private void handle(int choice) {
        switch (choice) {
            case 1 -> {
                ConsoleWriter.printTitle("Доступные заявки");
                ConsoleWriter.printRequests(available());
            }
            case 2 -> {
                ConsoleWriter.printTitle("Мои заявки");
                ConsoleWriter.printRequests(assigned());
            }

            case 3 -> takeRequest();
            case 4 -> rejectAssignment();
            case 5 -> startTransportation();
            case 6 -> completeDelivery();
            default -> ConsoleWriter.printError("Неизвестный пункт меню ");
        }
    }

    private void takeRequest() {
        Long requestId = chooseRequest("Доступные заявки", available());
        if (requestId == null) return;

        driverService.takeRequest(session.getActorId(), requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " взята в работу");
    }

    private void rejectAssignment() {
        Long requestId = chooseRequest("Мои заявки", assigned());
        if (requestId == null) return;
        if (!reader.confirm("Отказаться от заявки " + requestId + "?")) {
            return;
        }
        driverService.rejectAssignment(session.getActorId(), requestId);
        ConsoleWriter.printSuccess("Отказ от заявки " + requestId + " оформлен");
    }

    private void startTransportation() {
        Long requestId = chooseRequest("Мои заявки", assigned());
        if (requestId == null) return;
        driverService.startTransportation(session.getActorId(), requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " в пути");
    }

    private void completeDelivery() {
        Long requestId = chooseRequest("Мои заявки", assigned());
        if (requestId == null) return;
        driverService.completeDelivery(session.getActorId(), requestId);
        ConsoleWriter.printSuccess("Заявка " + requestId + " доставлена");
    }

    private Long chooseRequest(String title, List<TransportationRequest> requests) {
        ConsoleWriter.printTitle(title);
        ConsoleWriter.printRequests(requests);
        if (requests.isEmpty()) return null;
        return reader.readIdFrom("ID заявки", ConsoleWriter.idsOf(requests));
    }

    private List<TransportationRequest> available() {
        return driverService.findAvailableRequests(session.getActorId());
    }

    private List<TransportationRequest> assigned() {
        return driverService.findAssignedRequests(session.getActorId());
    }
}
