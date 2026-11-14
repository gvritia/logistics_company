package com.lcorp.backend.repository;

import com.lcorp.backend.domain.TransportationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.domain.Specification;

public interface TransportationRequestRepository extends JpaRepository<TransportationRequest, Long>,
    JpaSpecificationExecutor<TransportationRequest> {
    @EntityGraph(attributePaths = {"client", "operator", "driver"})
    Optional<TransportationRequest> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"client", "operator", "driver"})
    Page<TransportationRequest> findAll(Specification<TransportationRequest> specification, Pageable pageable);

    boolean existsByRequestCodeIgnoreCase(String requestCode);
    Optional<TransportationRequest> findByRequestCodeIgnoreCase(String requestCode);
}
