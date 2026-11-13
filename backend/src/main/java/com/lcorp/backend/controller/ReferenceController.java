package com.lcorp.backend.controller;

import com.lcorp.backend.repository.ClientRepository;
import com.lcorp.backend.repository.DriverRepository;
import com.lcorp.backend.repository.OperatorRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReferenceController {
    private final ClientRepository clients;
    private final OperatorRepository operators;
    private final DriverRepository drivers;

    public ReferenceController(ClientRepository clients, OperatorRepository operators, DriverRepository drivers) {
        this.clients = clients;
        this.operators = operators;
        this.drivers = drivers;
    }

    @GetMapping("/api/clients")
    public List<ClientView> clients() {
        return clients.findAll(Sort.by("fullName")).stream()
            .map(c -> new ClientView(c.getId(), c.getFullName(), c.isActive())).toList();
    }

    @GetMapping("/api/operators")
    public List<PersonView> operators() {
        return operators.findAll(Sort.by("fullName")).stream()
            .map(o -> new PersonView(o.getId(), o.getFullName())).toList();
    }

    @GetMapping("/api/drivers")
    public List<DriverView> drivers() {
        return drivers.findAll(Sort.by("fullName")).stream()
            .map(d -> new DriverView(d.getId(), d.getFullName(), d.isActive())).toList();
    }

    public record ClientView(long id, String fullName, boolean active) {
    }

    public record PersonView(long id, String fullName) {
    }

    public record DriverView(long id, String fullName, boolean active) {
    }
}
