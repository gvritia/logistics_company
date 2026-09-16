package com.lcorp.console.model;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TransportationRequest {


    private Long id;


    private Long clientId;


    private Long driverId;


    private Long operatorId;

    private String cargoDescription;


    private BigDecimal weightKg;


    private BigDecimal price;

    private LocalDateTime createdAt;
    private LocalDateTime plannedDeliveryAt;
    private TransportationRequestStatus status;  // enum
    private String pickupAddress;
    private String deliveryAddress;


    public TransportationRequest(
        Long clientId,
        Long operatorId,
        String cargoDescription,
        BigDecimal weightKg,
        BigDecimal price,
        LocalDateTime plannedDeliveryAt,
        String pickupAddress,
        String deliveryAddress
    ) {
        this(
            null,
            clientId,
            null,
            operatorId,
            cargoDescription,
            weightKg,
            price,
            LocalDateTime.now(),
            plannedDeliveryAt,
            TransportationRequestStatus.CREATED,
            pickupAddress,
            deliveryAddress
        );
    }


    public TransportationRequest(
        Long id,
        Long clientId,
        Long driverId,
        Long operatorId,
        String cargoDescription,
        BigDecimal weightKg,
        BigDecimal price,
        LocalDateTime createdAt,
        LocalDateTime plannedDeliveryAt,
        TransportationRequestStatus status,
        String pickupAddress,
        String deliveryAddress
    ) {
        this.id = id;
        this.clientId = clientId;
        this.driverId = driverId;
        this.operatorId = operatorId;
        this.cargoDescription = cargoDescription;
        this.weightKg = weightKg;
        this.price = price;
        this.createdAt = createdAt;
        this.plannedDeliveryAt = plannedDeliveryAt;
        this.status = status;
        this.pickupAddress = pickupAddress;
        this.deliveryAddress = deliveryAddress;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getOperatorId() {
        return operatorId;
    }

    public void setOperatorId(Long operatorId) {
        this.operatorId = operatorId;
    }

    public String getCargoDescription() {
        return cargoDescription;
    }

    public void setCargoDescription(String cargoDescription) {
        this.cargoDescription = cargoDescription;
    }

    public BigDecimal getWeightKg() {
        return weightKg;
    }

    public void setWeightKg(BigDecimal weightKg) {
        this.weightKg = weightKg;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPlannedDeliveryAt() {
        return plannedDeliveryAt;
    }

    public void setPlannedDeliveryAt(LocalDateTime plannedDeliveryAt) {
        this.plannedDeliveryAt = plannedDeliveryAt;
    }

    public TransportationRequestStatus getStatus() {
        return status;
    }

    public void setStatus(TransportationRequestStatus status) {
        this.status = status;
    }

    public String getPickupAddress() {
        return pickupAddress;
    }

    public void setPickupAddress(String pickupAddress) {
        this.pickupAddress = pickupAddress;
    }

    public String getDeliveryAddress() {
        return deliveryAddress;
    }

    public void setDeliveryAddress(String deliveryAddress) {
        this.deliveryAddress = deliveryAddress;
    }

    @Override
    public String toString() {
        return "TransportationRequest{" +
            "id=" + id +
            ", clientId=" + clientId +
            ", driverId=" + driverId +
            ", operatorId=" + operatorId +
            ", cargoDescription='" + cargoDescription + '\'' +
            ", weightKg=" + weightKg +
            ", price=" + price +
            ", createdAt=" + createdAt +
            ", plannedDeliveryAt=" + plannedDeliveryAt +
            ", status=" + status +
            ", pickupAddress='" + pickupAddress + '\'' +
            ", deliveryAddress='" + deliveryAddress + '\'' +
            '}';
    }
}
