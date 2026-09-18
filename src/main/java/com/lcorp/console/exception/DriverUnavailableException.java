package com.lcorp.console.exception;

public class DriverUnavailableException extends LogisticsException {

    private static final long serialVersionUID = 1L; // для сериализации (версия сериализуемого класса)

    private final Long driverId;

    private final String reason;

    public DriverUnavailableException(Long driverId, String reason) {
        super("Доставщик с ID " + driverId + " недоступен: " + reason);
        this.driverId = driverId;
        this.reason = reason;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getReason() {
        return reason;
    }
}
