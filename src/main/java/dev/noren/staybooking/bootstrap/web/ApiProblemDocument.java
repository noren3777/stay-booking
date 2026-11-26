package dev.noren.staybooking.bootstrap.web;

import dev.noren.staybooking.bootstrap.exception.dto.ValidationError;
import io.swagger.v3.oas.annotations.media.Schema;

import java.net.URI;
import java.util.List;

@Schema(name = "ApiProblem", description = "RFC 9457 ProblemDetail with application fields. errors is present for Bean Validation failures.")
public record ApiProblemDocument(
        @Schema(example = "about:blank") URI type,
        @Schema(example = "Bad Request") String title,
        @Schema(example = "400") int status,
        @Schema(example = "Request validation failed") String detail,
        @Schema(example = "VALIDATION_FAILED") String code,
        @Schema(example = "/api/bookings") String path,
        @Schema(example = "e1a75576-a7d0-4114-a65b-a5b2ef9140d8") String requestId,
        List<ValidationError> errors
) {
}
