package com.lcorp.backend.controller;

import com.lcorp.backend.dto.BulkDtos;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.BulkService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/requests/bulk")
public class BulkController {
    private final BulkService bulk;

    public BulkController(BulkService bulk) { this.bulk = bulk; }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public BulkDtos.Result apply(@Valid @RequestBody BulkDtos.Request body,
                                 @AuthenticationPrincipal AuthPrincipal actor,
                                 HttpServletRequest request) {
        return bulk.execute(body, actor.id(), CorrelationIdFilter.get(request));
    }
}
