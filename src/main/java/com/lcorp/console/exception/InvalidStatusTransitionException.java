package com.lcorp.console.exception;

import com.lcorp.console.model.TransportationRequestStatus;

public class InvalidStatusTransitionException extends LogisticsException {

    private static final long serialVersionUID = 1L;

    private final TransportationRequestStatus currentStatus;

    private final TransportationRequestStatus targetStatus;

    public InvalidStatusTransitionException(
            TransportationRequestStatus currentStatus,
            TransportationRequestStatus targetStatus
    ) {
        super("Недопустимый переход статуса заявки: "
                + currentStatus + " -> " + targetStatus);
        this.currentStatus = currentStatus;
        this.targetStatus = targetStatus;
    }

    public TransportationRequestStatus getCurrentStatus() {
        return currentStatus;
    }

    public TransportationRequestStatus getTargetStatus() {
        return targetStatus;
    }
}
