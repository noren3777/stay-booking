CREATE TABLE property_images
(
    id          UUID                        NOT NULL,
    created_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at  TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    property_id UUID                        NOT NULL,
    url         VARCHAR(2048)               NOT NULL,
    sort_order  INTEGER                     NOT NULL,
    is_primary  BOOLEAN                     NOT NULL,
    CONSTRAINT pk_property_images PRIMARY KEY (id),
    CONSTRAINT fk_property_images_property FOREIGN KEY (property_id) REFERENCES properties (id),
    CONSTRAINT ck_property_images_sort_order CHECK (sort_order >= 0),
    CONSTRAINT ck_property_images_url_not_blank CHECK (btrim(url) <> '')
);
