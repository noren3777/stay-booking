package dev.noren.staybooking.identity.internal.web.request;

import dev.noren.staybooking.identity.internal.domain.enums.UserRole;
import dev.noren.staybooking.identity.internal.web.validation.Password;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.annotation.Nullable;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Email @Size(max = 255) @Schema(example = "alice@example.com") String email,
        @NotBlank @Size(min = 6) @Password @Schema(example = "Integration-password-31") String password,
        @Nullable @Schema(description = "Defaults to GUEST. Public registration cannot create ADMIN users.", allowableValues = {"GUEST", "HOST"}, example = "GUEST") UserRole role
) {
}
