package dev.noren.staybooking.identity.internal.web.controller;

import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.application.service.AuthService;
import dev.noren.staybooking.identity.internal.web.mapper.AuthWebMapper;
import dev.noren.staybooking.identity.internal.web.request.LoginRequest;
import dev.noren.staybooking.identity.internal.web.request.RegisterRequest;
import dev.noren.staybooking.identity.internal.web.response.TokenResponse;
import dev.noren.staybooking.identity.internal.web.response.UserResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthWebMapper mapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        UserResult result = authService.register(request.email(), request.password(), request.role());
        return mapper.toResponse(result);
    }

    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenResult result = authService.login(request.email(), request.password());
        return mapper.toResponse(result);
    }

}
