package com.lcorp.console.config;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Objects;

public final class DatabaseConnectionFactory {

    private final DatabaseConfig config;

    public DatabaseConnectionFactory(DatabaseConfig config) {
        this.config = Objects.requireNonNull(config);
    }

    public Connection openConnection() throws SQLException {
        return DriverManager.getConnection(
            config.getJdbcUrl(),
            config.getUsername(),
            config.getPassword()
        );
    }
}
