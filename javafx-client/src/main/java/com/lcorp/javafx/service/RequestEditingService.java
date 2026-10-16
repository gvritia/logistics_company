package com.lcorp.javafx.service;

import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.model.TransportationRequestStatus;
import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.service.TransportationRequestService;
import com.lcorp.javafx.repository.JdbcRequestUpdateDao;

import java.util.Objects;

public final class RequestEditingService {
    private final JdbcRequestUpdateDao dao;

    public RequestEditingService(JdbcRequestUpdateDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    // Проверяет выбранную строку и данные формы сам DAO меняет только разрешённые поля
    public long update(TransportationRequestView existing, TransportationRequest edited) {
        Objects.requireNonNull(existing);
        Objects.requireNonNull(edited);
        if (existing.status() != TransportationRequestStatus.CREATED
            || existing.driverId() != null) {
            throw new IllegalStateException("Редактировать можно только заявку CREATED без доставщика.");
        }
        if (!Objects.equals(existing.id(), edited.getId())) {
            throw new IllegalArgumentException("ID редактируемой заявки изменился.");
        }
        TransportationRequestService.validateEditableDetails(edited, existing.createdAt());

        if (!dao.update(edited)) {
            throw new IllegalStateException(
                "Заявка удалена или уже обрабатывается. Обновите список заявок."
            );
        }
        return existing.id();
    }
}
