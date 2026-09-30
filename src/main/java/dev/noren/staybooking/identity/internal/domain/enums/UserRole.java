package dev.noren.staybooking.identity.internal.domain.enums;

public enum UserRole {
    GUEST,
    HOST,
    ADMIN;

    public boolean canRegister() {
        return this == GUEST || this == HOST;
    }
}
