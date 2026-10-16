package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Objects;

public final class JdbcRequestDeleteDao {
    private static final String DELETE_REQUEST = """
        DELETE FROM public.transportation_requests
        WHERE id = ? AND status = 'CREATED' AND driver_id IS NULL
        """;

    private final DatabaseConnectionFactory connectionFactory;

    public JdbcRequestDeleteDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    // Условие в WHERE не даст удалить заявку, если её статус изменился после выбора строки
    public boolean deleteById(long requestId) {
        try (var connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(DELETE_REQUEST)) {
            statement.setLong(1, requestId);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Не удалось удалить заявку", exception);
        }
    }
}
