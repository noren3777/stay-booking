package dev.noren.staybooking.property.internal.application.result;

public record PropertyImageResult(
        String url,
        int sortOrder,
        boolean primary
) {
}
