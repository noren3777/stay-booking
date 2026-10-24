package dev.noren.staybooking.property.internal.web.controller;

import dev.noren.staybooking.bootstrap.web.parser.VersionHeader;
import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.application.service.PropertyService;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import dev.noren.staybooking.property.internal.web.mapper.PropertyWebMapper;
import dev.noren.staybooking.property.internal.web.request.*;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RequestMapping("/api/properties")
@RestController
@RequiredArgsConstructor
public class PropertyController {

    private final PropertyService service;
    private final PropertyWebMapper mapper;
    private final VersionHeader versionHeader;

    @GetMapping("/{id}")
    public ResponseEntity<PropertyResponse> getById(@PathVariable UUID id) {
        PropertyResult property = service.getActiveById(id);
        return propertyResponse(property);
    }

    @GetMapping("/{id}/management")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    public ResponseEntity<PropertyResponse> management(@PathVariable UUID id) {
        PropertyResult property = service.getForManagement(id);
        return propertyResponse(property);
    }

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody CreatePropertyRequest request) {
        CreatePropertyDetails details = mapper.toDetails(request);
        PropertyResult property = service.create(details);

        PropertyResponse response = mapper.toResponse(property);
        String etag = versionHeader.toEtag(property.version());

        return ResponseEntity
                .created(URI.create("/api/properties/" + property.id()))
                .header(HttpHeaders.ETAG, etag)
                .body(response);
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> update(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody UpdatePropertyRequest request
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        PropertyDetailsPatch details = mapper.toDetails(request);
        PropertyResult property = service.update(id, details, expectedVersion);
        return propertyResponse(property);
    }

    @PutMapping("/{id}/amenities")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> replaceAmenities(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ReplacePropertyAmenitiesRequest request
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.replaceAmenities(id, request.amenityIds(), expectedVersion);
        return voidResponse(version);
    }

    @PutMapping("/{id}/images")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> replaceImages(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @RequestBody List<@Valid PropertyImageRequest> request
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);

        List<PropertyImageDetails> images = mapper.toImageDetails(request);
        long version = service.replaceImages(id, images, expectedVersion);

        return voidResponse(version);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('HOST')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        service.delete(id, expectedVersion);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> activate(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.activate(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.deactivate(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> block(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.block(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblock(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.unblock(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/host")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeHost(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeHostRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.changeHost(id, request.hostId(), expectedVersion);
        return voidResponse(version);
    }

    private ResponseEntity<Void> voidResponse(long version) {
        String etag = versionHeader.toEtag(version);

        return ResponseEntity.noContent()
                .header(HttpHeaders.ETAG, etag)
                .build();
    }

    private ResponseEntity<PropertyResponse> propertyResponse(PropertyResult property) {
        PropertyResponse response = mapper.toResponse(property);
        String etag = versionHeader.toEtag(property.version());

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, etag)
                .body(response);
    }

}
