package com.lcorp.backend.controller;

import com.lcorp.backend.dto.UserAdminDtos;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.UserAdminService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
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
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserAdminController {
    private final UserAdminService users;

    public UserAdminController(UserAdminService users) {
        this.users = users;
    }

    @GetMapping
    public UserAdminDtos.Page list(@RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "20") int size,
                                   @RequestParam(defaultValue = "email,asc") String sort) {
        return users.list(page, size, sort);
    }

    @GetMapping("/{id}")
    public UserAdminDtos.User get(@PathVariable UUID id) {
        return users.get(id);
    }

    @PostMapping
    public ResponseEntity<UserAdminDtos.TemporaryPassword> create(@Valid @RequestBody UserAdminDtos.Create body,
                                                                    @AuthenticationPrincipal AuthPrincipal actor,
                                                                    HttpServletRequest request) {
        var created = users.create(body, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.created(URI.create("/api/admin/users/" + created.user().id()))
            .header(HttpHeaders.CACHE_CONTROL, "no-store").body(created);
    }

    @PutMapping("/{id}")
    public UserAdminDtos.User update(@PathVariable UUID id, @Valid @RequestBody UserAdminDtos.Update body,
                                     @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return users.update(id, body, actor.id(), CorrelationIdFilter.get(request));
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<UserAdminDtos.TemporaryPassword> resetPassword(
        @PathVariable UUID id, @Valid @RequestBody UserAdminDtos.ResetPassword body,
        @AuthenticationPrincipal AuthPrincipal actor, HttpServletRequest request) {
        return ResponseEntity.ok().header(HttpHeaders.CACHE_CONTROL, "no-store")
            .body(users.resetPassword(id, body, actor.id(), CorrelationIdFilter.get(request)));
    }
}
