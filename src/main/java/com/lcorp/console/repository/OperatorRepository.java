package com.lcorp.console.repository;

import com.lcorp.console.model.Operator;

import java.util.List;
import java.util.Optional;


public interface OperatorRepository {
    Operator create(Operator operator);

    Optional<Operator> findById(Long id);

    List<Operator> findAll();

    boolean update(Operator operator);

    boolean deleteById(Long id);
}
