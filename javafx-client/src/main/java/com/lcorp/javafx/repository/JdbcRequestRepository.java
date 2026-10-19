package com.lcorp.javafx.repository;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.repository.TransportationRequestRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class JdbcRequestRepository implements TransportationRequestRepository {
    private static final String COLUMNS = "id, client_id, driver_id, operator_id, cargo_description, "
        + "weight_kg, price, created_at, planned_delivery_at, status, pickup_address, delivery_address";
    private final DatabaseConnectionFactory database;

    public JdbcRequestRepository(DatabaseConnectionFactory database) {
        this.database = database;
    }

    @Override
    public TransportationRequest create(TransportationRequest request) {
        String sql = "INSERT INTO transportation_requests (client_id, driver_id, operator_id, "
            + "cargo_description, weight_kg, price, created_at, planned_delivery_at, status, "
            + "pickup_address, delivery_address) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bind(statement, request, false);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    request.setId(keys.getLong(1));
                }
            }
            return request;
        } catch (SQLException e) {
            throw failure("создать заявку", e);
        }
    }

    @Override
    public Optional<TransportationRequest> findById(Long id) {
        return select("WHERE id = ?", id).stream().findFirst();
    }

    @Override
    public List<TransportationRequest> findAll() {
        return select("ORDER BY id DESC");
    }

    @Override
    public boolean update(TransportationRequest request) {
        String sql = "UPDATE transportation_requests SET client_id = ?, driver_id = ?, "
            + "operator_id = ?, cargo_description = ?, weight_kg = ?, price = ?, "
            + "created_at = ?, planned_delivery_at = ?, status = ?, pickup_address = ?, "
            + "delivery_address = ? WHERE id = ?";
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, request, true);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("изменить заявку", e);
        }
    }

    @Override
    public boolean deleteById(Long id) {
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(
                 "DELETE FROM transportation_requests WHERE id = ?")) {
            statement.setLong(1, id);
            return statement.executeUpdate() == 1;
        } catch (SQLException e) {
            throw failure("удалить заявку", e);
        }
    }

    @Override
    public List<TransportationRequest> findAvailable() {
        return select("WHERE status = 'APPROVED' AND driver_id IS NULL ORDER BY planned_delivery_at");
    }

    @Override
    public List<TransportationRequest> findByDriverId(Long driverId) {
        return select("WHERE driver_id = ? ORDER BY planned_delivery_at", driverId);
    }

    private List<TransportationRequest> select(String suffix, Object... parameters) {
        String sql = "SELECT " + COLUMNS + " FROM transportation_requests " + suffix;
        try (Connection connection = database.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (int i = 0; i < parameters.length; i++) {
                statement.setObject(i + 1, parameters[i]);
            }
            try (ResultSet rows = statement.executeQuery()) {
                List<TransportationRequest> result = new ArrayList<>();
                while (rows.next()) {
                    result.add(map(rows));
                }
                return result;
            }
        } catch (SQLException e) {
            throw failure("загрузить заявки", e);
        }
    }

    private void bind(PreparedStatement statement, TransportationRequest request, boolean withId)
        throws SQLException {
        statement.setLong(1, request.getClientId());
        statement.setObject(2, request.getDriverId());
        statement.setLong(3, request.getOperatorId());
        statement.setString(4, request.getCargoDescription());
        statement.setBigDecimal(5, request.getWeightKg());
        statement.setBigDecimal(6, request.getPrice());
        statement.setTimestamp(7, Timestamp.valueOf(request.getCreatedAt()));
        statement.setTimestamp(8, Timestamp.valueOf(request.getPlannedDeliveryAt()));
        statement.setString(9, request.getStatus().name());
        statement.setString(10, request.getPickupAddress());
        statement.setString(11, request.getDeliveryAddress());
        if (withId) {
            statement.setLong(12, request.getId());
        }
    }

    private TransportationRequest map(ResultSet rows) throws SQLException {
        Long driverId = (Long) rows.getObject("driver_id");
        return new TransportationRequest(
            rows.getLong("id"), rows.getLong("client_id"), driverId,
            rows.getLong("operator_id"), rows.getString("cargo_description"),
            rows.getBigDecimal("weight_kg"), rows.getBigDecimal("price"),
            rows.getTimestamp("created_at").toLocalDateTime(),
            rows.getTimestamp("planned_delivery_at").toLocalDateTime(),
            TransportationRequestStatus.valueOf(rows.getString("status")),
            rows.getString("pickup_address"), rows.getString("delivery_address")
        );
    }

    private DataAccessException failure(String operation, SQLException cause) {
        return new DataAccessException("Не удалось " + operation + " в базе данных", cause);
    }
}
