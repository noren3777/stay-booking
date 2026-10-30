CREATE TABLE property_amenities
(
    property_id UUID NOT NULL,
    amenity_id  UUID NOT NULL,
    CONSTRAINT pk_property_amenities PRIMARY KEY (property_id, amenity_id),
    CONSTRAINT fk_property_amenities_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT fk_property_amenities_amenity FOREIGN KEY (amenity_id) REFERENCES amenities (id)
);
