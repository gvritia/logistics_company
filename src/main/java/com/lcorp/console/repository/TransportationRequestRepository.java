package com.lcorp.console.repository;

import com.lcorp.console.model.TransportationRequest;

import java.util.List;
import java.util.Optional;

public interface TransportationRequestRepository {

    TransportationRequest create(TransportationRequest request);

    Optional<TransportationRequest> findById(Long id);

    List<TransportationRequest> findAll();

    boolean update(TransportationRequest request);

    boolean deleteById(Long id);

    List<TransportationRequest> findAvailable();

    List<TransportationRequest> findByDriverId(Long driverId);
}
