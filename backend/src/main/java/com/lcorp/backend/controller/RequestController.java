package com.lcorp.backend.controller;

import com.lcorp.backend.dto.RequestDtos;
import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.RequestSearch;
import com.lcorp.backend.service.RequestService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests")
public class RequestController {
    private final RequestService requests;

    public RequestController(RequestService requests) { this.requests = requests; }

    @GetMapping
    public RequestDtos.PageResponse list(@RequestParam(required = false) String q,
                                         @RequestParam(required = false) Long clientId,
                                         @RequestParam(required = false) TransportationRequestStatus status,
                                         @RequestParam(required = false) Boolean fragile,
                                         @RequestParam(required = false) WeightCategory weightCategory,
                                         @RequestParam(required = false) String tag,
                                         @RequestParam(defaultValue = "false") boolean archived,
                                         @RequestParam(required = false) String weightOp,
                                         @RequestParam(required = false) BigDecimal weightValue,
                                         @RequestParam(defaultValue = "0") int page,
                                         @RequestParam(defaultValue = "20") int size,
                                         @RequestParam(defaultValue = "createdAt,desc") String sort) {
        return requests.list(new RequestSearch(q, clientId, status, fragile, weightCategory,
            tag, archived, weightOp, weightValue, page, size, sort));
    }

    @GetMapping("/{id}")
    public RequestDtos.Item get(@PathVariable UUID id) { return requests.get(id); }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<RequestDtos.Item> create(@Valid @RequestBody RequestDtos.Create body,
                                                   @AuthenticationPrincipal AuthPrincipal actor,
                                                   HttpServletRequest request) {
        RequestDtos.Item item = requests.create(body, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.created(URI.create("/api/requests/" + item.id())).body(item);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public RequestDtos.Item update(@PathVariable UUID id, @Valid @RequestBody RequestDtos.Update body,
                                   @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return requests.update(id, body, actor.id(), CorrelationIdFilter.get(request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public RequestDtos.Item archive(@PathVariable UUID id, @RequestParam long version,
                                    @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return requests.archive(id, version, actor.id(), CorrelationIdFilter.get(request));
    }

    @PostMapping("/{id}/restore")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public RequestDtos.Item restore(@PathVariable UUID id, @RequestParam long version,
                                    @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return requests.restore(id, version, actor.id(), CorrelationIdFilter.get(request));
    }

    @DeleteMapping("/{id}/permanent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> permanentDelete(@PathVariable UUID id, @RequestParam long version,
                                                @AuthenticationPrincipal AuthPrincipal actor,
                                                HttpServletRequest request) {
        requests.permanentDelete(id, version, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.noContent().build();
    }
}
