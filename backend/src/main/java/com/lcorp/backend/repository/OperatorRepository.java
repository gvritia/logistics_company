package com.lcorp.backend.repository;

import com.lcorp.backend.domain.Operator;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OperatorRepository extends JpaRepository<Operator, Long> {
}
