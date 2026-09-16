package com.lcorp.console.repository;

import com.lcorp.console.model.Client;

import java.util.List;
import java.util.Optional;

public interface ClientRepository {
    Client create(Client client);

    Optional<Client> findById(Long id);

    List<Client> findAll();

    boolean update(Client client);

    boolean deleteById(Long id);
}
