package dev.noren.staybooking.bootstrap.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springdoc.core.customizers.OpenApiCustomizer;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI stayBookingOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Stay Booking API").version("1"))
                .components(new Components().addSecuritySchemes("bearerAuth",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }

    @Bean
    OpenApiCustomizer completeResponseDocumentation() {
        return openApi -> openApi.getPaths().values().stream()
                .flatMap(path -> path.readOperations().stream())
                .forEach(operation -> {
                    operation.getResponses().values().stream()
                            .filter(response -> response.getContent() != null && response.getContent().containsKey("*/*"))
                            .forEach(response -> response.getContent()
                                    .addMediaType("application/json", response.getContent().remove("*/*")));
                    operation.getResponses().putIfAbsent("500", new ApiResponse()
                            .description("Unexpected server error")
                            .content(new Content().addMediaType("application/problem+json",
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/ApiProblem")))));
                });
    }
}
