package com.lcorp.console.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// Загружает параметры подключения, не сохраняя пароль в исходном коде
public final class DatabaseConfig {

    private static final Path DOT_ENV_PATH = Path.of(".env");

    private final String jdbcUrl;
    private final String username;
    private final String password;

    private DatabaseConfig(String jdbcUrl, String username, String password) {
        this.jdbcUrl = jdbcUrl;
        this.username = username;
        this.password = password;
    }

    public static DatabaseConfig load() {
        Map<String, String> fileValues = readDotEnv();

        String host = requireSetting("POSTGRES_HOST", fileValues);
        int port = parsePort(requireSetting("POSTGRES_PORT", fileValues));
        String database = requireSetting("POSTGRES_DB", fileValues);
        String username = requireSetting("POSTGRES_USER", fileValues);
        String password = requireSetting("POSTGRES_PASSWORD", fileValues);

        String jdbcUrl = "jdbc:postgresql://" + host + ":" + port + "/" + database;
        return new DatabaseConfig(jdbcUrl, username, password);
    }

    public String getJdbcUrl() {
        return jdbcUrl;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    private static Map<String, String> readDotEnv() {
        if (!Files.exists(DOT_ENV_PATH)) {
            return Map.of();
        }

        try {
            List<String> lines = Files.readAllLines(DOT_ENV_PATH, StandardCharsets.UTF_8);
            Map<String, String> values = new HashMap<>();

            for (String line : lines) {
                String trimmedLine = line.trim(); // trim удаляет пробелы
                // пропуск пустых строк или комментариев
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("//")) {
                    continue;
                }

                int separatorIndex = trimmedLine.indexOf('=');
                if (separatorIndex <= 0) {
                    throw new IllegalStateException(
                        "Некорректная строка в .env: ожидается формат KEY=VALUE"
                    );
                }
                // substring вырезает часть строки
                String key = trimmedLine.substring(0, separatorIndex).trim();
                String value = trimmedLine.substring(separatorIndex + 1).trim();
                // removeMatchingQuotes убирает кавычки, что бы не было "123" а просто 123
                values.put(key, removeMatchingQuotes(value));
            }

            return values;
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать файл .env", exception);
        }
    }

    // Параметр -DKEY=value имеет приоритет над переменной окружения и .env
    private static String requireSetting(String key, Map<String, String> fileValues) {
        String value = firstNotBlank(
            System.getProperty(key),
            System.getenv(key),
            fileValues.get(key)
        );

        if (value == null) {
            throw new IllegalStateException(
                "Не задан параметр подключения " + key
                    + ". Укажите его в переменной окружения или файле .env"
            );
        }

        return value;
    }

    private static String firstNotBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static int parsePort(String value) {
        try {
            int port = Integer.parseInt(value);
            if (port < 1 || port > 65_535) {
                throw new IllegalStateException(
                    "POSTGRES_PORT должен находиться в диапазоне от 1 до 65535"
                );
            }
            return port;
        } catch (NumberFormatException exception) {
            throw new IllegalStateException(
                "POSTGRES_PORT должен быть целым числом",
                exception
            );
        }
    }

    private static String removeMatchingQuotes(String value) {
        if (value.length() < 2) {
            return value;
        }

        char first = value.charAt(0);
        char last = value.charAt(value.length() - 1);
        if ((first == '"' && last == '"') || (first == '\'' && last == '\'')) {
            return value.substring(1, value.length() - 1);
        }

        return value;
    }
}
