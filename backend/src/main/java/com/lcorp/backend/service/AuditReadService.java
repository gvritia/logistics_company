package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.dto.AuditDtos;
import com.lcorp.backend.repository.AuditEventRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuditReadService {
    private final AuditEventRepository events;

    public AuditReadService(AuditEventRepository events) {
        this.events = events;
    }

    @Transactional(readOnly = true)
    public AuditDtos.Page list(AuditSearch search) {
        var result = events.findAll(search.specification(),
            PageRequest.of(search.page(), search.size(), search.safeSort()));
        return new AuditDtos.Page(result.stream().map(AuditReadService::view).toList(),
            search.page(), search.size(), result.getTotalElements(), result.getTotalPages());
    }

    private static AuditDtos.Entry view(AuditEvent event) {
        UserAccount user = event.getUser();
        return new AuditDtos.Entry(event.getId(), user == null ? null : user.getId(),
            user == null ? null : user.getEmail(), event.getEventType(), event.getEntityType(),
            event.getEntityId(), event.getOccurredAt(), event.getDescription(),
            event.getCorrelationId());
    }
}
