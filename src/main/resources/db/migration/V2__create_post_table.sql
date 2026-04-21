CREATE TABLE IF NOT EXISTS posts
(
    id         BIGSERIAL    PRIMARY KEY,
    uuid       UUID         NOT NULL UNIQUE DEFAULT gen_random_uuid(),
    title      VARCHAR(500) NOT NULL,
    slug       VARCHAR(500) NOT NULL,
    content    TEXT         NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',
    created_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_posts_slug UNIQUE (slug)
);

CREATE INDEX IF NOT EXISTS idx_posts_status ON posts (status);
