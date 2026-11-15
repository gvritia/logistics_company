package com.lcorp.backend.controller;

import com.lcorp.backend.dto.ClientDtos;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.ClientService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/clients")
public class ClientController {
    private final ClientService clients;

    public ClientController(ClientService clients) { this.clients = clients; }

    @GetMapping
    public List<ClientDtos.View> list(@RequestParam(required = false) Boolean active) {
        return clients.list(active);
    }

    @GetMapping("/{id}")
    public ClientDtos.View get(@PathVariable long id) { return clients.get(id); }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ClientDtos.View> create(@Valid @RequestBody ClientDtos.Create body,
                                                  @AuthenticationPrincipal AuthPrincipal actor,
                                                  HttpServletRequest request) {
        ClientDtos.View client = clients.create(body, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.created(URI.create("/api/clients/" + client.id())).body(client);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientDtos.View update(@PathVariable long id, @Valid @RequestBody ClientDtos.Update body,
                                  @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return clients.update(id, body, actor.id(), CorrelationIdFilter.get(request));
    }

    @PostMapping("/{id}/activate")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientDtos.View activate(@PathVariable long id, @RequestParam long version,
                                    @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return clients.setActive(id, version, true, actor.id(), CorrelationIdFilter.get(request));
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('ADMIN')")
    public ClientDtos.View deactivate(@PathVariable long id, @RequestParam long version,
                                      @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return clients.setActive(id, version, false, actor.id(), CorrelationIdFilter.get(request));
    }
}
