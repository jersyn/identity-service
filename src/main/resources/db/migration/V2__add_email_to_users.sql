ALTER TABLE users
    ADD COLUMN email VARCHAR(320) NOT NULL;

ALTER TABLE users
    ADD CONSTRAINT uk_users_email UNIQUE (email);
