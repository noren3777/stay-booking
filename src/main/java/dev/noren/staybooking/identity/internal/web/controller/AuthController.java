package dev.noren.staybooking.identity.internal.web.controller;

import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.application.service.AuthService;
import dev.noren.staybooking.identity.internal.web.request.LoginRequest;
import dev.noren.staybooking.identity.internal.web.request.RegisterRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResult register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(
                request.email(),
                request.password(),
                request.role()
        );
    }

    @PostMapping("/login")
    public TokenResult login(@Valid @RequestBody LoginRequest request) {
        return authService.login(
                request.email(),
                request.password()
        );
    }

}
