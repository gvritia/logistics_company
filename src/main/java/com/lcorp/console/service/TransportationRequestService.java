package com.lcorp.console.service;

import com.lcorp.console.exception.*;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.repository.ClientRepository;
import com.lcorp.console.repository.DriverRepository;
import com.lcorp.console.repository.OperatorRepository;
import com.lcorp.console.repository.TransportationRequestRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

// Реализация бизнес-правила для работы с заявками на перевозку
public class TransportationRequestService {

    private final TransportationRequestRepository requestRepository;
    private final ClientRepository clientRepository;
    private final OperatorRepository operatorRepository;
    private final DriverRepository driverRepository;

    // Зависимости передаются через конструктор
    public TransportationRequestService(
        TransportationRequestRepository requestRepository,
        ClientRepository clientRepository,
        OperatorRepository operatorRepository,
        DriverRepository driverRepository
    ) {
        this.requestRepository = Objects.requireNonNull(requestRepository);
        this.clientRepository = Objects.requireNonNull(clientRepository);
        this.operatorRepository = Objects.requireNonNull(operatorRepository);
        this.driverRepository = Objects.requireNonNull(driverRepository);
    }

    // Проверяет новую заявку и сохраняет её через Repository
    public TransportationRequest createRequest(TransportationRequest request) {
        validateNewRequest(request);

        requireClient(request.getClientId());
        requireOperator(request.getOperatorId());

        return requestRepository.create(request);
    }

    // Изменять содержимое можно только у новой заявки, пока её не начали обрабатывать.
    // Служебные поля берутся из сохранённой заявки, а не из объекта, полученного от меню.
    public TransportationRequest updateRequest(TransportationRequest request) {
        if (request == null) {
            throw new InvalidRequestDataException("request", "заявка не должна быть null");
        }
        requireId(request.getId(), "id");

        TransportationRequest existingRequest = requireRequest(request.getId());
        ensureDraftRequest(existingRequest, "Редактирование");
        validateRequestDetails(request, existingRequest.getCreatedAt());

        requireClient(request.getClientId());
        requireOperator(request.getOperatorId());

        existingRequest.setClientId(request.getClientId());
        existingRequest.setOperatorId(request.getOperatorId());
        existingRequest.setCargoDescription(request.getCargoDescription());
        existingRequest.setWeightKg(request.getWeightKg());
        existingRequest.setPrice(request.getPrice());
        existingRequest.setPlannedDeliveryAt(request.getPlannedDeliveryAt());
        existingRequest.setPickupAddress(request.getPickupAddress());
        existingRequest.setDeliveryAddress(request.getDeliveryAddress());

        updateExisting(existingRequest);
        return existingRequest;
    }

    // Физически удалять можно только черновик CREATED.
    // Для заявок, попавших в обработку, используется смена статуса на CANCELLED.
    public void deleteRequest(Long requestId) {
        TransportationRequest request = requireRequest(requestId);
        ensureDraftRequest(request, "Удаление");

        if (!requestRepository.deleteById(requestId)) {
            throw new EntityNotFoundException("Заявка", requestId);
        }
    }

    public TransportationRequest getById(Long requestId) {
        return requireRequest(requestId);
    }

    public List<TransportationRequest> findAll() {
        return requestRepository.findAll();
    }

    public List<TransportationRequest> findAvailable() {
        return requestRepository.findAvailable();
    }

    public List<TransportationRequest> findByDriverId(Long driverId) {
        requireDriver(driverId);
        return requestRepository.findByDriverId(driverId);
    }

    // Переводит созданную заявку в состояние APPROVED
    public TransportationRequest approve(Long requestId) {
        return changeStatus(requestId, TransportationRequestStatus.APPROVED);
    }

