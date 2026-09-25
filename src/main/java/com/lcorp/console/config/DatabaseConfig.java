package com.lcorp.console.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        int port = Integer.parseInt(requireSetting("POSTGRES_PORT", fileValues));
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
                String trimmedLine = line.trim();
                if (trimmedLine.isEmpty() || trimmedLine.startsWith("#")
                    || trimmedLine.startsWith("//")) {
                    continue;
                }
                int separatorIndex = trimmedLine.indexOf('=');
                String key = trimmedLine.substring(0, separatorIndex).trim();
                String value = trimmedLine.substring(separatorIndex + 1).trim();
                values.put(key, value);
            }

            return values;
        } catch (IOException exception) {
            throw new IllegalStateException("Не удалось прочитать файл .env", exception);
        }
    }

        private static String requireSetting(String key, Map<String, String> fileValues) {
        String value = firstNotBlank(
            System.getProperty(key),
            System.getenv(key),
            fileValues.get(key)
        );

        if (value == null) {
            throw new IllegalStateException(
                "Value is not set: " + key
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
}
