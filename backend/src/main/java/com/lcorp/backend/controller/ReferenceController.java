package com.lcorp.backend.controller;

import com.lcorp.backend.repository.DriverRepository;
import com.lcorp.backend.repository.OperatorRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReferenceController {
    private final OperatorRepository operators;
    private final DriverRepository drivers;

    public ReferenceController(OperatorRepository operators, DriverRepository drivers) {
        this.operators = operators;
        this.drivers = drivers;
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

    public record PersonView(long id, String fullName) {
    }

    public record DriverView(long id, String fullName, boolean active) {
    }
}
