package com.lcorp.console.util;

import com.lcorp.console.model.TransportationRequestStatus;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Scanner;

public final class ConsoleReaderCheck {
    public static void main(String[] args) {
        PrintStream original = System.out;
        try (PrintStream quiet = new PrintStream(new ByteArrayOutputStream())) {
            System.setOut(quiet);
            require(reader("0\n").readMenuChoice(3) == 0, "0 в меню");
            require(reader("7\nабв\n2\n").readMenuChoice(3) == 2, "Неверный пункт меню переспрашивается");

            cancelled(() -> reader("0\n").readIdFrom("ID", List.of(1L)));
            cancelled(() -> reader("0\n").readString("Груз", 3, 500));
            cancelled(() -> reader("0\n").readPositiveDecimal("Вес", BigDecimal.TEN, 3));
            cancelled(() -> reader("0\n").readDateTime("Дата"));
            cancelled(() -> reader("0\n").readFilePath("Путь", "test.xlsx", ".xlsx"));
            cancelled(() -> reader("0\n").readOptionalEnum("Статус", TransportationRequestStatus.class));
            cancelled(() -> reader("0\n").confirm("Сохранить?"));
            cancelled(() -> reader("0\n").readEditedString("Имя", "Иван", 3, 150));
            cancelled(() -> reader("0\n").readEditedOptionalString("Email", "old@example.com", 254));

            require(reader("10\n").readString("Адрес", 1, 10).equals("10"), "Отменяет только ровно 0");
            require(reader("\nАБ\nГруз\n").readString("Груз", 3, 500).equals("Груз"), "Обязательное поле");
            require(reader("\n").readOptionalEnum("Статус", TransportationRequestStatus.class) == null,
                "Enter пропускает статус");
            require(reader("\n").readEditedString("Имя", "Иван", 3, 150).equals("Иван"),
                "Enter оставляет имя");
            require(reader("Ив\nПётр\n").readEditedString("Имя", "Иван", 3, 150).equals("Пётр"),
                "Короткое имя переспрашивается");
            require(reader("\n").readEditedOptionalString("Email", "old@example.com", 254)
                .equals("old@example.com"), "Enter оставляет email");
            require(reader("-\n").readEditedOptionalString("Email", "old@example.com", 254) == null,
                "«-» удаляет email");
            require(reader("\n").readEditedOptionalString("Email", null, 254) == null,
                "Enter оставляет пустое поле пустым");
            require(reader("new@example.com\n").readEditedOptionalString("Email", "old@example.com", 254)
                .equals("new@example.com"), "Новое значение email");

            require(reader("-1\n0.0\n11\n1.2345\n1,25\n").readPositiveDecimal("Вес", BigDecimal.TEN, 3)
                .compareTo(new BigDecimal("1.25")) == 0, "Вес: > 0, не больше максимума, 3 знака");
            require(reader("1.500\n").readPositiveDecimal("Цена", BigDecimal.TEN, 2)
                .compareTo(new BigDecimal("1.5")) == 0, "Незначащие нули не считаются знаками");
            require(reader("31.02.2028 12:00\n29.02.2028 12:00\n").readDateTime("Дата")
                .equals(LocalDateTime.of(2028, 2, 29, 12, 0)), "Несуществующая дата отклоняется");
            String future = LocalDateTime.now().plusDays(1).format(ConsoleReader.DATE_TIME_FORMAT);
            require(reader("01.01.2000 12:00\n" + future + "\n").readFutureDateTime("Дата")
                .isAfter(LocalDateTime.now()), "Прошедшая дата отклоняется");

            require(reader("1\n").confirm("Да?"), "1 — да");
            require(reader("Y\n").confirm("Да?"), "y — да");
            require(!reader("2\n").confirm("Нет?"), "2 — нет");
            require(!reader("\n").confirm("Нет?"), "Enter — нет");
            require(reader("x\n1\n").confirm("Да?"), "Непонятный ответ переспрашивается");

            ConsoleReader flow = reader("0\n2\n");
            boolean completed = ErrorHandler.run(() -> flow.readString("Груз", 3, 500));
            require(!completed && flow.readMenuChoice(2) == 2, "Отмена не съедает следующий пункт меню");
            reader("0\n").pause();
            try {
                reader("").readString("Поле", 1, 10);
                throw new AssertionError("EOF должен завершать ввод");
            } catch (InputClosedException expected) { }
        } finally {
            System.setOut(original);
        }
    }

    private static ConsoleReader reader(String input) { return new ConsoleReader(new Scanner(input)); }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
    private static void cancelled(Runnable action) {
        try {
            action.run();
            throw new AssertionError("Ожидалась отмена");
        } catch (InputCancelledException expected) { }
    }
}
