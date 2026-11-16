-- Activity schema migration (idempotent).
-- Run against the "sitebuilder-data" schema before starting the application,
-- because spring.jpa.hibernate.ddl-auto=validate does not create tables.
--
-- Railway does not execute .sql files on deploy. This file must be applied
-- manually to the database that DATABASE_URL points to, once, before the new
-- backend version starts. Re-running it is safe.
--
-- New objects only: this script never drops or rewrites existing rows.

CREATE SCHEMA IF NOT EXISTS "sitebuilder-data";

-- activity_events
--
-- Deliberately no foreign key on site_id: the trail records that a site
-- existed and must survive its deletion. owner_id still cascades, because an
-- event without an owner belongs to no account.
CREATE TABLE IF NOT EXISTS "sitebuilder-data".activity_events (
    id bigserial PRIMARY KEY,
    owner_id bigint NOT NULL REFERENCES "sitebuilder-data".users (id) ON DELETE CASCADE,
    site_id bigint NOT NULL,
    type varchar(64) NOT NULL,
    target_label varchar(255) NOT NULL,
    summary varchar(255) NOT NULL,
    actor_name varchar(255),
    created_at timestamptz NOT NULL DEFAULT now()
);

-- Matches findByOwnerIdOrderByCreatedAtDescIdDesc: newest first per account.
CREATE INDEX IF NOT EXISTS idx_activity_owner_created
    ON "sitebuilder-data".activity_events (owner_id, created_at DESC);
