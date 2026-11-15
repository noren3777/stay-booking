package dev.noren.staybooking.bootstrap.exception.dto;

public record ValidationError(
        String field,
        String message
) {
}
