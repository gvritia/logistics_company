package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.Client;
import com.lcorp.console.repository.ClientRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

// JDBC-реализация интерфейса клиентов, который ожидают сервисы КР1.
public final class JdbcClientRepository implements ClientRepository {
    private final DatabaseConnectionFactory database;

    public JdbcClientRepository(DatabaseConnectionFactory database) {
        this.database = database;
    }

    // @Override указывает, что метод реализует контракт ClientRepository.
    @Override
    public Client create(Client client) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "INSERT INTO clients (full_name, phone, email) VALUES (?, ?, ?)",
                 Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, client);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) client.setId(keys.getLong(1));
            }
            return client;
        } catch (SQLException e) {
            throw failure("создать клиента", e);
        }
    }

    @Override
    public Optional<Client> findById(Long id) {
        return select("WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<Client> findAll() {
        return select("ORDER BY full_name, id");
    }

    @Override
    public boolean update(Client client) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "UPDATE clients SET full_name = ?, phone = ?, email = ? WHERE id = ?")) {
            bind(statement, client);
            statement.setLong(4, client.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("изменить клиента", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement("DELETE FROM clients WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("удалить клиента", e);
        }
    }

    // Подставляет значения отдельно от SQL и собирает модели из ResultSet.
    private List<Client> select(String suffix, Object... args) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "SELECT id, full_name, phone, email FROM clients " + suffix)) {
            for (int i = 0; i < args.length; i++) statement.setObject(i + 1, args[i]);
            try (ResultSet rows = statement.executeQuery()) {
                List<Client> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(new Client(rows.getLong("id"), rows.getString("full_name"),
                        rows.getString("phone"), rows.getString("email")));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure("загрузить клиентов", e);
        }
    }

    private void bind(PreparedStatement statement, Client client) throws SQLException {
        statement.setString(1, client.getFullName());
        statement.setString(2, client.getPhone());
        statement.setString(3, client.getEmail());
    }

    private DataAccessException failure(String action, SQLException cause) {
        return new DataAccessException("Не удалось " + action + " в базе данных", cause);
    }
}
