package dev.noren.staybooking.bootstrap.domain;

public record FieldChange<T>(boolean specified, T value) {
}
