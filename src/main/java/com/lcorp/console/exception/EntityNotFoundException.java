package com.lcorp.console.exception;

public class EntityNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String entityName;

    private final Long entityId;

    public EntityNotFoundException(String entityName, Long entityId) {
        super(entityName + " с ID " + entityId + " не найден(а)");
        this.entityName = entityName;
        this.entityId = entityId;
    }

    public String getEntityName() {
        return entityName;
    }

    public Long getEntityId() {
        return entityId;
    }
}
