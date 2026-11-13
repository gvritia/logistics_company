package com.lcorp.backend.repository;

import com.lcorp.backend.domain.TransportationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface TransportationRequestRepository extends JpaRepository<TransportationRequest, Long> {
    @EntityGraph(attributePaths = {"client", "operator", "driver"})
    Optional<TransportationRequest> findByPublicId(UUID publicId);

    @EntityGraph(attributePaths = {"client", "operator", "driver"})
    Page<TransportationRequest> findByArchived(boolean archived, Pageable pageable);

    boolean existsByRequestCodeIgnoreCase(String requestCode);
}
