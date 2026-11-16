-- Apply before deploying this branch (Hibernate uses ddl-auto=validate).
CREATE TABLE IF NOT EXISTS "sitebuilder-data".activity_events (
    id BIGSERIAL PRIMARY KEY,
    owner_id BIGINT NOT NULL REFERENCES "sitebuilder-data".users(id) ON DELETE CASCADE,
    site_id BIGINT NOT NULL,
    type VARCHAR(64) NOT NULL,
    target_label VARCHAR(255) NOT NULL,
    summary VARCHAR(255) NOT NULL,
    actor_name VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX IF NOT EXISTS idx_activity_owner_created
    ON "sitebuilder-data".activity_events(owner_id, created_at DESC);
