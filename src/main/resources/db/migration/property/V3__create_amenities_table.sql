CREATE TABLE amenities
(
    id         UUID                        NOT NULL,
    created_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    name       VARCHAR(100)                NOT NULL,
    CONSTRAINT pk_amenities PRIMARY KEY (id),
    CONSTRAINT ck_amenities_name_trimmed_not_blank
        CHECK (btrim(name) <> '' AND name = btrim(name))
);

CREATE UNIQUE INDEX uc_amenities_name_lower ON amenities (lower(name));
