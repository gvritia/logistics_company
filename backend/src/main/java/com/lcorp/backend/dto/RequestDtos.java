package com.lcorp.backend.dto;

import com.lcorp.backend.domain.TransportationRequestStatus;
import com.lcorp.backend.domain.WeightCategory;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class RequestDtos {
    private RequestDtos() {
    }

    public record Create(@NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String requestCode,
                         @NotNull @Positive Long clientId,
                         @NotNull @Positive Long operatorId,
                         @NotBlank @Size(max = 500) String cargoDescription,
                         @NotNull @DecimalMin("0.001") @Digits(integer = 9, fraction = 3) BigDecimal weightKg,
                         @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
                         @NotBlank @Size(max = 300) String pickupAddress,
                         @NotBlank @Size(max = 300) String deliveryAddress,
                         @NotNull LocalDateTime plannedDeliveryAt,
                         boolean fragile,
                         @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags) {
    }

    public record Update(@NotNull @PositiveOrZero Long version,
                         @NotBlank @Pattern(regexp = "[A-Za-z0-9][A-Za-z0-9._-]{0,39}") String requestCode,
                         @NotNull @Positive Long clientId,
                         @NotNull @Positive Long operatorId,
                         @NotBlank @Size(max = 500) String cargoDescription,
                         @NotNull @DecimalMin("0.001") @Digits(integer = 9, fraction = 3) BigDecimal weightKg,
                         @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal price,
                         @NotBlank @Size(max = 300) String pickupAddress,
                         @NotBlank @Size(max = 300) String deliveryAddress,
                         @NotNull LocalDateTime plannedDeliveryAt,
                         boolean fragile,
                         @Size(max = 20) Set<@NotBlank @Size(max = 50) String> tags) {
    }

    public record Assignment(@NotNull @Positive Long driverId, @NotNull @PositiveOrZero Long version) {
    }

    public record Item(UUID id, String requestCode,
                       Long clientId, String clientName,
                       Long operatorId, String operatorName,
                       Long driverId, String driverName,
                       String cargoDescription, BigDecimal weightKg, BigDecimal price,
                       String pickupAddress, String deliveryAddress,
                       LocalDateTime plannedDeliveryAt, TransportationRequestStatus status,
                       WeightCategory weightCategory, boolean fragile, boolean archived,
                       Set<String> tags, LocalDateTime createdAt, LocalDateTime updatedAt,
                       long version, boolean permanentDeleteEnabled) {
    }

    public record PageResponse(List<Item> items, int page, int size,
                               long totalElements, int totalPages) {
    }
}
