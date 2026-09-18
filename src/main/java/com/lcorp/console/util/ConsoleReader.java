package com.lcorp.console.util;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

public final class ConsoleReader {

    public static final DateTimeFormatter DATE_TIME_FORMAT =
        DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");

    private final Scanner scanner;

    public ConsoleReader(Scanner scanner) {
        this.scanner = Objects.requireNonNull(scanner);
    }

    public int readInt(String prompt, int min, int max) {
        while (true) {
            String line = readLine(prompt);
            try {
                int value = Integer.parseInt(line);
                if (value < min || value > max) {
                    System.out.println("Введите число от " + min + " до " + max);
                    continue;
                }
                return value;
            } catch (NumberFormatException exception) {
                System.out.println("Введите целое число");
            }
        }
    }

    public long readLong(String prompt, long min) {
        while (true) {
            String line = readLine(prompt);
            try {
                long value = Long.parseLong(line);
                if (value < min) {
                    System.out.println("Значение должно быть не меньше " + min);
                    continue;
                }
                return value;
            } catch (NumberFormatException exception) {
                System.out.println("Введите целое число");
            }
        }
    }

    public Long readOptionalLong(String prompt, long min) {
        while (true) {
            String line = readLine(prompt + " (Enter — пропустить)");
            if (line.isEmpty()) {
                return null;
            }
            try {
                long value = Long.parseLong(line);
                if (value < min) {
                    System.out.println("Значение должно быть не меньше " + min);
                    continue;
                }
                return value;
            } catch (NumberFormatException exception) {
                System.out.println("Введите целое число или нажмите Enter");
            }
        }
    }

    // Принимает только ID из показанного списка, поэтому опечатка видна сразу,
    // не доходя до исключения сервиса
    public long readIdFrom(String prompt, Collection<Long> allowedIds) {
        while (true) {
            long value = readLong(prompt, 1);
            if (allowedIds.contains(value)) {
                return value;
            }
            System.out.println("В списке выше нет строки с ID " + value);
        }
    }

    public void pause() {
        readLine("Enter — продолжить");
    }

    public Path readFilePath(String prompt, String defaultName, String extension) {
        while (true) {
            String line = readLine(prompt + " (Enter — " + defaultName + ")");
            String fileName = line.isEmpty() ? defaultName : line;
            if (!fileName.toLowerCase().endsWith(extension)) {
                fileName = fileName + extension;
            }

            Path path;
            try {
                path = Path.of(fileName).toAbsolutePath();
            } catch (InvalidPathException exception) {
                System.out.println("Недопустимый путь к файлу");
                continue;
            }

            if (Files.isDirectory(path)) {
                System.out.println("Это каталог, укажите имя файла!");
                continue;
            }
            if (Files.exists(path) && !confirm("Файл существует, перезаписать?")) {
                return null;
            }
            return path;
        }
    }

    public String readString(String prompt, int minLength, int maxLength) {
        while (true) {
            String line = readLine(prompt);
            if (line.length() < minLength) {
                System.out.println("Введите не меньше " + minLength + " символов");
                continue;
            }
            if (line.length() > maxLength) {
                System.out.println("Введите не больше " + maxLength + " символов");
                continue;
            }
            return line;
        }
    }

    public String readOptionalString(String prompt, int maxLength) {
        while (true) {
            String line = readLine(prompt + " (Enter — пропустить)");
            if (line.isEmpty()) {
                return null;
            }
            if (line.length() > maxLength) {
                System.out.println("Введите не больше " + maxLength + " символов");
                continue;
            }
            return line;
        }
    }

    public BigDecimal readBigDecimal(String prompt, BigDecimal min, BigDecimal max) {
        while (true) {
            String line = readLine(prompt);
            BigDecimal value = parseDecimal(line);
            if (value == null) {
                continue;
            }
            if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
                System.out.println("Введите значение от " + min + " до " + max);
                continue;
            }
            return value;
        }
    }

    public BigDecimal readOptionalBigDecimal(String prompt, BigDecimal min) {
        while (true) {
            String line = readLine(prompt + " (Enter — пропустить)");
            if (line.isEmpty()) {
                return null;
            }
            BigDecimal value = parseDecimal(line);
            if (value == null) {
                continue;
            }
            if (value.compareTo(min) < 0) {
                System.out.println("Значение должно быть не меньше " + min);
                continue;
            }
            return value;
        }
    }

    public LocalDateTime readDateTime(String prompt) {
        while (true) {
            String line = readLine(prompt + " (дд.мм.гггг чч:мм)");
            LocalDateTime value = parseDateTime(line);
            if (value != null) {
                return value;
            }
        }
    }

    public LocalDateTime readOptionalDateTime(String prompt) {
        while (true) {
            String line = readLine(prompt + " (дд.мм.гггг чч:мм, Enter — пропустить)");
            if (line.isEmpty()) {
                return null;
            }
            LocalDateTime value = parseDateTime(line);
            if (value != null) {
                return value;
            }
        }
    }

    public <E extends Enum<E>> E readEnum(String prompt, Class<E> enumType) {
        List<E> values = List.of(enumType.getEnumConstants());
        System.out.println(prompt);
        for (int index = 0; index < values.size(); index++) {
            System.out.println("  " + (index + 1) + ") " + values.get(index));
        }
        int choice = readInt("Выбор", 1, values.size());
        return values.get(choice - 1);
    }

    public <E extends Enum<E>> E readOptionalEnum(String prompt, Class<E> enumType) {
        List<E> values = List.of(enumType.getEnumConstants());
        System.out.println(prompt + " (0 — пропустить)");
        for (int index = 0; index < values.size(); index++) {
            System.out.println("  " + (index + 1) + ") " + values.get(index));
        }
        int choice = readInt("Выбор", 0, values.size());
        return choice == 0 ? null : values.get(choice - 1);
    }

    public boolean confirm(String prompt) {
        while (true) {
            String line = readLine(prompt + " (д/н)").toLowerCase();
            if (line.equals("д") || line.equals("y")) {
                return true;
            }
            if (line.equals("н") || line.equals("n")) {
                return false;
            }
            System.out.println("Введите 'д' или 'н'");
        }
    }

    public String readLine(String prompt) {
        System.out.print(prompt + ": ");
        try {
            return scanner.nextLine().trim();
        } catch (NoSuchElementException exception) {
            throw new InputClosedException("Ввод завершён", exception);
        }
    }

    private BigDecimal parseDecimal(String line) {
        try {
            return new BigDecimal(line.replace(',', '.'));
        } catch (NumberFormatException exception) {
            System.out.println("Введите число, например 12.5");
            return null;
        }
    }

    private LocalDateTime parseDateTime(String line) {
        try {
            return LocalDateTime.parse(line, DATE_TIME_FORMAT);
        } catch (DateTimeParseException exception) {
            System.out.println("Введите дату в формате 31.12.2026 18:30");
            return null;
        }
    }
}
