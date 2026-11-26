package dev.noren.staybooking.property.internal.web.config;

import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Optional;

@Configuration(proxyBeanMethods = false)
class PropertyOpenApiConfig {

    @Bean
    OpenApiCustomizer documentOptionalCatalogAuthentication() {
        return openApi -> Optional.ofNullable(openApi.getPaths())
                .map(paths -> paths.get("/api/properties"))
                .map(PathItem::getGet)
                .ifPresent(catalog -> catalog.addSecurityItem(new SecurityRequirement()));
    }
}
