DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id                  CHAR(36)        NOT NULL,
    email_address       VARCHAR(100)    NOT NULL,
    password_hash       VARCHAR(255)    NOT NULL,
    `enabled`           BOOLEAN         NOT NULL        DEFAULT TRUE,
    created_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME        NOT NULL        DEFAULT CURRENT_TIMESTAMP       ON UPDATE CURRENT_TIMESTAMP,

    PRIMARY KEY (id),
    CONSTRAINT uk_users_email_address   UNIQUE (email_address)
);