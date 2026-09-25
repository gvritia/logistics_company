package com.lcorp.console.repository.hibernate;

import com.lcorp.console.config.DatabaseConfig;
import com.lcorp.console.config.HibernateConfig;
import com.lcorp.console.model.*;
import org.hibernate.exception.ConstraintViolationException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

// Запускать вручную на локальной БД из .env. Проверка работает только со своими
// временными записями и удаляет их в finally; seed-данные не изменяются
public final class HibernateRepositoriesCheck {

    public static void main(String[] args) {
        try (var factory = HibernateConfig.createSessionFactory(DatabaseConfig.load())) {
            var clients = new HibernateClientRepository(factory);
            var drivers = new HibernateDriverRepository(factory);
            var operators = new HibernateOperatorRepository(factory);
            var requests = new HibernateTransportationRequestRepository(factory);

            String marker = "ORM check " + UUID.randomUUID();
            Client client = new Client(marker, null, "check@example.com");
            Client otherClient = new Client(marker + " other", null, "other@example.com");
            Driver driver = new Driver(marker, "+7-000-000-00-01");
            Driver otherDriver = new Driver(marker + " other", "+7-000-000-00-02");
            Operator operator = new Operator(marker, "+7-000-000-00-03");
            Operator otherOperator = new Operator(marker + " other", "+7-000-000-00-04");
            TransportationRequest request = null;

            try {
                clients.create(client);
                clients.create(otherClient);
                drivers.create(driver);
                drivers.create(otherDriver);
                operators.create(operator);
                operators.create(otherOperator);
                require(drivers.findById(driver.getId()).orElseThrow().getPhone().equals(driver.getPhone()),
                    "Driver create/findById");
                require(operators.findById(operator.getId()).orElseThrow().getPhone().equals(operator.getPhone()),
                    "Operator create/findById");
                require(drivers.findAll().stream().anyMatch(row -> row.getId().equals(driver.getId())),
                    "Driver findAll");
                require(operators.findAll().stream().anyMatch(row -> row.getId().equals(operator.getId())),
                    "Operator findAll");

                driver.setFullName(marker + " updated");
                driver.setPhone("+7-000-000-00-05");
                driver.setActive(false);
                require(drivers.update(driver), "Driver update");
                Driver savedDriver = drivers.findById(driver.getId()).orElseThrow();
                require(savedDriver.getFullName().equals(driver.getFullName())
                    && savedDriver.getPhone().equals(driver.getPhone()) && !savedDriver.isActive(),
                    "All driver fields must be updated");
                require(drivers.findActive().stream().noneMatch(row -> row.getId().equals(driver.getId())),
                    "Inactive driver must be excluded");
                require(drivers.findActive().stream().anyMatch(row -> row.getId().equals(otherDriver.getId())),
                    "Active driver must be included");
                driver.setFullName(" ");
                expectConstraint(() -> drivers.update(driver), "23514");
                require(drivers.findById(driver.getId()).orElseThrow().getFullName().equals(savedDriver.getFullName()),
                    "Driver update must roll back on CHECK failure");

                operator.setFullName(marker + " updated");
                operator.setPhone("+7-000-000-00-06");
                require(operators.update(operator), "Operator update");
                Operator savedOperator = operators.findById(operator.getId()).orElseThrow();
                require(savedOperator.getFullName().equals(operator.getFullName())
                    && savedOperator.getPhone().equals(operator.getPhone()), "All operator fields must be updated");
                operator.setPhone(" ");
                expectConstraint(() -> operators.update(operator), "23514");
                require(operators.findById(operator.getId()).orElseThrow().getPhone().equals(savedOperator.getPhone()),
                    "Operator update must roll back on CHECK failure");

                // Фиксируем секунды, чтобы сравнение не зависело от округления наносекунд PostgreSQL
                LocalDateTime createdAt = LocalDateTime.now().withNano(0);
                request = new TransportationRequest(null, client.getId(), null, operator.getId(),
                    marker, new BigDecimal("10.125"), new BigDecimal("100.25"), createdAt,
                    createdAt.plusDays(1), TransportationRequestStatus.CREATED, "Pickup A", "Delivery B");
                requests.create(request);
                Long requestId = request.getId();
                assertSameRequest(request, requests.findById(requestId).orElseThrow());
                require(requests.findAll().stream().anyMatch(row -> row.getId().equals(requestId)),
                    "Request findAll");
                require(requests.findAvailable().stream().noneMatch(row -> row.getId().equals(requestId)),
                    "CREATED without driver must not be available");

                // Меняем каждое поле, чтобы обнаружить пропуски setter в update
                request.setClientId(otherClient.getId());
                request.setOperatorId(otherOperator.getId());
                request.setDriverId(otherDriver.getId());
                request.setCargoDescription(marker + " updated");
                request.setWeightKg(new BigDecimal("20.875"));
                request.setPrice(new BigDecimal("220.75"));
                request.setCreatedAt(createdAt.plusHours(1));
                request.setPlannedDeliveryAt(createdAt.plusDays(2));
                request.setStatus(TransportationRequestStatus.APPROVED);
                request.setPickupAddress("Pickup C");
                request.setDeliveryAddress("Delivery D");
                require(requests.update(request), "Request update");
                assertSameRequest(request, requests.findById(requestId).orElseThrow());
                require(requests.findAvailable().stream().noneMatch(row -> row.getId().equals(requestId)),
                    "Assigned APPROVED must not be available");
                require(requests.findByDriverId(otherDriver.getId()).stream()
                    .anyMatch(row -> row.getId().equals(requestId)), "Assigned driver lookup");
                require(requests.findByDriverId(driver.getId()).isEmpty(), "Different driver must not see request");

                expectConstraint(() -> drivers.deleteById(otherDriver.getId()), "23503");
                expectConstraint(() -> operators.deleteById(otherOperator.getId()), "23503");
                require(drivers.findById(otherDriver.getId()).isPresent()
                    && operators.findById(otherOperator.getId()).isPresent(), "FK must preserve related records");

                TransportationRequest invalid = requests.findById(requestId).orElseThrow();
                invalid.setWeightKg(BigDecimal.ZERO);
                invalid.setCargoDescription("This change must roll back too");
                expectConstraint(() -> requests.update(invalid), "23514");
                assertSameRequest(request, requests.findById(requestId).orElseThrow());

                request.setDriverId(null);
                require(requests.update(request), "Clear driver assignment");
                require(requests.findAvailable().stream().anyMatch(row -> row.getId().equals(requestId)),
                    "Free APPROVED must be available");
                require(requests.findByDriverId(otherDriver.getId()).isEmpty(), "Cleared assignment must disappear");

                // История водителя должна включать завершённые и отменённые заявки
                for (var status : new TransportationRequestStatus[] {
                    TransportationRequestStatus.IN_TRANSIT, TransportationRequestStatus.DELIVERED,
                    TransportationRequestStatus.CANCELLED
                }) {
                    request.setStatus(status);
                    request.setDriverId(otherDriver.getId());
                    require(requests.update(request), "Update stored status");
                    require(requests.findByDriverId(otherDriver.getId()).stream()
                        .anyMatch(row -> row.getId().equals(requestId)), "Driver lookup must include history");
                    require(requests.findAvailable().stream().noneMatch(row -> row.getId().equals(requestId)),
                        "Non-APPROVED must not be available");
                }
                request.setDriverId(null);
                require(requests.update(request), "CANCELLED without driver");
                require(requests.findAvailable().stream().noneMatch(row -> row.getId().equals(requestId)),
                    "CANCELLED without driver must not be available");

                require(requests.deleteById(requestId), "Request delete");
                require(requests.findById(requestId).isEmpty() && !requests.deleteById(requestId)
                    && !requests.update(request) && requests.findById(requestId).isEmpty(), "Missing request ID");
                require(drivers.deleteById(driver.getId()), "Driver delete");
                require(drivers.findById(driver.getId()).isEmpty() && !drivers.deleteById(driver.getId())
                    && !drivers.update(driver) && drivers.findById(driver.getId()).isEmpty(), "Missing driver ID");
                require(operators.deleteById(operator.getId()), "Operator delete");
                require(operators.findById(operator.getId()).isEmpty() && !operators.deleteById(operator.getId())
                    && !operators.update(operator) && operators.findById(operator.getId()).isEmpty(), "Missing operator ID");
            } finally {
                // Сначала удаляем свою заявку, затем связанные с ней временные записи
                if (request != null && request.getId() != null) requests.deleteById(request.getId());
                if (driver.getId() != null) drivers.deleteById(driver.getId());
                if (otherDriver.getId() != null) drivers.deleteById(otherDriver.getId());
                if (operator.getId() != null) operators.deleteById(operator.getId());
                if (otherOperator.getId() != null) operators.deleteById(otherOperator.getId());
                if (client.getId() != null) clients.deleteById(client.getId());
                if (otherClient.getId() != null) clients.deleteById(otherClient.getId());
            }
        }
        System.out.println("PASS: repositories CRUD, all fields, selections, rollback, FK protection, missing IDs");
    }

