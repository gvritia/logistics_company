package com.lcorp.javafx.service;

import com.lcorp.console.model.TransportationRequest;
import com.lcorp.console.service.TransportationRequestService;
import com.lcorp.javafx.model.ReferenceOption;
import com.lcorp.javafx.repository.JdbcRequestCreateDao;

import java.util.List;
import java.util.Objects;

public final class RequestCreationService {
    private final JdbcRequestCreateDao dao;

    public RequestCreationService(JdbcRequestCreateDao dao) {
        this.dao = Objects.requireNonNull(dao);
    }

    public List<ReferenceOption> findClients() {
        return dao.findClients();
    }

    public List<ReferenceOption> findOperators() {
        return dao.findOperators();
    }

    public long create(TransportationRequest request) {
        TransportationRequestService.validateNewRequest(request);
        return dao.create(request);
    }
}
