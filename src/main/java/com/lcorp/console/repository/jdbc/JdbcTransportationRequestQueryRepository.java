package com.lcorp.console.repository.jdbc;

import com.lcorp.console.config.DatabaseConnectionFactory;
import com.lcorp.console.exception.DataAccessException;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.console.model.query.SortDirection;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestSortField;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.repository.TransportationRequestQueryRepository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

// Выполняет только чтение через JDBC. CRUD этого класса не касается.
public class JdbcTransportationRequestQueryRepository
    implements TransportationRequestQueryRepository {

    private static final String SELECT_REQUESTS = """
        SELECT
            r.id,
            r.client_id,
            c.full_name AS client_name,
            r.driver_id,
            d.full_name AS driver_name,
            r.operator_id,
            o.full_name AS operator_name,
            r.cargo_description,
            r.weight_kg,
            r.price,
            r.created_at,
            r.planned_delivery_at,
            r.status,
            r.pickup_address,
            r.delivery_address
        FROM public.transportation_requests r
        JOIN public.clients c ON c.id = r.client_id
        JOIN public.operators o ON o.id = r.operator_id
        LEFT JOIN public.drivers d ON d.id = r.driver_id
        WHERE 1 = 1
        """;

    private static final String SELECT_STATISTICS = """
        SELECT
            r.status,
            COUNT(*) AS request_count,
            COALESCE(SUM(r.price), 0) AS total_price,
            COALESCE(AVG(r.price), 0) AS average_price,
            COALESCE(SUM(r.weight_kg), 0) AS total_weight_kg
        FROM public.transportation_requests r
        GROUP BY r.status
        ORDER BY CASE r.status
            WHEN 'CREATED' THEN 1
            WHEN 'APPROVED' THEN 2
            WHEN 'IN_TRANSIT' THEN 3
            WHEN 'DELIVERED' THEN 4
            WHEN 'CANCELLED' THEN 5
            ELSE 6
        END
        """;

    private final DatabaseConnectionFactory connectionFactory;

    public JdbcTransportationRequestQueryRepository(
        DatabaseConnectionFactory connectionFactory
    ) {
        this.connectionFactory = Objects.requireNonNull(connectionFactory);
    }

    @Override
    public List<TransportationRequestView> search(
        TransportationRequestSearchCriteria criteria
    ) {
        Objects.requireNonNull(criteria);

        StringBuilder sql = new StringBuilder(SELECT_REQUESTS);
        List<Object> parameters = new ArrayList<>();

        appendSearchText(sql, parameters, criteria.getSearchText());
        appendEqual(sql, parameters, "r.status", statusName(criteria.getStatus()));
        appendEqual(sql, parameters, "r.client_id", criteria.getClientId());
        appendEqual(sql, parameters, "r.driver_id", criteria.getDriverId());
        appendLowerBound(sql, parameters, "r.price", criteria.getMinPrice());
        appendUpperBound(sql, parameters, "r.price", criteria.getMaxPrice());
        appendLowerBound(
            sql,
            parameters,
            "r.planned_delivery_at",
            criteria.getPlannedFrom()
        );
        appendUpperBound(
            sql,
            parameters,
            "r.planned_delivery_at",
            criteria.getPlannedTo()
        );

        // Имена столбцов нельзя передать параметром PreparedStatement
        // Поэтому выбираем их только из закрытого enum через switch
        sql.append(" ORDER BY ")
            .append(toOrderByColumn(criteria.getSortField()))
            .append(' ')
            .append(toOrderDirection(criteria.getSortDirection()))
            .append(", r.id ASC");

        try (var connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            setParameters(statement, parameters);

            try (ResultSet resultSet = statement.executeQuery()) {
                List<TransportationRequestView> result = new ArrayList<>();
                while (resultSet.next()) {
                    result.add(mapRequest(resultSet));
                }
                return result;
            }
        } catch (SQLException exception) {
            throw new DataAccessException("Не удалось выполнить поиск заявок", exception);
        }
    }

    @Override
    public List<RequestStatusStatistics> getStatisticsByStatus() {
        try (var connection = connectionFactory.openConnection();
             PreparedStatement statement = connection.prepareStatement(SELECT_STATISTICS);
             ResultSet resultSet = statement.executeQuery()) {
            List<RequestStatusStatistics> result = new ArrayList<>();
            while (resultSet.next()) {
                result.add(new RequestStatusStatistics(
                    TransportationRequestStatus.valueOf(resultSet.getString("status")),
                    resultSet.getLong("request_count"),
                    resultSet.getBigDecimal("total_price"),
                    resultSet.getBigDecimal("average_price"),
                    resultSet.getBigDecimal("total_weight_kg")
                ));
            }
            return result;
        } catch (SQLException exception) {
            throw new DataAccessException("Не удалось получить статистику заявок", exception);
        }
    }

    private void appendSearchText(
        StringBuilder sql,
        List<Object> parameters,
        String searchText
    ) {
        if (searchText == null || searchText.isBlank()) {
            return;
        }

        sql.append("""
             AND (
                r.cargo_description ILIKE ? ESCAPE '\\'
                OR r.pickup_address ILIKE ? ESCAPE '\\'
                OR r.delivery_address ILIKE ? ESCAPE '\\'
                OR c.full_name ILIKE ? ESCAPE '\\'
             )
            """);
        String pattern = "%" + escapeLikePattern(searchText.trim()) + "%";
        parameters.add(pattern);
        parameters.add(pattern);
        parameters.add(pattern);
        parameters.add(pattern);
    }

    private void appendEqual(
        StringBuilder sql,
        List<Object> parameters,
        String column,
        Object value
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" = ?");
            parameters.add(value);
        }
    }

    private void appendLowerBound(
        StringBuilder sql,
        List<Object> parameters,
        String column,
        Object value
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" >= ?");
            parameters.add(value);
        }
    }

    private void appendUpperBound(
        StringBuilder sql,
        List<Object> parameters,
        String column,
        Object value
    ) {
        if (value != null) {
            sql.append(" AND ").append(column).append(" <= ?");
            parameters.add(value);
        }
    }

    private void setParameters(
        PreparedStatement statement,
        List<Object> parameters
    ) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            statement.setObject(index + 1, parameters.get(index));
        }
    }

    private TransportationRequestView mapRequest(ResultSet resultSet) throws SQLException {
        long driverIdValue = resultSet.getLong("driver_id");
        Long driverId = resultSet.wasNull() ? null : driverIdValue;

        return new TransportationRequestView(
            resultSet.getLong("id"),
            resultSet.getLong("client_id"),
            resultSet.getString("client_name"),
            driverId,
            resultSet.getString("driver_name"),
            resultSet.getLong("operator_id"),
            resultSet.getString("operator_name"),
            resultSet.getString("cargo_description"),
            resultSet.getBigDecimal("weight_kg"),
            resultSet.getBigDecimal("price"),
            resultSet.getObject("created_at", java.time.LocalDateTime.class),
            resultSet.getObject("planned_delivery_at", java.time.LocalDateTime.class),
            TransportationRequestStatus.valueOf(resultSet.getString("status")),
            resultSet.getString("pickup_address"),
            resultSet.getString("delivery_address")
        );
    }

    private String statusName(TransportationRequestStatus status) {
        return status == null ? null : status.name();
    }

    private String toOrderByColumn(TransportationRequestSortField sortField) {
        return switch (sortField) {
            case ID -> "r.id";
            case CREATED_AT -> "r.created_at";
            case PLANNED_DELIVERY_AT -> "r.planned_delivery_at";
            case PRICE -> "r.price";
            case WEIGHT_KG -> "r.weight_kg";
        };
    }

    private String toOrderDirection(SortDirection direction) {
        return switch (direction) {
            case ASC -> "ASC";
            case DESC -> "DESC";
        };
    }

    private String escapeLikePattern(String value) {
        return value
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
