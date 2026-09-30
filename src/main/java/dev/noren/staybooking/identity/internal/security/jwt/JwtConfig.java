package dev.noren.staybooking.identity.internal.security.jwt;

import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.UUID;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

    @Bean
    SecretKey jwtSecretKey(JwtProperties jwtProperties) {
        byte[] bytes = Base64.getDecoder().decode(jwtProperties.secretBase64());

        if (bytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must contain at least 32 bytes");
        }

        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey jwtSecretKey) {
        return NimbusJwtEncoder
                .withSecretKey(jwtSecretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
    }

    @Bean
    JwtDecoder jwtDecoder(
            SecretKey jwtSecretKey,
            JwtProperties jwtProperties
    ) {
        NimbusJwtDecoder decoder = NimbusJwtDecoder
                .withSecretKey(jwtSecretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();

        JwtTimestampValidator timestamps = new JwtTimestampValidator();
        timestamps.setAllowEmptyExpiryClaim(false);

        JwtClaimValidator<Object> subject = new JwtClaimValidator<>(
                JwtClaimNames.SUB,
                value -> value instanceof String sub && isUuid(sub)
        );

        JwtClaimValidator<Object> role = new JwtClaimValidator<>(
                "role",
                value -> value instanceof String name
                        && Arrays.stream(UserRole.values())
                        .anyMatch(userRole -> userRole.name().equals(name))
        );

        decoder.setJwtValidator(
                JwtValidators.createDefaultWithValidators(
                        List.of(
                                new JwtIssuerValidator(jwtProperties.issuer()),
                                timestamps,
                                subject,
                                role
                        )
                )
        );

        return decoder;
    }


    private static boolean isUuid(String value) {
        try {
            return UUID.fromString(value).toString().equalsIgnoreCase(value);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

}
