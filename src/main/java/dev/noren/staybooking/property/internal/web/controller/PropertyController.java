package dev.noren.staybooking.property.internal.web.controller;

import dev.noren.staybooking.bootstrap.web.parser.VersionHeader;
import dev.noren.staybooking.bootstrap.web.request.PaginationRequest;
import dev.noren.staybooking.property.internal.application.input.PropertyCatalogCriteria;
import dev.noren.staybooking.property.internal.application.result.PropertyCatalogResult;
import dev.noren.staybooking.property.internal.application.result.PropertyResult;
import dev.noren.staybooking.property.internal.application.service.PropertyService;
import dev.noren.staybooking.property.internal.domain.input.CreatePropertyDetails;
import dev.noren.staybooking.property.internal.domain.input.PropertyDetailsPatch;
import dev.noren.staybooking.property.internal.domain.input.PropertyImageDetails;
import dev.noren.staybooking.property.internal.web.mapper.PropertyWebMapper;
import dev.noren.staybooking.property.internal.web.request.*;
import dev.noren.staybooking.property.internal.web.response.PropertyResponse;
import dev.noren.staybooking.property.internal.web.response.PropertyCatalogResponse;
import dev.noren.staybooking.bootstrap.web.ApiProblemDocument;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springdoc.core.annotations.ParameterObject;
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
@Tag(name = "Properties")
public class PropertyController {

    private static final String VERSION_ETAG_DESCRIPTION = "Current strong version ETag. Send it as If-Match for the next write.";
    private static final String IF_NONE_MATCH_DESCRIPTION = "Send a previously returned ETag. A matching tag returns 304 without a body.";

    private final PropertyService service;
    private final PropertyWebMapper mapper;
    private final VersionHeader versionHeader;

