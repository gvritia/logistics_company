package com.lcorp.console.repository;

import com.lcorp.console.model.Driver;

import java.util.List;
import java.util.Optional;

public interface DriverRepository {
    Driver create(Driver driver);

    Optional<Driver> findById(Long id);

    List<Driver> findAll();

    boolean update(Driver driver);

    boolean deleteById(Long id);

    List<Driver> findActive();
}