    private static void assertSameRequest(TransportationRequest expected, TransportationRequest actual) {
        require(Objects.equals(expected.getId(), actual.getId())
            && Objects.equals(expected.getClientId(), actual.getClientId())
            && Objects.equals(expected.getOperatorId(), actual.getOperatorId())
            && Objects.equals(expected.getDriverId(), actual.getDriverId())
            && expected.getCargoDescription().equals(actual.getCargoDescription())
            && expected.getWeightKg().compareTo(actual.getWeightKg()) == 0
            && expected.getPrice().compareTo(actual.getPrice()) == 0
            && expected.getCreatedAt().equals(actual.getCreatedAt())
            && expected.getPlannedDeliveryAt().equals(actual.getPlannedDeliveryAt())
            && expected.getStatus() == actual.getStatus()
            && expected.getPickupAddress().equals(actual.getPickupAddress())
            && expected.getDeliveryAddress().equals(actual.getDeliveryAddress()), "Request field mapping");
    }

    private static void expectConstraint(Runnable action, String sqlState) {
        try {
            action.run();
            throw new AssertionError("Expected constraint violation " + sqlState);
        } catch (ConstraintViolationException expected) {
            require(sqlState.equals(expected.getSQLState()), "Unexpected SQLState");
        }
    }

    private static void require(boolean condition, String message) {
        if (!condition) throw new AssertionError(message);
    }
}