    // Назначает активного доставщика на свободную одобренную заявку
    // Метод может вызывать меню оператора
    public TransportationRequest assignDriver(Long requestId, Long driverId) {
        TransportationRequest request = requireRequest(requestId);
        Driver driver = requireActiveDriver(driverId);

        ensureApproved(request, driver.getId());
        if (request.getDriverId() != null) {
            throw new RequestAssignmentException(
                requestId,
                driverId,
                "на заявку уже назначен другой доставщик"
            );
        }

        request.setDriverId(driver.getId());
        updateExisting(request);
        return request;
    }

    // Доставщик самостоятельно берёт свободную заявку
    public TransportationRequest takeRequest(Long requestId, Long driverId) {
        return assignDriver(requestId, driverId);
    }


    // Меняет назначенного доставщика до начала перевозки
    // После перехода в IN_TRANSIT правило ensureApproved запретит операцию
    public TransportationRequest reassignDriver(Long requestId, Long newDriverId) {
        TransportationRequest request = requireRequest(requestId);
        Driver driver = requireActiveDriver(newDriverId);

        ensureApproved(request, driver.getId());
        request.setDriverId(driver.getId());
        updateExisting(request);
        return request;
    }

    // Снимает доставщика с заявки до начала перевозки
    // Заявка остаётся одобренной и снова появляется в списке свободных
    public TransportationRequest rejectAssignment(Long requestId, Long driverId) {
        TransportationRequest request = requireRequest(requestId);

        ensureApproved(request, driverId);
        ensureAssignedTo(request, driverId);

        request.setDriverId(null);
        updateExisting(request);
        return request;
    }

    // Начинает перевозку назначенным доставщиком
    public TransportationRequest startTransportation(Long requestId, Long driverId) {
        TransportationRequest request = requireRequest(requestId);
        ensureAssignedTo(request, driverId);

        changeStatus(request, TransportationRequestStatus.IN_TRANSIT);
        updateExisting(request);
        return request;
    }

    // Завершает перевозку назначенным доставщиком
    public TransportationRequest completeDelivery(Long requestId, Long driverId) {
        TransportationRequest request = requireRequest(requestId);
        ensureAssignedTo(request, driverId);

        changeStatus(request, TransportationRequestStatus.DELIVERED);
        updateExisting(request);
        return request;
    }

    // Отменяет только созданную или одобренную заявку
    public TransportationRequest cancel(Long requestId) {
        return changeStatus(requestId, TransportationRequestStatus.CANCELLED);
    }

    private TransportationRequest changeStatus(
        Long requestId,
        TransportationRequestStatus targetStatus
    ) {
        TransportationRequest request = requireRequest(requestId);
        changeStatus(request, targetStatus);
        updateExisting(request);
        return request;
    }

    // Проверяет таблицу разрешённых переходов статусов
    private void changeStatus(
        TransportationRequest request,
        TransportationRequestStatus targetStatus
    ) {
        TransportationRequestStatus currentStatus = request.getStatus();
        if (currentStatus == null) {
            throw new InvalidRequestDataException("status", "статус заявки обязателен");
        }

        boolean allowed = switch (currentStatus) {
            case CREATED -> targetStatus == TransportationRequestStatus.APPROVED
                || targetStatus == TransportationRequestStatus.CANCELLED;
            case APPROVED -> targetStatus == TransportationRequestStatus.IN_TRANSIT
                || targetStatus == TransportationRequestStatus.CANCELLED;
            case IN_TRANSIT -> targetStatus == TransportationRequestStatus.DELIVERED;
            case DELIVERED, CANCELLED -> false;
        };

        if (!allowed) {
            throw new InvalidStatusTransitionException(currentStatus, targetStatus);
        }

        request.setStatus(targetStatus);
    }

    private void validateNewRequest(TransportationRequest request) {
        if (request == null) {
            throw new InvalidRequestDataException("request", "заявка не должна быть null");
        }
        if (request.getId() != null) {
            throw new InvalidRequestDataException("id", "у новой заявки ID должен отсутствовать");
        }

        validateRequestDetails(request, request.getCreatedAt());

        if (request.getStatus() != TransportationRequestStatus.CREATED) {
            throw new InvalidRequestDataException(
                "status",
                "новая заявка должна иметь статус CREATED"
            );
        }
        if (request.getDriverId() != null) {
            throw new InvalidRequestDataException(
                "driverId",
                "новая заявка должна быть свободной"
            );
        }
    }

