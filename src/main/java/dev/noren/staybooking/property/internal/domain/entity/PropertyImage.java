package dev.noren.staybooking.property.internal.domain.entity;

import dev.noren.staybooking.bootstrap.entity.BaseEntity;
import dev.noren.staybooking.bootstrap.exception.InvalidDomainArgumentException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URISyntaxException;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "property_images")
public class PropertyImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "property_id", nullable = false)
    private Property property;

    @Column(nullable = false, length = 2048)
    private String url;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "is_primary", nullable = false)
    private boolean primary;

    static PropertyImage create(Property property, String url, int sortOrder, Boolean primary) {
        if (primary == null) {
            throw new InvalidDomainArgumentException("image primary must not be null");
        }

        PropertyImage image = new PropertyImage();
        image.property = property;
        image.url = validateUrl(url);
        image.sortOrder = sortOrder;
        image.primary = primary;
        return image;
    }

    private static String validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new InvalidDomainArgumentException("image URL must not be blank or null");
        }
        if (url.length() > 2048) {
            throw new InvalidDomainArgumentException("image URL must not exceed 2048 characters");
        }

        try {
            URI uri = new URI(url);
            if (!uri.isAbsolute() || uri.getHost() == null
                    || !("http".equalsIgnoreCase(uri.getScheme())
                    || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new InvalidDomainArgumentException("image URL must be an absolute HTTP(S) address");
            }
        } catch (URISyntaxException exception) {
            throw new InvalidDomainArgumentException("image URL must be an absolute HTTP(S) address");
        }

        return url;
    }
}
