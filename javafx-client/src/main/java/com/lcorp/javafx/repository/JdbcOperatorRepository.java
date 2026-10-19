package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.Operator;
import com.lcorp.console.repository.OperatorRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcOperatorRepository implements OperatorRepository {
    private final DatabaseConnectionFactory database;

    public JdbcOperatorRepository(DatabaseConnectionFactory database) {
        this.database = database;
    }

    @Override
    public Operator create(Operator operator) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO operators (full_name, phone) VALUES (?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, operator);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) operator.setId(keys.getLong(1));
            }
            return operator;
        } catch (SQLException e) {
            throw failure("создать оператора", e);
        }
    }

    @Override
    public Optional<Operator> findById(Long id) {
        return select("WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<Operator> findAll() {
        return select("ORDER BY full_name, id");
    }

    @Override
    public boolean update(Operator operator) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE operators SET full_name = ?, phone = ? WHERE id = ?")) {
            bind(statement, operator);
            statement.setLong(3, operator.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("изменить оператора", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM operators WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("удалить оператора", e);
        }
    }

    private List<Operator> select(String suffix, Object... args) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, full_name, phone FROM operators " + suffix)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (ResultSet rows = statement.executeQuery()) {
                List<Operator> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(new Operator(rows.getLong("id"), rows.getString("full_name"),
                        rows.getString("phone")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure("загрузить операторов", e);
        }
    }

    private void bind(PreparedStatement statement, Operator operator) throws SQLException {
        statement.setString(1, operator.getFullName());
        statement.setString(2, operator.getPhone());
    }

    private DataAccessException failure(String action, SQLException cause) {
        return new DataAccessException("Не удалось " + action + " в базе данных", cause);
    }
}
