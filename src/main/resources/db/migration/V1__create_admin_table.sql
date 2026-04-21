CREATE TABLE IF NOT EXISTS admin
(
    id               BIGSERIAL    PRIMARY KEY,
    github_username  VARCHAR(255) NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_admin_github_username UNIQUE (github_username)
);
