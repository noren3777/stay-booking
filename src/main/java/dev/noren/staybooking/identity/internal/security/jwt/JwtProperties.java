package dev.noren.staybooking.identity.internal.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(
        String secretBase64,
        String issuer,
        Duration ttl
) {
    public JwtProperties {
        if (secretBase64 == null || secretBase64.isBlank()) {
            throw new IllegalArgumentException("JWT_SECRET_BASE64 is required and must not be blank");
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT issuer cannot be null or blank");
        }

        if (ttl == null || ttl.isZero() || ttl.isNegative()) {
            throw new IllegalArgumentException("JWT TTL must be greater than zero");
        }
    }
}
