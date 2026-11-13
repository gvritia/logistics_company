package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.TransportationRequest;
import com.lcorp.backend.domain.TransportationRequestNote;
import com.lcorp.backend.domain.UserAccount;
import com.lcorp.backend.dto.NoteDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.error.NotFoundFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.TransportationRequestNoteRepository;
import com.lcorp.backend.repository.TransportationRequestRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class NoteService {
    private final TransportationRequestRepository requests;
    private final TransportationRequestNoteRepository notes;
    private final UserAccountRepository users;
    private final AuditEventRepository audit;

    public NoteService(TransportationRequestRepository requests, TransportationRequestNoteRepository notes,
                       UserAccountRepository users, AuditEventRepository audit) {
        this.requests = requests;
        this.notes = notes;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<NoteDtos.View> list(UUID requestId) {
        TransportationRequest request = request(requestId);
        return notes.findByRequest_IdOrderByPinnedDescCreatedAtDesc(request.getId()).stream()
            .map(NoteService::view).toList();
    }

    @Transactional
    public NoteDtos.View create(UUID requestId, NoteDtos.Create body, UUID actorId, String correlationId) {
        TransportationRequest request = editable(requestId);
        UserAccount author = users.findById(actorId).orElseThrow(() -> new NotFoundFailure("Автор не найден"));
        TransportationRequestNote note = notes.saveAndFlush(new TransportationRequestNote(
            UUID.randomUUID(), request, author, body.body().trim(), body.pinned()));
        record(author, "NOTE_CREATED", note.getId(), correlationId);
        return view(note);
    }

    @Transactional
    public NoteDtos.View update(UUID requestId, UUID noteId, NoteDtos.Update body,
                                UUID actorId, boolean admin, String correlationId) {
        TransportationRequest request = editable(requestId);
        TransportationRequestNote note = note(noteId, request.getId());
        authorize(note, actorId, admin);
        checkVersion(note, body.version());
        note.setBody(body.body().trim());
        note.setPinned(body.pinned());
        notes.flush();
        record(users.getReferenceById(actorId), "NOTE_UPDATED", noteId, correlationId);
        return view(note);
    }

    @Transactional
    public void delete(UUID requestId, UUID noteId, long version, UUID actorId,
                       boolean admin, String correlationId) {
        TransportationRequest request = editable(requestId);
        TransportationRequestNote note = note(noteId, request.getId());
        authorize(note, actorId, admin);
        checkVersion(note, version);
        notes.delete(note);
        notes.flush();
        record(users.getReferenceById(actorId), "NOTE_DELETED", noteId, correlationId);
    }

    private TransportationRequest request(UUID id) {
        return requests.findByPublicId(id).orElseThrow(() -> new NotFoundFailure("Заявка не найдена"));
    }

    private TransportationRequest editable(UUID id) {
        TransportationRequest request = request(id);
        if (request.isArchived()) throw new ConflictFailure("Архивную заявку сначала восстановите");
        return request;
    }

    private TransportationRequestNote note(UUID id, Long requestId) {
        return notes.findByIdAndRequest_Id(id, requestId)
            .orElseThrow(() -> new NotFoundFailure("Заметка не найдена"));
    }

    private static void authorize(TransportationRequestNote note, UUID actorId, boolean admin) {
        if (!admin && !note.getAuthor().getId().equals(actorId)) {
            throw new AccessDeniedException("Редактировать заметку может её автор или администратор");
        }
    }

    private static void checkVersion(TransportationRequestNote note, long expected) {
        if (expected < 0 || note.getVersion() != expected) {
            throw new ConflictFailure("Версия заметки устарела; обновите данные");
        }
    }

    private void record(UserAccount actor, String type, UUID noteId, String correlationId) {
        audit.save(new AuditEvent(UUID.randomUUID(), actor, type,
            "TRANSPORTATION_REQUEST_NOTE", noteId, Instant.now(), type, correlationId));
    }

    private static NoteDtos.View view(TransportationRequestNote note) {
        return new NoteDtos.View(note.getId(), note.getRequest().getPublicId(),
            note.getAuthor().getId(), note.getAuthor().getDisplayName(), note.getBody(),
            note.isPinned(), note.getCreatedAt(), note.getUpdatedAt(), note.getVersion());
    }
}
