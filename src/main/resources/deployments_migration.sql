-- Deployments schema migration (idempotent).
-- Run against the "sitebuilder-data" schema before starting the application,
-- because spring.jpa.hibernate.ddl-auto=validate does not create tables.

CREATE SCHEMA IF NOT EXISTS "sitebuilder-data";

CREATE TABLE IF NOT EXISTS "sitebuilder-data".deployments (
    id bigserial PRIMARY KEY,
    site_id bigint NOT NULL REFERENCES "sitebuilder-data".sites (id) ON DELETE CASCADE,
    owner_id bigint NOT NULL REFERENCES "sitebuilder-data".users (id),
    version integer NOT NULL,
    status varchar(20) NOT NULL DEFAULT 'DRAFT',
    published_url varchar(500),
    log_message text,
    triggered_by varchar(255),
    snapshot jsonb,
    created_at timestamptz NOT NULL DEFAULT now(),
    started_at timestamptz,
    finished_at timestamptz,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT ux_deployments_site_version UNIQUE (site_id, version)
);

ALTER TABLE "sitebuilder-data".deployments
    ADD COLUMN IF NOT EXISTS published_url varchar(500),
    ADD COLUMN IF NOT EXISTS log_message text,
    ADD COLUMN IF NOT EXISTS triggered_by varchar(255),
    ADD COLUMN IF NOT EXISTS snapshot jsonb,
    ADD COLUMN IF NOT EXISTS created_at timestamptz NOT NULL DEFAULT now(),
    ADD COLUMN IF NOT EXISTS started_at timestamptz,
    ADD COLUMN IF NOT EXISTS finished_at timestamptz,
    ADD COLUMN IF NOT EXISTS lock_version bigint NOT NULL DEFAULT 0;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ux_deployments_site_version'
          AND conrelid = '"sitebuilder-data".deployments'::regclass
    ) THEN
        ALTER TABLE "sitebuilder-data".deployments
            ADD CONSTRAINT ux_deployments_site_version UNIQUE (site_id, version);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_deployments_site_id
    ON "sitebuilder-data".deployments (site_id);

CREATE INDEX IF NOT EXISTS idx_deployments_owner_id
    ON "sitebuilder-data".deployments (owner_id);

CREATE INDEX IF NOT EXISTS idx_deployments_status
    ON "sitebuilder-data".deployments (status);

-- At most one LIVE deployment per site.
CREATE UNIQUE INDEX IF NOT EXISTS ux_deployments_one_live_per_site
    ON "sitebuilder-data".deployments (site_id)
    WHERE status = 'LIVE';
