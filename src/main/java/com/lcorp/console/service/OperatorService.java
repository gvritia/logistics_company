package com.lcorp.console.service;

import com.lcorp.console.exception.EntityNotFoundException;
import com.lcorp.console.model.Operator;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.repository.OperatorRepository;

import java.util.List;
import java.util.Objects;

// Управление данными операторов
public class OperatorService {

    private final OperatorRepository operatorRepository;
    private final TransportationRequestService requestService;

    public OperatorService(
            OperatorRepository operatorRepository,
            TransportationRequestService requestService
    ) {
        this.operatorRepository = Objects.requireNonNull(operatorRepository);
        this.requestService = Objects.requireNonNull(requestService);
    }

    public Operator createOperator(Operator operator) {
        validateNewOperator(operator);
        return operatorRepository.create(operator);
    }

    public Operator getById(Long operatorId) {
        return requireOperator(operatorId);
    }

    public List<Operator> findAll() {
        return operatorRepository.findAll();
    }

    public Operator updateOperator(Operator operator) {
        validateExistingOperator(operator);
        requireOperator(operator.getId());

        if (!operatorRepository.update(operator)) {
            throw new EntityNotFoundException("Оператор", operator.getId());
        }
        return operator;
    }

    public void deleteOperator(Long operatorId) {
        requireOperator(operatorId);

        // Удаление оператора со связанными заявками нарушило бы историю.
        boolean hasRequests = requestService.findAll().stream()
                .anyMatch(request -> Objects.equals(request.getOperatorId(), operatorId));
        if (hasRequests) {
            throw new IllegalStateException(
                    "Нельзя удалить оператора, у которого есть созданные заявки"
            );
        }

        if (!operatorRepository.deleteById(operatorId)) {
            throw new EntityNotFoundException("Оператор", operatorId);
        }
    }

    // Оператор создаёт заявку, но её валидация остаётся в сервисе заявок.
    public TransportationRequest createRequest(TransportationRequest request) {
        return requestService.createRequest(request);
    }

    public TransportationRequest updateRequest(TransportationRequest request) {
        return requestService.updateRequest(request);
    }

    public void deleteRequest(Long requestId) {
        requestService.deleteRequest(requestId);
    }

    public List<TransportationRequest> findAllRequests() {
        return requestService.findAll();
    }

    public List<TransportationRequest> findAvailableRequests() {
        return requestService.findAvailable();
    }

    public TransportationRequest approveRequest(Long requestId) {
        return requestService.approve(requestId);
    }

    public TransportationRequest assignDriver(Long requestId, Long driverId) {
        return requestService.assignDriver(requestId, driverId);
    }

    public TransportationRequest reassignDriver(Long requestId, Long newDriverId) {
        return requestService.reassignDriver(requestId, newDriverId);
    }

    public TransportationRequest cancelRequest(Long requestId) {
        return requestService.cancel(requestId);
    }

    private void validateNewOperator(Operator operator) {
        requireOperatorObject(operator);
        if (operator.getId() != null) {
            throw new IllegalArgumentException("У нового оператора ID должен отсутствовать");
        }
        validateOperatorFields(operator);
    }

    private void validateExistingOperator(Operator operator) {
        requireOperatorObject(operator);
        requirePositiveId(operator.getId());
        validateOperatorFields(operator);
    }

    private void validateOperatorFields(Operator operator) {
        if (operator.getFullName() == null || operator.getFullName().isBlank()) {
            throw new IllegalArgumentException("ФИО оператора обязательно");
        }
        if (operator.getPhone() == null || operator.getPhone().isBlank()) {
            throw new IllegalArgumentException("Номер телефона оператора обязателен");
        }
    }

    private Operator requireOperator(Long operatorId) {
        requirePositiveId(operatorId);
        return operatorRepository.findById(operatorId)
                .orElseThrow(() -> new EntityNotFoundException("Оператор", operatorId));
    }

    private void requireOperatorObject(Operator operator) {
        if (operator == null) {
            throw new IllegalArgumentException("Оператор не должен быть null");
        }
    }

    private void requirePositiveId(Long operatorId) {
        if (operatorId == null || operatorId <= 0) {
            throw new IllegalArgumentException("ID оператора должен быть положительным");
        }
    }
}