    private void validateRequestDetails(
        TransportationRequest request,
        LocalDateTime createdAt
    ) {
        requireId(request.getClientId(), "clientId");
        requireId(request.getOperatorId(), "operatorId");
        requireText(request.getCargoDescription(), "cargoDescription");
        requirePositive(request.getWeightKg(), "weightKg");
        requirePositive(request.getPrice(), "price");
        requireText(request.getPickupAddress(), "pickupAddress");
        requireText(request.getDeliveryAddress(), "deliveryAddress");

        if (request.getPickupAddress().trim()
            .equalsIgnoreCase(request.getDeliveryAddress().trim())) {
            throw new InvalidRequestDataException(
                "deliveryAddress",
                "адрес доставки должен отличаться от адреса отправления"
            );
        }
        if (createdAt == null) {
            throw new InvalidRequestDataException("createdAt", "дата создания обязательна");
        }
        if (request.getPlannedDeliveryAt() == null
            || !request.getPlannedDeliveryAt().isAfter(createdAt)) {
            throw new InvalidRequestDataException(
                "plannedDeliveryAt",
                "плановая доставка должна быть позже даты создания"
            );
        }
    }

    private void ensureDraftRequest(TransportationRequest request, String operationName) {
        if (request.getStatus() != TransportationRequestStatus.CREATED
            || request.getDriverId() != null) {
            throw new IllegalStateException(
                operationName + " разрешено только для заявки CREATED без доставщика"
            );
        }
    }

    private void ensureApproved(TransportationRequest request, Long driverId) {
        if (request.getStatus() != TransportationRequestStatus.APPROVED) {
            throw new RequestAssignmentException(
                request.getId(),
                driverId,
                "назначение разрешено только для заявки APPROVED"
            );
        }
    }

    private void ensureAssignedTo(TransportationRequest request, Long driverId) {
        requireId(driverId, "driverId");
        if (!Objects.equals(request.getDriverId(), driverId)) {
            throw new RequestAssignmentException(
                request.getId(),
                driverId,
                "заявка не назначена этому доставщику"
            );
        }
    }

    private TransportationRequest requireRequest(Long requestId) {
        requireId(requestId, "requestId");
        return requestRepository.findById(requestId)
            .orElseThrow(() -> new EntityNotFoundException("Заявка", requestId));
    }

    private void requireClient(Long clientId) {
        clientRepository.findById(clientId)
            .orElseThrow(() -> new EntityNotFoundException("Клиент", clientId));
    }

    private void requireOperator(Long operatorId) {
        operatorRepository.findById(operatorId)
            .orElseThrow(() -> new EntityNotFoundException("Оператор", operatorId));
    }

    private Driver requireDriver(Long driverId) {
        requireId(driverId, "driverId");
        return driverRepository.findById(driverId)
            .orElseThrow(() -> new EntityNotFoundException("Доставщик", driverId));
    }

    private Driver requireActiveDriver(Long driverId) {
        Driver driver = requireDriver(driverId);
        if (!driver.isActive()) {
            throw new DriverUnavailableException(driverId, "доставщик неактивен");
        }
        return driver;
    }

    private void updateExisting(TransportationRequest request) {
        if (!requestRepository.update(request)) {
            throw new EntityNotFoundException("Заявка", request.getId());
        }
    }

    private void requireId(Long id, String fieldName) {
        if (id == null || id <= 0) {
            throw new InvalidRequestDataException(
                fieldName,
                "ID должен быть положительным числом"
            );
        }
    }

    private void requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidRequestDataException(fieldName, "значение обязательно");
        }
    }

    private void requirePositive(BigDecimal value, String fieldName) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidRequestDataException(
                fieldName,
                "значение должно быть больше нуля"
            );
        }
    }
}
