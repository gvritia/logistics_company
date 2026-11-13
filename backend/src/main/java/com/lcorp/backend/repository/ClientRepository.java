package com.lcorp.backend.repository;

import com.lcorp.backend.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Sort;

import java.util.List;

public interface ClientRepository extends JpaRepository<Client, Long> {
    List<Client> findByActive(boolean active, Sort sort);
}
