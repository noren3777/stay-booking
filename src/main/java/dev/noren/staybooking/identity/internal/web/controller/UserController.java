package dev.noren.staybooking.identity.internal.web.controller;

import dev.noren.staybooking.identity.internal.application.result.UserResult;
import dev.noren.staybooking.identity.internal.application.service.UserService;
import dev.noren.staybooking.identity.internal.web.mapper.AuthWebMapper;
import dev.noren.staybooking.identity.internal.web.response.UserResponse;
import dev.noren.staybooking.bootstrap.web.ApiProblemDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
@Tag(name = "Users")
public class UserController {

    private final UserService userService;
    private final AuthWebMapper mapper;

    @GetMapping("/me")
    @Operation(summary = "Get the current user")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponse(responseCode = "200", description = "Current user")
    @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    @ApiResponse(responseCode = "404", description = "Current user no longer exists", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    public UserResponse getCurrentUser() {
        UserResult result = userService.getCurrentUser();
        return mapper.toResponse(result);
    }

}
