CREATE TABLE bookings
(
    id                       UUID                        NOT NULL,
    created_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at               TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    property_id              UUID                        NOT NULL,
    guest_id                 UUID                        NOT NULL,
    check_in                 DATE                        NOT NULL,
    check_out                DATE                        NOT NULL,
    number_of_guests         INTEGER                     NOT NULL,
    price_per_night_snapshot NUMERIC(12, 2)              NOT NULL,
    total_price              NUMERIC(12, 2)              NOT NULL,
    status                   VARCHAR(20)                 NOT NULL,
    CONSTRAINT pk_bookings PRIMARY KEY (id),
    CONSTRAINT ck_bookings_dates CHECK (check_out > check_in),
    CONSTRAINT ck_bookings_number_of_guests CHECK (number_of_guests > 0),
    CONSTRAINT ck_bookings_price_per_night_snapshot CHECK (price_per_night_snapshot > 0),
    CONSTRAINT ck_bookings_total_price CHECK (total_price > 0),
    CONSTRAINT ck_bookings_status CHECK (status IN (
        'PENDING_PAYMENT', 'CONFIRMED', 'PAYMENT_FAILED', 'CANCELLED', 'COMPLETED'
    ))
);

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_property FOREIGN KEY (property_id) REFERENCES properties (id);

ALTER TABLE bookings
    ADD CONSTRAINT fk_bookings_guest FOREIGN KEY (guest_id) REFERENCES users (id);
