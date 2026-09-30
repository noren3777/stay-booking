package dev.noren.staybooking.identity.internal.web.request;

import dev.noren.staybooking.identity.internal.web.validation.Password;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        @NotBlank @Email @Size(max = 255) String email,
        @NotBlank @Password String password
) {
}
