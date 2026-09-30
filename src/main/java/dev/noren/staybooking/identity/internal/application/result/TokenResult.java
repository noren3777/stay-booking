package dev.noren.staybooking.identity.internal.application.result;

public record TokenResult(
        String token,
        String type
) {
}
