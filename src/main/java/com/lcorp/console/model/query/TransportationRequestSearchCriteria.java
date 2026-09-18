package com.lcorp.console.model.query;

import com.lcorp.console.model.TransportationRequestStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

// Необязательные условия поиска. Незаполненное поле не добавляется в WHERE
public final class TransportationRequestSearchCriteria {

    private String searchText;
    private TransportationRequestStatus status;
    private Long clientId;
    private Long driverId;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime plannedFrom;
    private LocalDateTime plannedTo;
    private TransportationRequestSortField sortField =
        TransportationRequestSortField.PLANNED_DELIVERY_AT;
    private SortDirection sortDirection = SortDirection.ASC;

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    public TransportationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(TransportationRequestStatus status) {
        this.status = status;
    }

    public Long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public void setDriverId(Long driverId) {
        this.driverId = driverId;
    }

    public BigDecimal getMinPrice() {
        return minPrice;
    }

    public void setMinPrice(BigDecimal minPrice) {
        this.minPrice = minPrice;
    }

    public BigDecimal getMaxPrice() {
        return maxPrice;
    }

    public void setMaxPrice(BigDecimal maxPrice) {
        this.maxPrice = maxPrice;
    }

    public LocalDateTime getPlannedFrom() {
        return plannedFrom;
    }

    public void setPlannedFrom(LocalDateTime plannedFrom) {
        this.plannedFrom = plannedFrom;
    }

    public LocalDateTime getPlannedTo() {
        return plannedTo;
    }

    public void setPlannedTo(LocalDateTime plannedTo) {
        this.plannedTo = plannedTo;
    }

    public TransportationRequestSortField getSortField() {
        return sortField;
    }

    public void setSortField(TransportationRequestSortField sortField) {
        this.sortField = Objects.requireNonNull(sortField);
    }

    public SortDirection getSortDirection() {
        return sortDirection;
    }

    public void setSortDirection(SortDirection sortDirection) {
        this.sortDirection = Objects.requireNonNull(sortDirection);
    }
}
