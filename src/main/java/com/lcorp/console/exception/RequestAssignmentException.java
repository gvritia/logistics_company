package com.lcorp.console.exception;

public class RequestAssignmentException extends LogisticsException {

    private static final long serialVersionUID = 1L;

    private final Long requestId;
    private final Long driverId;
    private final String reason;

    public RequestAssignmentException(Long requestId, Long driverId, String reason) {
        super("Невозможно назначить доставщика с ID " + driverId
                + " на заявку с ID " + requestId + ": " + reason);
        this.requestId = requestId;
        this.driverId = driverId;
        this.reason = reason;
    }

    public Long getRequestId() {
        return requestId;
    }

    public Long getDriverId() {
        return driverId;
    }

    public String getReason() {
        return reason;
    }
}
