package dev.noren.staybooking.property.internal.domain.enums;

public enum PropertyStatus {
    ACTIVE,
    INACTIVE,
    BLOCKED;

    public boolean isBlocked() {
        return this == BLOCKED;
    }

    public boolean isActive() {
        return this == ACTIVE;
    }
}
