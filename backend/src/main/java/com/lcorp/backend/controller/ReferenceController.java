package com.lcorp.backend.controller;

import com.lcorp.backend.repository.DriverRepository;
import com.lcorp.backend.repository.OperatorRepository;
import com.lcorp.backend.dto.ReferenceDtos;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.ReferenceAdminService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class ReferenceController {

    private final OperatorRepository operators;
    private final DriverRepository drivers;
    private final ReferenceAdminService admin;

    public ReferenceController(OperatorRepository operators, DriverRepository drivers,
                               ReferenceAdminService admin) {
        this.operators = operators;
        this.drivers = drivers;
        this.admin = admin;
    }

    @GetMapping("/api/operators")
    public List<ReferenceDtos.Person> operators() {
        return operators.findAll(Sort.by("fullName")).stream()
            .map(o -> new ReferenceDtos.Person(o.getId(), o.getFullName())).toList();
    }

    @GetMapping("/api/drivers")
    public List<ReferenceDtos.Driver> drivers() {
        return drivers.findAll(Sort.by("fullName")).stream()
            .map(d -> new ReferenceDtos.Driver(d.getId(), d.getFullName(), d.isActive())).toList();
    }

    @PostMapping("/api/operators")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceDtos.Person> createOperator(@Valid @RequestBody ReferenceDtos.CreatePerson body,
                                                               @AuthenticationPrincipal AuthPrincipal actor,
                                                               HttpServletRequest request) {
        var created = admin.createOperator(body, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PostMapping("/api/drivers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReferenceDtos.Driver> createDriver(@Valid @RequestBody ReferenceDtos.CreatePerson body,
                                                             @AuthenticationPrincipal AuthPrincipal actor,
                                                             HttpServletRequest request) {
        var created = admin.createDriver(body, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }
}
