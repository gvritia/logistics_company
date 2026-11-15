package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.Driver;
import com.lcorp.backend.domain.Operator;
import com.lcorp.backend.dto.ReferenceDtos;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.DriverRepository;
import com.lcorp.backend.repository.OperatorRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class ReferenceAdminService {
    private final OperatorRepository operators;
    private final DriverRepository drivers;
    private final UserAccountRepository users;
    private final AuditEventRepository audit;

    public ReferenceAdminService(OperatorRepository operators, DriverRepository drivers,
                                 UserAccountRepository users, AuditEventRepository audit) {
        this.operators = operators;
        this.drivers = drivers;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public ReferenceDtos.Person createOperator(ReferenceDtos.CreatePerson body, UUID actorId,
                                                String correlationId) {
        Operator operator = operators.saveAndFlush(new Operator(body.fullName().trim(), phone(body.phone())));
        record(actorId, "OPERATOR_CREATED", operator.getId(), correlationId);
        return new ReferenceDtos.Person(operator.getId(), operator.getFullName());
    }

    @Transactional
    public ReferenceDtos.Driver createDriver(ReferenceDtos.CreatePerson body, UUID actorId,
                                             String correlationId) {
        Driver driver = drivers.saveAndFlush(new Driver(body.fullName().trim(), phone(body.phone())));
        record(actorId, "DRIVER_CREATED", driver.getId(), correlationId);
        return new ReferenceDtos.Driver(driver.getId(), driver.getFullName(), driver.isActive());
    }

    private static String phone(String value) {
        String phone = value.trim();
        if (!phone.matches("\\+?[0-9() -]+") || phone.chars().filter(Character::isDigit).count() < 7) {
            throw new IllegalArgumentException("Некорректный номер телефона");
        }
        return phone;
    }

    private void record(UUID actorId, String type, long personId, String correlationId) {
        audit.save(new AuditEvent(UUID.randomUUID(), users.getReferenceById(actorId), type,
            type.startsWith("DRIVER") ? "DRIVER" : "OPERATOR", null,
            Instant.now(), "Сотрудник #" + personId, correlationId));
    }
}
