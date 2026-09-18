package com.lcorp.console.repository;

import com.lcorp.console.model.query.RequestStatusStatistics;
import com.lcorp.console.model.query.TransportationRequestSearchCriteria;
import com.lcorp.console.model.query.TransportationRequestView;

import java.util.List;

// Отдельный read-only контракт для ручных SQL-запросов
public interface TransportationRequestQueryRepository {

    List<TransportationRequestView> search(TransportationRequestSearchCriteria criteria);

    List<RequestStatusStatistics> getStatisticsByStatus();
}
