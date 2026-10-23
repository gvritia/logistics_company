package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.Driver;
import com.lcorp.console.repository.DriverRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// Сохраняет и загружает доставщиков через JDBC, включая признак активности.
public final class JdbcDriverRepository implements DriverRepository {
    private final DatabaseConnectionFactory database;

    public JdbcDriverRepository(DatabaseConnectionFactory database) {
        this.database = database;
    }

    // @Override указывает, что метод реализует контракт DriverRepository.
    @Override
    public Driver create(Driver driver) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO drivers (full_name, phone, active) VALUES (?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, driver);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) driver.setId(keys.getLong(1));
            }
            return driver;
        } catch (SQLException e) {
            throw failure("создать доставщика", e);
        }
    }

    @Override
    public Optional<Driver> findById(Long id) {
        return select("WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<Driver> findAll() {
        return select("ORDER BY full_name, id");
    }

    // Только активные доставщики могут получить новое назначение.
    @Override
    public List<Driver> findActive() {
        return select("WHERE active ORDER BY full_name, id");
    }

    @Override
    public boolean update(Driver driver) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE drivers SET full_name = ?, phone = ?, active = ? WHERE id = ?")) {
            bind(statement, driver);
            statement.setLong(4, driver.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("изменить доставщика", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM drivers WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("удалить доставщика", e);
        }
    }

    // Общая выборка восстанавливает признак active для каждого доставщика.
    private List<Driver> select(String suffix, Object... args) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, full_name, phone, active FROM drivers " + suffix)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (ResultSet rows = statement.executeQuery()) {
                List<Driver> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(new Driver(rows.getLong("id"), rows.getString("full_name"),
                        rows.getString("phone"), rows.getBoolean("active")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure("загрузить доставщиков", e);
        }
    }

    private void bind(PreparedStatement statement, Driver driver) throws SQLException {
        statement.setString(1, driver.getFullName());
        statement.setString(2, driver.getPhone());
        statement.setBoolean(3, driver.isActive());
    }

    private DataAccessException failure(String action, SQLException cause) {
        return new DataAccessException("Не удалось " + action + " в базе данных", cause);
    }
}
