package dev.noren.staybooking.property.internal.web.response;

public record PropertyImageResponse(
        String url,
        int sortOrder,
        boolean primary
) {
}
