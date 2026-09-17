package com.lcorp.console.service;

import com.lcorp.console.exception.DriverUnavailableException;
import com.lcorp.console.exception.EntityNotFoundException;
import com.lcorp.console.model.Driver;
import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.repository.DriverRepository;

import java.util.List;
import java.util.Objects;

// Управляет данными доставщиков и их пользовательскими сценариями
public class DriverService {

    private final DriverRepository driverRepository;
    private final TransportationRequestService requestService;

    public DriverService(
        DriverRepository driverRepository,
        TransportationRequestService requestService
    ) {
        this.driverRepository = Objects.requireNonNull(driverRepository);
        this.requestService = Objects.requireNonNull(requestService);
    }

    // Проверяет и сохраняет нового активного доставщика
    public Driver createDriver(Driver driver) {
        validateNewDriver(driver);
        return driverRepository.create(driver);
    }

    public Driver getById(Long driverId) {
        return requireDriver(driverId);
    }

    public List<Driver> findAll() {
        return driverRepository.findAll();
    }

    public List<Driver> findActive() {
        return driverRepository.findActive();
    }

    // Обновляет ФИО, телефон и признак активности существующего доставщика
    public Driver updateDriver(Driver driver) {
        validateExistingDriver(driver);
        requireDriver(driver.getId());

        if (!driverRepository.update(driver)) {
            throw new EntityNotFoundException("Доставщик", driver.getId());
        }
        return driver;
    }

    // Делает доставщика доступным для новых назначений
    public Driver activate(Long driverId) {
        Driver driver = requireDriver(driverId);
        driver.setActive(true);
        updateExisting(driver);
        return driver;
    }

    // Запрещает новые назначения, сохраняя доставщика и историю его заявок
    public Driver deactivate(Long driverId) {
        Driver driver = requireDriver(driverId);
        driver.setActive(false);
        updateExisting(driver);
        return driver;
    }


    // Удаляет доставщика только при отсутствии связанных заявок
    public void deleteDriver(Long driverId) {
        Driver driver = requireDriver(driverId);
        if (!requestService.findByDriverId(driver.getId()).isEmpty()) {
            throw new DriverUnavailableException(
                driverId,
                "нельзя удалить доставщика, у которого есть заявки"
            );
        }
        if (!driverRepository.deleteById(driverId)) {
            throw new EntityNotFoundException("Доставщик", driverId);
        }
    }

    // Возвращает свободные заявки, которые активный доставщик может взять
    public List<TransportationRequest> findAvailableRequests(Long driverId) {
        requireActiveDriver(driverId);
        return requestService.findAvailable();
    }

    // Возвращает заявки, назначенные выбранному доставщику
    public List<TransportationRequest> findAssignedRequests(Long driverId) {
        requireDriver(driverId);
        return requestService.findByDriverId(driverId);
    }

    //Делегирует принятие свободной заявки TransportationRequestService
    public TransportationRequest takeRequest(Long driverId, Long requestId) {
        requireActiveDriver(driverId);
        return requestService.takeRequest(requestId, driverId);
    }

    // Делегирует отказ от назначения TransportationRequestService.
    public TransportationRequest rejectAssignment(Long driverId, Long requestId) {
        requireDriver(driverId);
        return requestService.rejectAssignment(requestId, driverId);
    }

    // Делегирует начало перевозки TransportationRequestService.
    public TransportationRequest startTransportation(Long driverId, Long requestId) {
        requireActiveDriver(driverId);
        return requestService.startTransportation(requestId, driverId);
    }

    // Делегирует завершение перевозки TransportationRequestService.
    public TransportationRequest completeDelivery(Long driverId, Long requestId) {
        // Деактивация запрещает новые назначения, но не мешает завершить
        // перевозку, которая уже была начата этим доставщиком.
        requireDriver(driverId);
        return requestService.completeDelivery(requestId, driverId);
    }

    private void validateNewDriver(Driver driver) {
        requireDriverObject(driver);
        if (driver.getId() != null) {
            throw new IllegalArgumentException("У нового доставщика ID должен отсутствовать");
        }
        validateDriverFields(driver);
        if (!driver.isActive()) {
            throw new IllegalArgumentException("Новый доставщик должен быть активным");
        }
    }

    private void validateExistingDriver(Driver driver) {
        requireDriverObject(driver);
        requirePositiveId(driver.getId());
        validateDriverFields(driver);
    }

    private void validateDriverFields(Driver driver) {
        if (driver.getFullName() == null || driver.getFullName().isBlank()) {
            throw new IllegalArgumentException("ФИО доставщика обязательно");
        }
        if (driver.getPhone() == null || driver.getPhone().isBlank()) {
            throw new IllegalArgumentException("Телефон доставщика обязателен");
        }
    }

    private Driver requireDriver(Long driverId) {
        requirePositiveId(driverId);
        return driverRepository.findById(driverId)
            .orElseThrow(() -> new EntityNotFoundException("Доставщик", driverId));
    }

    private void requireActiveDriver(Long driverId) {
        Driver driver = requireDriver(driverId);
        if (!driver.isActive()) {
            throw new DriverUnavailableException(driverId, "доставщик неактивен");
        }
        // return driver;
    }

    private void updateExisting(Driver driver) {
        if (!driverRepository.update(driver)) {
            throw new EntityNotFoundException("Доставщик", driver.getId());
        }
    }

    private void requireDriverObject(Driver driver) {
        if (driver == null) {
            throw new IllegalArgumentException("Доставщик не должен быть null");
        }
    }

    private void requirePositiveId(Long driverId) {
        if (driverId == null || driverId <= 0) {
            throw new IllegalArgumentException("ID доставщика должен быть положительным");
        }
    }
}
