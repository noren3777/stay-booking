CREATE TABLE users
(
    id            UUID                        NOT NULL,
    created_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    updated_at    TIMESTAMP(6) WITH TIME ZONE NOT NULL,
    email         VARCHAR(255)                NOT NULL,
    password_hash VARCHAR(255)                NOT NULL,
    role          VARCHAR(20)                 NOT NULL,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

ALTER TABLE users
    ADD CONSTRAINT uc_users_email UNIQUE (email);

ALTER TABLE users
    ADD CONSTRAINT ck_users_role CHECK (role IN ('GUEST', 'HOST', 'ADMIN'));

ALTER TABLE users
    ADD CONSTRAINT ck_users_email_normalized CHECK (email = lower(btrim(email)));
