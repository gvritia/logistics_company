package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.TransportationRequest;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Objects;

public final class JdbcRequestUpdateDao {
    private static final String UPDATE_REQUEST = """
        UPDATE public.transportation_requests
        SET client_id = ?, operator_id = ?, cargo_description = ?,
            weight_kg = ?, price = ?, planned_delivery_at = ?,
            pickup_address = ?, delivery_address = ?
        WHERE id = ? AND status = 'CREATED' AND driver_id IS NULL
        """;

    private final DatabaseConnectionFactory connectionFactory;

    public JdbcRequestUpdateDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    // Условие в WHERE повторно защищает заявку, если её статус успел измениться после открытия формы
    public boolean update(TransportationRequest request) {
        try (var connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(UPDATE_REQUEST)) {
            statement.setLong(1, request.getClientId());
            statement.setLong(2, request.getOperatorId());
            statement.setString(3, request.getCargoDescription().trim());
            statement.setBigDecimal(4, request.getWeightKg());
            statement.setBigDecimal(5, request.getPrice());
            statement.setTimestamp(6, Timestamp.valueOf(request.getPlannedDeliveryAt()));
            statement.setString(7, request.getPickupAddress().trim());
            statement.setString(8, request.getDeliveryAddress().trim());
            statement.setLong(9, request.getId());
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new DataAccessException("Не удалось обновить заявку", exception);
        }
    }
}
