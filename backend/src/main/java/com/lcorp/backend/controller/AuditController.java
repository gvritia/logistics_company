package com.lcorp.backend.controller;

import com.lcorp.backend.dto.AuditDtos;
import com.lcorp.backend.filter.AuditSearch;
import com.lcorp.backend.service.AuditReadService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit")
@PreAuthorize("hasRole('ADMIN')")
public class AuditController {
    private final AuditReadService audit;

    public AuditController(AuditReadService audit) {
        this.audit = audit;
    }

    @GetMapping
    public AuditDtos.Page list(@RequestParam(required = false) String q,
                               @RequestParam(defaultValue = "0") int page,
                               @RequestParam(defaultValue = "20") int size,
                               @RequestParam(defaultValue = "occurredAt,desc") String sort) {
        return audit.list(new AuditSearch(q, page, size, sort));
    }
}
