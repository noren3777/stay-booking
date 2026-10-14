package dev.noren.staybooking.property.internal.web.controller;

import dev.noren.staybooking.bootstrap.web.parser.VersionHeader;
import dev.noren.staybooking.property.internal.application.service.PropertyService;
import dev.noren.staybooking.property.internal.domain.entity.Property;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.web.mapper.PropertyWebMapper;
import dev.noren.staybooking.property.internal.web.request.ChangeHostRequest;
import dev.noren.staybooking.property.internal.web.request.CreatePropertyRequest;
import dev.noren.staybooking.property.internal.web.request.UpdatePropertyRequest;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
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
        Property property = service.getActiveById(id);
        return propertyResponse(property);
    }

    @GetMapping("/{id}/management")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    public ResponseEntity<PropertyResponse> management(@PathVariable UUID id) {
        Property property = service.getForManagement(id);
        return propertyResponse(property);
    }

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<PropertyResponse> create(@Valid @RequestBody CreatePropertyRequest request) {
        CreatePropertyDetails details = mapper.toDetails(request);
        Property property = service.create(details);

        PropertyResponse response = mapper.toResponse(property);
        String etag = versionHeader.toEtag(property.getVersion());

        return ResponseEntity
                .created(URI.create("/api/properties/" + property.getId()))
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
        Property property = service.update(id, details, expectedVersion);
        return propertyResponse(property);
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

        Property property = service.activate(id, expectedVersion);

        return voidResponse(property);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HOST')")
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        Property property = service.deactivate(id, expectedVersion);
        return voidResponse(property);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> block(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        Property property = service.block(id, expectedVersion);
        return voidResponse(property);
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> unblock(
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        Property property = service.unblock(id, expectedVersion);
        return voidResponse(property);
    }

    @PatchMapping("/{id}/host")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> changeHost(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeHostRequest request,
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        Property property = service.changeHost(id, request.hostId(), expectedVersion);
        return voidResponse(property);
    }

    private ResponseEntity<Void> voidResponse(Property property) {
        String etag = versionHeader.toEtag(property.getVersion());

        return ResponseEntity.noContent()
                .header(HttpHeaders.ETAG, etag)
                .build();
    }

    private ResponseEntity<PropertyResponse> propertyResponse(Property property) {
        PropertyResponse response = mapper.toResponse(property);
        String etag = versionHeader.toEtag(property.getVersion());

        return ResponseEntity.ok()
                .header(HttpHeaders.ETAG, etag)
                .body(response);
    }

}