    @GetMapping
    @Operation(summary = "Search the property catalog", description = "Defaults to ACTIVE properties. INACTIVE and BLOCKED status filters require ADMIN. Pagination is zero-based; size is 1..100. Repeat sort=field,direction; fields: createdAt, pricePerNight, title.")
    @SecurityRequirement(name = "bearerAuth")
    @Parameter(name = "sort", in = ParameterIn.QUERY, description = "Repeat for multiple sorts: field,asc or field,desc. Allowed fields: createdAt, pricePerNight, title.",
            array = @ArraySchema(schema = @Schema(type = "string", example = "pricePerNight,asc")))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Catalog page"),
            @ApiResponse(responseCode = "400", description = "Invalid filter, pagination or sort", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Invalid bearer token or authentication required for non-active status", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN required for non-active status", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public PropertyCatalogResponse list(
            @ParameterObject @Valid @ModelAttribute PropertyFilterRequest filters,
            @ParameterObject @Valid @ModelAttribute PaginationRequest pagination
    ) {
        PropertyCatalogCriteria criteria = mapper.toCriteria(filters, pagination);
        PropertyCatalogResult catalog = service.list(criteria);
        return mapper.toResponse(catalog);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an active property", description = "Returns a strong version ETag.")
    @Parameter(name = HttpHeaders.IF_NONE_MATCH, in = ParameterIn.HEADER, description = IF_NONE_MATCH_DESCRIPTION,
            schema = @Schema(type = "string", example = "\"1\""))
    @ApiResponse(responseCode = "200", description = "Active property",
            headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                    required = true, schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "304", description = "Property has not changed", content = @Content,
            headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                    required = true, schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Invalid property ID", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    @ApiResponse(responseCode = "401", description = "Invalid bearer token", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    @ApiResponse(responseCode = "404", description = "Active property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    public ResponseEntity<PropertyResponse> getById(@PathVariable UUID id) {
        PropertyResult property = service.getActiveById(id);
        return propertyResponse(property);
    }

    @GetMapping("/{id}/management")
    @PreAuthorize("hasAnyRole('ADMIN', 'HOST')")
    @Operation(summary = "Get a property for management", description = "ADMIN or owning HOST. Returns a strong version ETag.")
    @SecurityRequirement(name = "bearerAuth")
    @Parameter(name = HttpHeaders.IF_NONE_MATCH, in = ParameterIn.HEADER, description = IF_NONE_MATCH_DESCRIPTION,
            schema = @Schema(type = "string", example = "\"1\""))
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Property for management",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "304", description = "Property has not changed", content = @Content,
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid property ID", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<PropertyResponse> management(@PathVariable UUID id) {
        PropertyResult property = service.getForManagement(id);
        return propertyResponse(property);
    }

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Create a property", description = "HOST only. New property starts INACTIVE. Returns Location and a strong version ETag.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Property created", headers = {
                    @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string")),
                    @Header(name = HttpHeaders.LOCATION, description = "Relative URI of the created property",
                            required = true, schema = @Schema(type = "string", format = "uri-reference"))
            }),
            @ApiResponse(responseCode = "400", description = "Invalid property details", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "HOST role required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
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
    @Operation(summary = "Update property details", description = "Owning HOST only. Send the current strong ETag in If-Match. Omitted JSON fields remain unchanged; explicit null clears nullable fields.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Updated property",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid patch or ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Invalid state transition", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<PropertyResponse> update(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
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
    @Operation(summary = "Replace property amenities", description = "Owning HOST only. Send the current strong ETag in If-Match.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Amenities replaced",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid request or ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property or amenity not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property is blocked", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> replaceAmenities(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch,
            @Valid @RequestBody ReplacePropertyAmenitiesRequest request
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.replaceAmenities(id, request.amenityIds(), expectedVersion);
        return voidResponse(version);
    }

    @PutMapping("/{id}/images")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Replace property images", description = "Owning HOST only. Send the current strong ETag in If-Match.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Images replaced",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid images or ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property is blocked", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> replaceImages(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
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
    @Operation(summary = "Delete a property", description = "Owning HOST only. Soft-deletes the property. Send the current strong ETag in If-Match.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Property deleted"),
            @ApiResponse(responseCode = "400", description = "Invalid ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property cannot be deleted in its current state", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public void delete(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        service.delete(id, expectedVersion);
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Activate a property", description = "Owning HOST only. Send the current strong ETag in If-Match; returns a new ETag and no body.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Property activated",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property cannot be activated", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> activate(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.activate(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Deactivate a property", description = "Owning HOST only. Send the current strong ETag in If-Match; returns a new ETag and no body.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Property deactivated",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "Role or ownership denied", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property cannot be deactivated", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> deactivate(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.deactivate(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/block")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Block a property", description = "ADMIN only. Send the current strong ETag in If-Match; returns a new ETag and no body.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Property blocked",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property cannot be blocked", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> block(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.block(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/unblock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unblock a property", description = "ADMIN only. Send the current strong ETag in If-Match; returns a new ETag and no body.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Property unblocked",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "409", description = "Property cannot be unblocked", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> unblock(
            @PathVariable UUID id,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
            @RequestHeader(value = HttpHeaders.IF_MATCH, required = false) String ifMatch
    ) {
        long expectedVersion = versionHeader.parseIfMatch(ifMatch);
        long version = service.unblock(id, expectedVersion);
        return voidResponse(version);
    }

    @PatchMapping("/{id}/host")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Change a property's host", description = "ADMIN only. Send the current strong ETag in If-Match; returns a new ETag and no body.")
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Host changed",
                    headers = @Header(name = HttpHeaders.ETAG, description = VERSION_ETAG_DESCRIPTION,
                            required = true, schema = @Schema(type = "string"))),
            @ApiResponse(responseCode = "400", description = "Invalid ETag or host ID. The new host must be a different existing HOST.", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "401", description = "Authentication required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "403", description = "ADMIN role required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "404", description = "Property not found", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "412", description = "Stale ETag", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class))),
            @ApiResponse(responseCode = "428", description = "If-Match required", content = @Content(mediaType = "application/problem+json", schema = @Schema(implementation = ApiProblemDocument.class)))
    })
    public ResponseEntity<Void> changeHost(
            @PathVariable UUID id,
            @Valid @RequestBody ChangeHostRequest request,
            @Parameter(description = "Current strong version ETag, such as the quoted value \"0\"", required = true)
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
