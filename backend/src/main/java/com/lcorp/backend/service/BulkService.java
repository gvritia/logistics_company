package com.lcorp.backend.service;

import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.dto.BulkDtos;
import com.lcorp.backend.dto.RequestDtos;
import com.lcorp.backend.error.ConflictFailure;
import com.lcorp.backend.error.NotFoundFailure;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class BulkService {
    private final RequestService requests;

    public BulkService(RequestService requests) {
        this.requests = requests;
    }


    public BulkDtos.Result execute(BulkDtos.Request body, UUID actorId, String correlationId) {
        String tag = validate(body);
        List<BulkDtos.Success> successes = new ArrayList<>();
        List<BulkDtos.Failure> failures = new ArrayList<>();
        for (BulkDtos.Target target : body.items()) {
            try {
                RequestDtos.Item item = apply(body.action(), body.status(), tag,
                    target.id(), target.version(), actorId, correlationId);
                successes.add(new BulkDtos.Success(item.id(), item.version(), item.status(), item.archived()));
            } catch (NotFoundFailure ex) {
                failures.add(new BulkDtos.Failure(target.id(), "NOT_FOUND", ex.getMessage()));
            } catch (ConflictFailure ex) {
                failures.add(new BulkDtos.Failure(target.id(), "CONFLICT", ex.getMessage()));
            } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException ex) {
                failures.add(new BulkDtos.Failure(target.id(), "CONFLICT", "Данные изменились; обновите список"));
            }
        }
        return new BulkDtos.Result(successes.size(), failures.size(), successes, failures);
    }

    private RequestDtos.Item apply(BulkDtos.Action action, TransportationRequestStatus status, String tag,
                                   UUID id, long version, UUID actorId, String correlationId) {
        return switch (action) {
            case ARCHIVE -> requests.archive(id, version, actorId, correlationId);
            case RESTORE -> requests.restore(id, version, actorId, correlationId);
            case CHANGE_STATUS -> requests.act(id, version, statusAction(status), null, actorId, correlationId);
            case ADD_TAG -> requests.changeTag(id, version, tag, true, actorId, correlationId);
            case REMOVE_TAG -> requests.changeTag(id, version, tag, false, actorId, correlationId);
        };
    }

    private static RequestService.Action statusAction(TransportationRequestStatus status) {
        return switch (status) {
            case APPROVED -> RequestService.Action.APPROVE;
            case CANCELLED -> RequestService.Action.CANCEL;
            case IN_TRANSIT -> RequestService.Action.START;
            case DELIVERED -> RequestService.Action.DELIVER;
            case CREATED -> throw new IllegalArgumentException("Возврат в CREATED запрещён");
        };
    }

    private static String validate(BulkDtos.Request body) {
        Set<UUID> ids = new HashSet<>();
        for (BulkDtos.Target target : body.items()) {
            if (!ids.add(target.id())) throw new IllegalArgumentException("UUID в наборе повторяются");
        }
        if (body.action() == BulkDtos.Action.CHANGE_STATUS) {
            if (body.status() == null || body.status() == TransportationRequestStatus.CREATED
                || body.tag() != null) throw new IllegalArgumentException("Укажите допустимый status без tag");
        } else if (body.action() == BulkDtos.Action.ADD_TAG || body.action() == BulkDtos.Action.REMOVE_TAG) {
            if (body.status() != null || body.tag() == null || body.tag().isBlank()) {
                throw new IllegalArgumentException("Укажите tag без status");
            }
        } else if (body.status() != null || body.tag() != null) {
            throw new IllegalArgumentException("Для действия status и tag не требуются");
        }
        return body.tag() == null ? null : body.tag().trim();
    }
}
