package com.lcorp.backend.repository;

import com.lcorp.backend.domain.Client;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClientRepository extends JpaRepository<Client, Long> {
}
