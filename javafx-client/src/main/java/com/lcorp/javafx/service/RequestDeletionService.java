package com.lcorp.javafx.service;

import com.lcorp.console.model.query.TransportationRequestView;
import com.lcorp.console.service.TransportationRequestService;
import com.lcorp.javafx.repository.JdbcRequestDeleteDao;

import java.util.Objects;

public final class RequestDeletionService {
    private final JdbcRequestDeleteDao dao;

    public RequestDeletionService(JdbcRequestDeleteDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    // Проверяет выбранную строк при устаревших данных DAO вернёт false
    public void delete(TransportationRequestView request) {
        Objects.requireNonNull(request);
        if (!TransportationRequestService.isEditableDraft(request.status(), request.driverId())) {
            throw new IllegalStateException("Удалить можно только заявку CREATED без доставщика.");
        }
        if (request.id() == null || request.id() <= 0) {
            throw new IllegalArgumentException("ID заявки должен быть положительным.");
        }
        if (!dao.deleteById(request.id())) {
            throw new IllegalStateException(
                "Заявка удалена или уже обрабатывается. Обновите список заявок."
            );
        }
    }
}
