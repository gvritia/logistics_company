package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.javafx.model.ReferenceOption;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class JdbcRequestCreateDao {
    private static final String INSERT_REQUEST = """
        INSERT INTO public.transportation_requests
            (client_id, operator_id, cargo_description, weight_kg, price,
             created_at, planned_delivery_at, status, pickup_address, delivery_address)
        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        RETURNING id
        """;

    private final DatabaseConnectionFactory connectionFactory;

    public JdbcRequestCreateDao(DatabaseConnectionFactory connectionFactory) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    public List<ReferenceOption> findClients() {
        return findOptions("SELECT id, full_name FROM public.clients ORDER BY full_name, id");
    }

    public List<ReferenceOption> findOperators() {
        return findOptions("SELECT id, full_name FROM public.operators ORDER BY full_name, id");
    }

    public long create(TransportationRequest request) {
        try (var connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(INSERT_REQUEST)) {
            statement.setLong(1, request.getClientId());
            statement.setLong(2, request.getOperatorId());
            statement.setString(3, request.getCargoDescription().trim());
            statement.setBigDecimal(4, request.getWeightKg());
            statement.setBigDecimal(5, request.getPrice());
            statement.setTimestamp(6, Timestamp.valueOf(request.getCreatedAt()));
            statement.setTimestamp(7, Timestamp.valueOf(request.getPlannedDeliveryAt()));
            statement.setString(8, request.getStatus().name());
            statement.setString(9, request.getPickupAddress().trim());
            statement.setString(10, request.getDeliveryAddress().trim());

            try (ResultSet result = statement.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("База не вернула ID новой заявки");
                }
                return result.getLong("id");
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Не удалось сохранить заявку", exception);
        }
    }

    private List<ReferenceOption> findOptions(String sql) {
        try (var connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            List<ReferenceOption> options = new ArrayList<>();
            while (result.next()) {
                options.add(new ReferenceOption(result.getLong("id"), result.getString("full_name")));
            }
            return options;
        } catch (SQLException exception) {
            throw new DataAccessException("Не удалось загрузить список участников", exception);
        }
    }
}
