package dev.noren.staybooking.identity.internal.web.request;

import dev.noren.staybooking.identity.internal.web.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 255) @Schema(example = "alice@example.com") String email,
        @NotBlank @Password @Schema(example = "Integration-password-31") String password
) {
}
