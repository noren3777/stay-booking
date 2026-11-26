package dev.noren.staybooking.identity.internal.web.controller;

import dev.noren.staybooking.identity.internal.application.result.TokenResult;
import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.application.service.AuthService;
import dev.noren.staybooking.identity.internal.web.mapper.AuthWebMapper;
import dev.noren.staybooking.identity.internal.web.request.LoginRequest;
import dev.noren.staybooking.identity.internal.web.request.RegisterRequest;
import dev.noren.staybooking.identity.internal.web.response.TokenResponse;
import dev.noren.staybooking.identity.internal.web.response.UserResponse;
import dev.noren.staybooking.bootstrap.web.ApiProblemDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication")
public class AuthController {

    private final AuthService authService;
    private final AuthWebMapper mapper;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a guest or host", description = "The role defaults to GUEST. ADMIN registration is rejected.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "User registered"),
            @ApiResponse(responseCode = "400", description = "Invalid registration", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Email already registered", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        UserResult result = authService.register(request.email(), request.password(), request.role());
        return mapper.toResponse(result);
    }

    @PostMapping("/login")
    @Operation(summary = "Log in and receive a bearer token")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "JWT access token"),
            @ApiResponse(responseCode = "400", description = "Invalid request", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        TokenResult result = authService.login(request.email(), request.password());
        return mapper.toResponse(result);
    }

}
