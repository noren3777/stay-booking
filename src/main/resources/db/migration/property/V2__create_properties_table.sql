CREATE TABLE properties
(
    id              UUID                        NOT NULL,
    deleted_at      TIMESTAMP(6) WITH TIME ZONE,
    created_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at      TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    host_id         UUID                        NOT NULL,
    title           VARCHAR(255)                NOT NULL,
    description     VARCHAR(5000),
    country         VARCHAR(100)                NOT NULL,
    city            VARCHAR(100)                NOT NULL,
    address         VARCHAR(255)                NOT NULL,
    price_per_night NUMERIC(12, 2)              NOT NULL,
    max_guests      INTEGER                     NOT NULL,
    status          VARCHAR(20)                 NOT NULL,
    version         BIGINT                      NOT NULL,
    CONSTRAINT pk_properties PRIMARY KEY (id)
);

ALTER TABLE properties
    ADD FOREIGN KEY (host_id) REFERENCES users (id);

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_price_per_night CHECK (
        price_per_night > 0
            AND price_per_night <= 9999999999.99
            AND scale(price_per_night) <= 2
        );

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_max_guests CHECK (max_guests > 0);

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_status CHECK (status IN ('ACTIVE', 'INACTIVE', 'BLOCKED'));

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_title_not_blank
        CHECK (btrim(title) <> '');

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_country_not_blank
        CHECK (btrim(country) <> '');

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_city_not_blank
        CHECK (btrim(city) <> '');

ALTER TABLE properties
    ADD CONSTRAINT ck_properties_address_not_blank
        CHECK (btrim(address) <> '');