package com.lcorp.backend.repository;

import com.lcorp.backend.domain.TransportationRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
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

    long countByArchivedFalse();
    long countByArchivedTrue();
    long countByArchivedFalseAndFragileTrue();
    long countByArchivedFalseAndWeightKgLessThan(BigDecimal maximum);
    long countByArchivedFalseAndWeightKgGreaterThanEqualAndWeightKgLessThan(BigDecimal minimum,
                                                                             BigDecimal maximum);
    long countByArchivedFalseAndWeightKgGreaterThanEqual(BigDecimal minimum);

    @Query("select sum(r.weightKg) from TransportationRequest r where r.archived = false")
    BigDecimal sumActiveWeightKg();

    @Query("select c.id, c.fullName, count(r) from TransportationRequest r join r.client c "
        + "where r.archived = false group by c.id, c.fullName "
        + "order by count(r) desc, c.fullName asc, c.id asc")
    List<Object[]> countActiveByClient();

    @Query("select r.status, count(r) from TransportationRequest r where r.archived = false "
        + "group by r.status")
    List<Object[]> countActiveByStatus();
}
