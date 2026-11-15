package dev.noren.staybooking.property.internal.web.controller;

import dev.noren.staybooking.property.internal.application.result.AmenityResult;
import dev.noren.staybooking.property.internal.application.service.AmenityService;
import dev.noren.staybooking.property.internal.web.mapper.AmenityWebMapper;
import dev.noren.staybooking.property.internal.web.request.CreateAmenityRequest;
import dev.noren.staybooking.property.internal.web.response.AmenityResponse;
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
public class AmenityController {

    private final AmenityService service;
    private final AmenityWebMapper mapper;

    @GetMapping
    public List<AmenityResponse> getAll() {
        List<AmenityResult> amenities = service.getAll();
        return mapper.toResponse(amenities);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public AmenityResponse create(@Valid @RequestBody CreateAmenityRequest request) {
        AmenityResult amenity = service.create(request.name());
        return mapper.toResponse(amenity);
    }
}
