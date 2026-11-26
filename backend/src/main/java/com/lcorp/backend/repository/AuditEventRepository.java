package com.lcorp.backend.repository;

import com.lcorp.backend.domain.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.UUID;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID>, JpaSpecificationExecutor<AuditEvent> {
    boolean existsByUser_IdAndEventType(UUID userId, String eventType);

    @EntityGraph(attributePaths = "user")
    Page<AuditEvent> findAll(Specification<AuditEvent> specification, Pageable pageable);
}
