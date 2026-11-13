package com.lcorp.backend.service;

import com.lcorp.backend.domain.AuditEvent;
import com.lcorp.backend.domain.Client;
import com.lcorp.backend.domain.Operator;
import com.lcorp.backend.domain.TransportationRequest;
import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.dto.RequestDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.error.NotFoundFailure;
import com.lcorp.backend.repository.AuditEventRepository;
import com.lcorp.backend.repository.ClientRepository;
import com.lcorp.backend.repository.OperatorRepository;
import com.lcorp.backend.repository.TransportationRequestRepository;
import com.lcorp.backend.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
public class RequestService {
    private final TransportationRequestRepository requests;
    private final ClientRepository clients;
    private final OperatorRepository operators;
    private final UserAccountRepository users;
    private final AuditEventRepository audit;
    private final boolean permanentDeleteEnabled;

    public RequestService(TransportationRequestRepository requests, ClientRepository clients,
                          OperatorRepository operators, UserAccountRepository users,
                          AuditEventRepository audit,
                          @Value("${app.requests.permanent-delete-enabled:false}") boolean permanentDeleteEnabled) {
        this.requests = requests;
        this.clients = clients;
        this.operators = operators;
        this.users = users;
        this.audit = audit;
        this.permanentDeleteEnabled = permanentDeleteEnabled;
    }

