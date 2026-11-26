package dev.noren.staybooking.property.internal.web.controller;

import dev.noren.staybooking.property.internal.application.result.AmenityResult;
import dev.noren.staybooking.property.internal.application.service.AmenityService;
import dev.noren.staybooking.property.internal.web.mapper.AmenityWebMapper;
import dev.noren.staybooking.property.internal.web.request.CreateAmenityRequest;
import dev.noren.staybooking.property.internal.web.response.AmenityResponse;
import dev.noren.staybooking.bootstrap.web.ApiProblemDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/amenities")
@RequiredArgsConstructor
@Tag(name = "Amenities")
public class AmenityController {

    private final AmenityService service;
    private final AmenityWebMapper mapper;

    @GetMapping
    @Operation(summary = "List amenities")
    @ApiResponse(responseCode = "200", description = "Amenities")
    @ApiResponse(responseCode = "401", description = "Invalid bearer token", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    public List<AmenityResponse> getAll() {
        List<AmenityResult> amenities = service.getAll();
        return mapper.toResponse(amenities);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create an amenity", description = "ADMIN only.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Amenity created"),
            @ApiResponse(responseCode = "400", description = "Invalid name", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Amenity already exists", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public AmenityResponse create(@Valid @RequestBody CreateAmenityRequest request) {
        AmenityResult amenity = service.create(request.name());
        return mapper.toResponse(amenity);
    }
}
