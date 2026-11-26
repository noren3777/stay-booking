package dev.noren.staybooking.identity.internal.web.response;

public record TokenResponse(
        String token,
        String type
) {
}
