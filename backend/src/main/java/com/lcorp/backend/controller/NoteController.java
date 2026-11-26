package com.lcorp.backend.controller;

import com.lcorp.backend.dto.NoteDtos;
import com.lcorp.backend.security.AuthPrincipal;
import com.lcorp.backend.service.NoteService;
import com.lcorp.backend.web.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
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
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/requests/{requestId}/notes")
public class NoteController {
    private final NoteService notes;

    public NoteController(NoteService notes) {
        this.notes = notes;
    }

    @GetMapping
    public List<NoteDtos.View> list(@PathVariable UUID requestId) {
        return notes.list(requestId);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<NoteDtos.View> create(@PathVariable UUID requestId,
                                                @Valid @RequestBody NoteDtos.Create body,
                                                @AuthenticationPrincipal AuthPrincipal actor,
                                                HttpServletRequest request) {
        NoteDtos.View note = notes.create(requestId, body, actor.id(), CorrelationIdFilter.get(request));
        return ResponseEntity.created(URI.create("/api/requests/" + requestId + "/notes/" + note.id())).body(note);
    }

    @PutMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public NoteDtos.View update(@PathVariable UUID requestId, @PathVariable UUID noteId,
                                @Valid @RequestBody NoteDtos.Update body,
                                @AuthenticationPrincipal AuthPrincipal actor, Authentication authentication,
                                HttpServletRequest request) {
        return notes.update(requestId, noteId, body, actor.id(), admin(authentication),
            CorrelationIdFilter.get(request));
    }

    @DeleteMapping("/{noteId}")
    @PreAuthorize("hasAnyRole('EDITOR','ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID requestId, @PathVariable UUID noteId,
                                       @RequestParam long version,
                                       @AuthenticationPrincipal AuthPrincipal actor,
                                       Authentication authentication, HttpServletRequest request) {
        notes.delete(requestId, noteId, version, actor.id(), admin(authentication),
            CorrelationIdFilter.get(request));
        return ResponseEntity.noContent().build();
    }

    private static boolean admin(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
    }
}
