CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE bookings
    ADD CONSTRAINT ex_bookings_property_occupied_dates
    EXCLUDE USING gist (
        property_id WITH =,
        daterange(check_in, check_out, '[)') WITH &&
    )
    WHERE (status IN ('PENDING_PAYMENT', 'CONFIRMED'));
