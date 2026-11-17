-- Account & Security schema migration (idempotent).
-- Run against the "sitebuilder-data" schema before starting the application,
-- because spring.jpa.hibernate.ddl-auto=validate does not create columns/tables.

CREATE SCHEMA IF NOT EXISTS "sitebuilder-data";

-- ---------------------------------------------------------------------------
-- users
-- ---------------------------------------------------------------------------
ALTER TABLE "sitebuilder-data".users
    ADD COLUMN IF NOT EXISTS email_verified_at timestamp;

-- Backfill users that were verified before the column existed.
UPDATE "sitebuilder-data".users
SET email_verified_at = created_at
WHERE email_verified = true
  AND email_verified_at IS NULL;

-- ---------------------------------------------------------------------------
-- auth_sessions
-- ---------------------------------------------------------------------------
ALTER TABLE "sitebuilder-data".auth_sessions
    ADD COLUMN IF NOT EXISTS public_id uuid NOT NULL DEFAULT gen_random_uuid();

-- Backfill rows that may have been added before the default existed.
UPDATE "sitebuilder-data".auth_sessions
SET public_id = gen_random_uuid()
WHERE public_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_auth_sessions_public_id
    ON "sitebuilder-data".auth_sessions (public_id);

-- ---------------------------------------------------------------------------
-- security_events
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "sitebuilder-data".security_events (
    id bigserial PRIMARY KEY,
    user_id bigint NOT NULL REFERENCES "sitebuilder-data".users (id),
    type varchar(40) NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT now(),
    ip_address varchar(45),
    user_agent varchar(512),
    detail varchar(255),
    session_public_id uuid
);

CREATE INDEX IF NOT EXISTS idx_security_events_user_occurred
    ON "sitebuilder-data".security_events (user_id, occurred_at DESC);

-- ---------------------------------------------------------------------------
-- notification_preferences
-- ---------------------------------------------------------------------------
CREATE TABLE IF NOT EXISTS "sitebuilder-data".notification_preferences (
    user_id bigint PRIMARY KEY REFERENCES "sitebuilder-data".users (id),
    in_app boolean NOT NULL DEFAULT true,
    email boolean NOT NULL DEFAULT false,
    security_alerts boolean NOT NULL DEFAULT true,
    website_updates boolean NOT NULL DEFAULT true,
    billing_updates boolean NOT NULL DEFAULT true,
    updated_at timestamptz NOT NULL DEFAULT now()
);
