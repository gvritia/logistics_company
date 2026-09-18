package com.lcorp.console.app;

import java.util.Objects;

public final class Session {

    private final Role role;
    private final Long actorId;
    private final String actorName;

    public Session(Role role, Long actorId, String actorName) {
        this.role = Objects.requireNonNull(role);
        this.actorId = Objects.requireNonNull(actorId);
        this.actorName = Objects.requireNonNull(actorName);
    }

    public Role getRole() {
        return role;
    }

    public Long getActorId() {
        return actorId;
    }

    public String getActorName() {
        return actorName;
    }
}
