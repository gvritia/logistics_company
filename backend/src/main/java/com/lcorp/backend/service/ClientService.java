package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.Client;
import com.lcorp.backend.dto.ClientDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.error.NotFoundFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.ClientRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
public class ClientService {

    private final ClientRepository clients;
    private final UserAccountRepository users;
    private final AuditEventRepository audit;

    public ClientService(ClientRepository clients, UserAccountRepository users, AuditEventRepository audit) {
        this.clients = clients;
        this.users = users;
        this.audit = audit;
    }

    @Transactional(readOnly = true)
    public List<ClientDtos.View> list(Boolean active) {
        Sort sort = Sort.by("fullName").ascending().and(Sort.by("id"));
        List<Client> result = active == null ? clients.findAll(sort) : clients.findByActive(active, sort);
        return result.stream().map(ClientService::view).toList();
    }

    @Transactional(readOnly = true)
    public ClientDtos.View get(long id) {
        return view(find(id));
    }

    @Transactional
    public ClientDtos.View create(ClientDtos.Create body, UUID actorId, String correlationId) {
        Contact contact = contact(body.phone(), body.email());
        Client client = clients.saveAndFlush(new Client(body.fullName().trim(), contact.phone(), contact.email()));
        record(actorId, "CLIENT_CREATED", client.getId(), correlationId);
        return view(client);
    }

    @Transactional
    public ClientDtos.View update(long id, ClientDtos.Update body, UUID actorId, String correlationId) {
        Client client = find(id);
        checkVersion(client, body.version());
        Contact contact = contact(body.phone(), body.email());
        client.setFullName(body.fullName().trim());
        client.setPhone(contact.phone());
        client.setEmail(contact.email());
        clients.flush();
        record(actorId, "CLIENT_UPDATED", id, correlationId);
        return view(client);
    }

    @Transactional
    public ClientDtos.View setActive(long id, long version, boolean active, UUID actorId, String correlationId) {
        Client client = find(id);
        checkVersion(client, version);
        if (client.isActive() == active) throw new ConflictFailure("Клиент уже имеет этот статус");
        client.setActive(active);
        clients.flush();
        record(actorId, active ? "CLIENT_ACTIVATED" : "CLIENT_DEACTIVATED", id, correlationId);
        return view(client);
    }

    private Client find(long id) {
        return clients.findById(id).orElseThrow(() -> new NotFoundFailure("Клиент не найден"));
    }

    private static void checkVersion(Client client, long version) {
        if (version < 0 || client.getVersion() != version) {
            throw new ConflictFailure("Версия клиента устарела; обновите данные");
        }
    }

    private static Contact contact(String phoneValue, String emailValue) {
        String phone = blankToNull(phoneValue);
        String email = blankToNull(emailValue);
        if (phone == null && email == null) {
            throw new IllegalArgumentException("Укажите телефон или email клиента");
        }
        if (phone != null && (!phone.matches("\\+?[0-9() -]+")
            || phone.chars().filter(Character::isDigit).count() < 7)) {
            throw new IllegalArgumentException("Некорректный номер телефона");
        }
        return new Contact(phone, email == null ? null : email.toLowerCase(Locale.ROOT));
    }

    private static String blankToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void record(UUID actorId, String type, long clientId, String correlationId) {

        audit.save(new AuditEvent(UUID.randomUUID(), users.getReferenceById(actorId), type,
            "CLIENT", null, Instant.now(), "Клиент #" + clientId, correlationId));
    }

    private static ClientDtos.View view(Client client) {
        return new ClientDtos.View(client.getId(), client.getFullName(), client.getPhone(),
            client.getEmail(), client.isActive(), client.getVersion());
    }

    private record Contact(String phone, String email) {
    }
}