    @Transactional(readOnly = true)
    public RequestDtos.PageResponse list(RequestSearch search) {
        Page<TransportationRequest> result = requests.findAll(search.specification(),
            PageRequest.of(search.page(), search.size(), search.safeSort()));
        return new RequestDtos.PageResponse(result.stream().map(RequestService::view).toList(),
            search.page(), search.size(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public RequestDtos.Item get(UUID id) {
        return view(find(id));
    }

    @Transactional
    public RequestDtos.Item create(RequestDtos.Create body, UUID actorId, String correlationId) {
        validateFields(body.pickupAddress(), body.deliveryAddress(), body.plannedDeliveryAt(), LocalDateTime.now());
        String code = code(body.requestCode());
        if (requests.existsByRequestCodeIgnoreCase(code)) throw new ConflictFailure("Код заявки уже занят");
        TransportationRequest request = new TransportationRequest(code, client(body.clientId(), null),
            operator(body.operatorId()), body.cargoDescription().trim(), body.weightKg(), body.price(),
            body.plannedDeliveryAt(), body.pickupAddress().trim(), body.deliveryAddress().trim(), body.fragile());
        request.setTags(tags(body.tags()));
        requests.saveAndFlush(request);
        record(actorId, "REQUEST_CREATED", request.getPublicId(), "Заявка создана", correlationId);
        return view(request);
    }

    @Transactional
    public RequestDtos.Item update(UUID id, RequestDtos.Update body, UUID actorId, String correlationId) {
        TransportationRequest request = find(id);
        checkVersion(request, body.version());
        if (request.isArchived() || request.getStatus() != TransportationRequestStatus.CREATED) {
            throw new ConflictFailure("Редактировать можно только активную новую заявку");
        }
        validateFields(body.pickupAddress(), body.deliveryAddress(), body.plannedDeliveryAt(), request.getCreatedAt());
        String code = code(body.requestCode());
        if (!request.getRequestCode().equalsIgnoreCase(code) && requests.existsByRequestCodeIgnoreCase(code)) {
            throw new ConflictFailure("Код заявки уже занят");
        }
        request.setRequestCode(code);
        request.setClient(client(body.clientId(), request.getClient().getId()));
        request.setOperator(operator(body.operatorId()));
        request.setCargoDescription(body.cargoDescription().trim());
        request.setWeightKg(body.weightKg());
        request.setPrice(body.price());
        request.setPickupAddress(body.pickupAddress().trim());
        request.setDeliveryAddress(body.deliveryAddress().trim());
        request.setPlannedDeliveryAt(body.plannedDeliveryAt());
        request.setFragile(body.fragile());
        request.setTags(tags(body.tags()));
        requests.flush();
        record(actorId, "REQUEST_UPDATED", id, "Заявка изменена", correlationId);
        return view(request);
    }

    @Transactional
    public RequestDtos.Item archive(UUID id, long version, UUID actorId, String correlationId) {
        TransportationRequest request = find(id);
        checkVersion(request, version);
        if (request.isArchived()) throw new ConflictFailure("Заявка уже в архиве");
        request.setArchived(true);
        requests.flush();
        record(actorId, "REQUEST_ARCHIVED", id, "Заявка отправлена в архив", correlationId);
        return view(request);
    }

    @Transactional
    public RequestDtos.Item restore(UUID id, long version, UUID actorId, String correlationId) {
        TransportationRequest request = find(id);
        checkVersion(request, version);
        if (!request.isArchived()) throw new ConflictFailure("Заявка не находится в архиве");
        request.setArchived(false);
        requests.flush();
        record(actorId, "REQUEST_RESTORED", id, "Заявка восстановлена", correlationId);
        return view(request);
    }

    @Transactional
    public void permanentDelete(UUID id, long version, UUID actorId, String correlationId) {
        if (!permanentDeleteEnabled) throw new ConflictFailure("Окончательное удаление отключено настройкой сервера");
        TransportationRequest request = find(id);
        checkVersion(request, version);
        if (!request.isArchived()) throw new ConflictFailure("Сначала отправьте заявку в архив");
        requests.delete(request);
        requests.flush();
        record(actorId, "REQUEST_DELETED", id, "Заявка окончательно удалена", correlationId);
    }

    private TransportationRequest find(UUID id) {
        return requests.findByPublicId(id).orElseThrow(() -> new NotFoundFailure("Заявка не найдена"));
    }

    private Client client(Long id, Long currentId) {
        Client client = clients.findById(id).orElseThrow(() -> new NotFoundFailure("Клиент не найден"));
        if (!client.isActive() && !id.equals(currentId)) throw new ConflictFailure("Клиент отключён");
        return client;
    }

    private Operator operator(Long id) {
        return operators.findById(id).orElseThrow(() -> new NotFoundFailure("Оператор не найден"));
    }

    private void record(UUID actorId, String type, UUID requestId, String description, String correlationId) {
        audit.save(new AuditEvent(UUID.randomUUID(), users.getReferenceById(actorId), type,
            "TRANSPORTATION_REQUEST", requestId, Instant.now(), description, correlationId));
    }

    private static void checkVersion(TransportationRequest request, long expected) {
        if (expected < 0 || request.getVersion() != expected) {
            throw new ConflictFailure("Версия заявки устарела; обновите данные");
        }
    }

    private static void validateFields(String pickup, String delivery, LocalDateTime planned, LocalDateTime created) {
        if (pickup.trim().equalsIgnoreCase(delivery.trim())) {
            throw new IllegalArgumentException("Адреса отправления и доставки должны отличаться");
        }
        if (!planned.isAfter(created)) {
            throw new IllegalArgumentException("Плановая доставка должна быть позже создания заявки");
        }
    }

    private static String code(String value) { return value.trim().toUpperCase(Locale.ROOT); }

    private static Set<String> tags(Set<String> values) {
        if (values == null) return Set.of();
        Set<String> result = new LinkedHashSet<>();
        for (String value : values) {
            String tag = value.trim();
            if (tag.isEmpty()) throw new IllegalArgumentException("Пустой тег недопустим");
            result.add(tag);
        }
        return result;
    }

    private static RequestDtos.Item view(TransportationRequest request) {
        return new RequestDtos.Item(request.getPublicId(), request.getRequestCode(),
            request.getClient().getId(), request.getClient().getFullName(),
            request.getOperator().getId(), request.getOperator().getFullName(),
            request.getDriver() == null ? null : request.getDriver().getId(),
            request.getDriver() == null ? null : request.getDriver().getFullName(),
            request.getCargoDescription(), request.getWeightKg(), request.getPrice(),
            request.getPickupAddress(), request.getDeliveryAddress(), request.getPlannedDeliveryAt(),
            request.getStatus(), request.getWeightCategory(), request.isFragile(), request.isArchived(),
            Set.copyOf(request.getTags()), request.getCreatedAt(), request.getUpdatedAt(), request.getVersion());
    }
}
